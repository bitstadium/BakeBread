/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.match;

import com.skype.util.partition.MetricTree;
import com.skype.util.partition.metric.Metric;
import com.skype.util.partition.rolling.HashRoller;
import com.skype.util.partition.rolling.Rolling;
import com.skype.util.partition.rolling.RollingHash;

import java.nio.ByteBuffer;

/**
 * "The weaver in Vienna weaves wonderful velvet, but he never wears it since winters are warm."
 * A "base fabric", i.e. a comparison source applied a window hash and presented as a metric tree.
 */
public class SeamBase implements Base {

	private final RollingHash hash;
	private final MetricTree tree;

	public SeamBase(ByteBuffer original, HashRoller roller, Metric metric) {
		this(roller.index(original), metric);
	}

	public SeamBase(RollingHash hash, Metric metric) {
		this.hash = hash;
		tree = new MetricTree(hash.computed(), metric);
	}

	@Override
	public RollingHash getHash() {
		return hash;
	}

	@Override
	public MetricTree getTree() {
		return tree;
	}

	public int stitchEstimate(ByteBuffer modified, HashRoller roller) {
		return stitchEstimate(Rolling.Utils.afterWindowToHashIndex(roller, modified.capacity()));
	}
	
	public int stitchEstimate(RollingHash modified) {
		return stitchEstimate(modified.computed().capacity());
	}
	
	public int stitchEstimate(int sideSteps) {
		return stitchEstimate(hash.computed().capacity(), sideSteps);
	}
	
	public static int stitchEstimate(int origSteps, int sideSteps) {
		int oOrder = log2(origSteps);
		int mOrder = log2(sideSteps);
		// produces 256 for a 4096-step hash
		// max(4, sqrt(GeoMean(o, s)) * 4)
		return 1 << (((oOrder + mOrder) >> (1 << 1)) + (1 << 1));
		// max(4, GeoMean(o, s) / 16)
		// return Math.max(4, 1 << (((oOrder + mOrder) >> 1) - (1 << (1 << 1))));
	}

	private static int log2(int origSteps) {
		return Integer.numberOfTrailingZeros(Integer.highestOneBit(origSteps * 3 / 2));
	}

	public SeamDiff approximate(ByteBuffer modified, HashRoller roller) {
		return approximate(modified, roller, stitchEstimate(modified, roller));
	}

	public SeamDiff approximate(RollingHash rhModified) {
		return approximate(rhModified, stitchEstimate(rhModified));
	}

	public SeamDiff approximate(ByteBuffer modified, HashRoller roller, int stitches) {
		return approximate(roller.index(modified), stitches);
	}

	public SeamDiff approximate(RollingHash rhModified, int stitches) {
		return new SeamDiff(hash, tree, rhModified, stitches);
	}
}
