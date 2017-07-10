/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.scanner;

import com.skype.research.sparse.DataChunk;
import com.skype.research.sparse.trimmer.NeedTrimmingException;
import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Scans for EXT4 superblock.
 */
public class Ext4Scanner implements Scanner {

	// "An ext2/ext3/ext4 filesystem always has the bytes 0x53 0xEF at positions 1080.–1081.
	//	https://ext4.wiki.kernel.org/index.php/Ext4_Disk_Layout
	//	http://superuser.com/questions/239088/whats-a-file-systems-magic-number-in-a-super-block
	
	// rating weights
	public static final int RATING_IDEAL = 100;
	public static final int RATING_LOCAL_UNIQUENESS = 30;
	public static final int RATING_BINARY_ROUNDNESS = 30;
	public static final int RATING_EARLY_BIRD_RIGHT = 20;
	public static final int RATING_EARLY_REFERENCED = 20;

	// magic
	private static final short EXT4MAGIC = (short) 0xef53;
	private static final int EXT4_OFFSET = 0x438;
	
	// binary
	private static final int SIZEOF_INT = 4;
	private static final int SIZEOF_SHORT = 2;
	private static final int KILO = 1 << 10;

	private int[] initialIndex;
	private long[] absolutized;
	private Map<DataChunk, ChunkTotal> chunkTotals = new HashMap<>();
	private SortedMap<Long, DataChunk> foundOutput = new TreeMap<>();
	private int foundCount;
	private long cutAtBytes = 0;
	private TrimChannel trimmer;
	private ScannerConf conf;
	
	@Override
	public void setTrimmer(TrimChannel trimmer) {
		this.trimmer = trimmer;
	}

	@Override
	public void reset() {
		initialIndex = null;
		foundOutput.clear();
		chunkTotals.clear();
		foundCount = 0;
		cutAtBytes = 0;
	}

	@Override
	public void setScannerCfg(ScannerConf conf) {
		this.conf = conf;
	}

	@Override
	public long scan(DataChunk dataChunk, DataInput input, FileChannel in, SeekableByteChannel out) throws IOException, NeedTrimmingException {
		final long scanAtMostBytes = conf.getScanAtMost() == 0 ? Long.MAX_VALUE : conf.getScanAtMost() * KILO;
		if (out.position() <= scanAtMostBytes) {
			ByteBuffer mapped = in.map(FileChannel.MapMode.READ_ONLY, in.position(),
					Math.min(dataChunk.getTargetBytes(), scanAtMostBytes))
					.order(ByteOrder.LITTLE_ENDIAN);
			if (initialIndex == null) {
				IntBuffer index = mapped.asIntBuffer();
				final int scanAtMostWords = conf.getForeAtMost() * KILO / SIZEOF_INT;
				int indexLength = Math.min(index.remaining(), scanAtMostWords);
				initialIndex = new int[indexLength];
				absolutized = new long[indexLength];
				index.get(initialIndex);
				for (int i = 0; i < initialIndex.length; i++) {
					int relative = initialIndex[i];
					absolutized[i] = (long) (int) (out.position() + relative * SIZEOF_INT);
				}
				Arrays.sort(initialIndex);
				Arrays.sort(absolutized);
			}
			ShortBuffer magic = mapped.asShortBuffer();
			while (magic.hasRemaining()) {
				if (magic.get() == EXT4MAGIC) {
					long rel = (magic.position() - 1) * SIZEOF_SHORT;
					long pos = out.position() + rel;
					long blk = pos - EXT4_OFFSET;
					if (blk >= 0) {
						foundOutput.put(blk, dataChunk);
						ChunkTotal total = totalForChunk(dataChunk);
						total.matchCount++;
						if (conf.getCutAtIndex() == foundCount++) {
							cutAtBytes = blk;
							if (trimmer != null) {
								trimmer.ensureTrim(blk);
							}
						}
					}
				}
			}
		}
		return -1;
	}

	private ChunkTotal totalForChunk(DataChunk dataChunk) {
		ChunkTotal total = chunkTotals.get(dataChunk);
		if (total == null) {
			total = new ChunkTotal();
			chunkTotals.put(dataChunk, total);
		}
		return total;
	}
	
	static enum Outcome {
		NOT_FOUND(false, false, false, 
				"Warning: no EXT4 superblock found in output. Relax the scanning settings."),
		RAW_IDEAL(true, false, false,
				"The image looks like a well-formed EXT4. No head cutting seems necessary."),
		CUT_FOUND(false, true, false,
				"The image would not be mountable unless its head is cut off. Use -Sc <cut-id>."),
		CUT_ALONE(false, true, true,
				"Only one superblock found. If mounting fails, relax the scanning settings."),
		CUT_MULTI(false, true, true,
				"Used a custom cutting point. If mounting fails, try another one!"),
		CUT_WRONG(true, true, true,
				"Warning: a custom cut used, though the beginning seems a good EXT4 superblock."),
		;

		private final boolean rawIsGood;
		private final boolean cutIsGood;
		private final boolean cutIsUsed;
		private final String displayName;

		Outcome(boolean rawIsGood, boolean cutIsGood, boolean cutIsUsed, String displayName) {
			this.rawIsGood = rawIsGood;
			this.cutIsGood = cutIsGood;
			this.cutIsUsed = cutIsUsed;
			this.displayName = displayName;
		}

		@Override
		public String toString() {
			return displayName;
		}
	}

	@Override
	public void report(PrintStream printStream) {
		final Outcome outcome;
		if (foundCount > 0) {
			class Result implements Comparable<Result> {
				final long blk;
				final int rank;
				int rating;

				public Result(Map.Entry<Long, DataChunk> found, int rank) {
					this.blk = found.getKey();
					this.rank = rank;
					if (blk == 0L) {
						rating = RATING_IDEAL;
					} else {
						final DataChunk chunk = found.getValue();
						final int blockSize = chunk.getBlockSize();
						rating += RATING_LOCAL_UNIQUENESS / chunkTotals.get(chunk).matchCount;
						rating += RATING_BINARY_ROUNDNESS * (Long.numberOfTrailingZeros(blk | blockSize) / Long.numberOfTrailingZeros(blockSize));
						rating += RATING_EARLY_BIRD_RIGHT / (rank + 1);
						rating += RATING_EARLY_REFERENCED * rateAgainstIndexBlock(blk);
					}
				}

				@Override
				public int compareTo(Result result) {
					return Integer.compare(result.rating, rating);
				}

				@Override
				public String toString() {
					return String.format("%02d%% match: %10d (b) -- select with -Sc %d", rating, blk, rank);
				}
			}
			int rank = 0;
			PriorityQueue<Result> queue = new PriorityQueue<>(foundCount);
			for (Map.Entry<Long, DataChunk> found : foundOutput.entrySet()) {
				queue.add(new Result(found, rank++));
			}
			final boolean rawIsGood = foundOutput.containsKey(0L);
			final boolean hasBeenCut = conf.getCutAtIndex() >= (rawIsGood ? 1 : 0) && conf.getCutAtIndex() < foundCount;
			Result result;
			while ((result = queue.poll()) != null) {
				if (result.rating >= conf.getGoodRating()) {
					printStream.println(result);
				} else {
					printStream.println(String.format("%d results rated below %d%%.", queue.size(), conf.getGoodRating()));
					printStream.println("Use -St<threshold> (e.g. -St0) to display them.");
					break;
				}
			}
			printStream.println();
			if (conf.getScanAtMost() != 0) {
				printStream.printf("Up to %d kb scanned; -Ss <at-most> to alter or -Ss 0 to search entire file.%n",
						conf.getScanAtMost());
			}
			printStream.printf("Up to %d kb from start treated as index; -Si <at-most> to change the threshold.%n",
					conf.getForeAtMost());
			if (rawIsGood) {
				if (hasBeenCut) {
					outcome = Outcome.CUT_WRONG;
				} else {
					outcome = Outcome.RAW_IDEAL;
				}
			} else {
				if (hasBeenCut) {
					if (foundCount > 1) {
						outcome = Outcome.CUT_MULTI;
					} else {
						outcome = Outcome.CUT_ALONE;
					}
				} else {
					outcome = Outcome.CUT_FOUND;
				}
			}
		} else {
			outcome = Outcome.NOT_FOUND;
		}
		printStream.println();
		if (outcome.cutIsUsed) {
			printStream.println(String.format("Output file cut at %10d (b)", cutAtBytes));
		}
		printStream.println(outcome);
	}

	public float rateAgainstIndexBlock(long blk) {
		float rating = 0;
		if (blk < Integer.MAX_VALUE && Arrays.binarySearch(initialIndex, (int) blk) >= 0) {
			rating += 0.6f;
		}
		// independent, not mutually exclusive
		if (Arrays.binarySearch(absolutized, blk) >= 0) {
			rating += 0.4f;
		} else if (Arrays.binarySearch(absolutized, blk - SIZEOF_INT) >= 0) {
			rating += 0.4f;
		}
		return rating;
	}

	static class ChunkTotal {
		int matchCount;
	}
}
