/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition;

import com.skype.research.bakebread.nio.BufferAdapter;
import com.skype.research.exediff.bleach.ArmBleach;
import com.skype.research.exediff.bleach.Bleach;
import com.skype.research.exediff.bleach.DataBleach;
import com.skype.research.exediff.bleach.ThumbBleach;
import com.skype.research.exediff.config.MutableThresholds;
import com.skype.research.exediff.config.Thresholds;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.match.Cost;
import com.skype.research.exediff.match.SeamBase;
import com.skype.research.exediff.match.SeamDiff;
import com.skype.research.exediff.present.DamageMeter;
import com.skype.research.exediff.present.HammingStat;
import com.skype.research.exediff.present.Hexualizer;
import com.skype.research.exediff.present.PrettyTotal;
import com.skype.util.partition.metric.Metric;
import com.skype.util.partition.metric.Metrics;
import com.skype.util.partition.rolling.HashRoller;
import com.skype.util.partition.rolling.HashRollers;
import com.skype.util.partition.rolling.Rolling;
import com.skype.util.partition.rolling.RollingHash;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * Metric tree test.
 */
public class MetricTreeTest extends TestCase {
	private static final Thresholds thresholds = new MutableThresholds();
	private static final PrintWriter writer = new PrintWriter(System.out);

	private final Random random = new Random();

	byte[] baDump;
	byte[] baHost;

	public void setUp() throws Exception {
		super.setUp();
		random.setSeed(0);
		baDump = getResourceAsBytes("libcFromDump.bin");
		baHost = getResourceAsBytes("libcFromHost.bin");
	}

	public void testGenerateTree() throws Exception {
		final RollingHash tamien = HashRollers.TAMIEN_HALFWORD.index(arm(getResourceAsBytes("libcFromDump.bin")));
		long[] hashes = asArray(tamien.computed());

		for(Metric metric : Metrics.values()) {
			writer.println();
			writer.println(metric.toString() + " on long[" + hashes.length + "]");

			final long original = Arrays.hashCode(hashes); // MOREINFO isolate in another test?
			MetricTree tree = new MetricTree(hashes, metric);
			Assert.assertEquals("Original array left intact", original, Arrays.hashCode(hashes));
			// writer.println(tree);
			MetricTree.Match match = new MetricTree.Match(true);

			for (long needle : new long[]{
					0L,  // all zeroes
					~0L, // all ones
					0xcafebabe90091ea1L,
					0xdeafbeef900df00dL,
					hashes[hashes.length >> 3],
					hashes[hashes.length >> 2],
					hashes[hashes.length >> 1]
			}) {
				tree.find(needle, match);
				Assert.assertEquals(match.toString(),
						hashes[match.matchIndex()],
						match.matchValue());
				Assert.assertEquals(match.toString(),
						metric.distance(match.needle(), match.matchValue()),
						match.matchDistance());
				for (int i = 0; i < hashes.length; ++i) {
					long value = hashes[i];
					Assert.assertFalse("Better match at " + i + ": " + Long.toHexString(value),
							metric.distance(match.needle(), value) < match.matchDistance());
				}
				writer.println(match);
			}
		}
	}

	public void testSuggestedStitches() throws Exception {
		Assert.assertEquals(256, SeamBase.stitchEstimate(4096, 4096));
		Assert.assertEquals(256, SeamBase.stitchEstimate(8192, 2048));
	}

	public void testHarmlessBleach() throws Exception {
		Bleach bleach = new ArmBleach();
		bleach.bleach(ByteBuffer.wrap(baDump));
		bleach.bleach(ByteBuffer.wrap(baHost));
		testPositionDeviationAkaNoBleach();
	}
	public void testWordRepeatBleach() throws Exception {
		Bleach bleach = new DataBleach<>(BufferAdapter.Stateless.INT_BAD);
		Arrays.fill(baDump, 0x1c20, baDump.length, (byte) 0);
		Arrays.fill(baHost, 0x1c20, baHost.length, (byte) 0);
		bleach.bleach(ByteBuffer.wrap(baDump));
		bleach.bleach(ByteBuffer.wrap(baHost));
		testCorrectBleach();
	}

	public void testHalfRepeatBleach() throws Exception {
		Bleach bleach = new DataBleach<>(BufferAdapter.Stateless.CHAR_BAD);
		Arrays.fill(baDump, 0x1c20, baDump.length, (byte) 0);
		Arrays.fill(baHost, 0x1c20, baHost.length, (byte) 0);
		bleach.bleach(ByteBuffer.wrap(baDump));
		bleach.bleach(ByteBuffer.wrap(baHost));
		testCorrectBleach();
	}

	public void testCorrectBleach() throws Exception {
		Bleach bleach = new ThumbBleach();
		bleach.bleach(ByteBuffer.wrap(baDump));
		bleach.bleach(ByteBuffer.wrap(baHost));
		testPositionDeviationAkaNoBleach();
	}
	
	public void testBadMixedBleach() throws Exception {
		new ThumbBleach().bleach(ByteBuffer.wrap(baDump));
		new ArmBleach().bleach(ByteBuffer.wrap(baHost));
		testPositionDeviationAkaNoBleach();
	}
	
	public void testOriginalSkewNear0() throws Exception {
		System.arraycopy(baDump, 4, baDump, 0, baDump.length - 4);
		testPositionDeviationAkaNoBleach();
	}
	
	public void testModifiedSkewNear0() throws Exception {
		System.arraycopy(baHost, 4, baHost, 0, baHost.length - 4);
		testPositionDeviationAkaNoBleach();
	}
	
	public void testPositionDeviationAkaNoBleach() throws Exception {
		// raw source data
		final ByteBuffer bbDump = arm(baDump);
		final ByteBuffer bbHost = arm(baHost);
		final Einsteinian frame = new Einsteinian(bbDump, 0L, bbHost, 0L);
		// display tool
		final Hexualizer hexualizer = new Hexualizer(writer, frame);

		// policy-making
		final HashRoller roller = HashRollers.TAMIEN_HALFWORD;
		final Metric metric = Metrics.ShortRadialMetric;

		// preprocessed source data
		final RollingHash rhDump = roller.index(bbDump);
		final RollingHash rhHost = roller.index(bbHost);

		assertEquals(rhDump.getHashAlgorithm(), rhHost.getHashAlgorithm());
		Rolling.Utils.checkInvariants(rhDump);
		Rolling.Utils.checkInvariants(rhHost);

		// the metric tree
		SeamDiff diff = new SeamBase(rhDump, metric).approximate(rhHost);

		writer.println(Arrays.toString(diff.drift()));

		writer.println();
		writer.println("Costs of sequential (coarse) match:");
		writer.println(diff.getBaseCost());
		PrettyTotal.displayDetailedHamming(hexualizer.writer, new HammingStat(diff, frame));

		writer.println();
		Cost fineCost = diff.newBlankCost();
		diff.healGaps(fineCost, bbDump, bbHost);
		HammingStat stat = new HammingStat(diff, frame);
		writer.println("Costs of refinement (greedy) match:");
		writer.println(fineCost);
		PrettyTotal.displayDetailedHamming(hexualizer.writer, stat);
		writer.println();
		PrettyTotal.displayBulkMappedBytes(hexualizer.writer, stat);
		PrettyTotal.displayOutlierPercents(writer, diff);
		writer.println();
		hexualizer.displayLineByteChanges(diff, stat, true);
		writer.println();
		writer.println("Overall quality: " + new DamageMeter(thresholds).assess(diff, stat));
		// TODO compare with Levenshtein
	}

	private static ByteBuffer arm(byte[] binaryCode) {
		return ByteBuffer.wrap(binaryCode).order(ByteOrder.LITTLE_ENDIAN);
	}

	private byte[] getResourceAsBytes(String name) throws IOException {
		try (InputStream inputStream = getClass().getResourceAsStream(name)) {
			byte[] bytes = new byte[inputStream.available()];
			DataInput dataInput = new DataInputStream(inputStream);
			dataInput.readFully(bytes);
			return bytes;
		}
	}

	private static long[] asArray(LongBuffer hashBuf) {
		long[] hashes = new long[hashBuf.remaining()];
		hashBuf.get(hashes);
		return hashes;
	}

	public void tearDown() throws Exception {
		writer.flush();
		super.tearDown();
	}
}
