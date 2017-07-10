/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition;

import com.skype.util.partition.metric.Metric;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * http://pnylab.com/pny/papers/vptree/main.html
 * http://pnylab.com/pny/papers/vptree/vptree.pdf
 * See others: http://pnylab.com/pny/index.html
 * (the VP-tree is not listed among the patents)
 */
public class MetricTree {
	static final int MAX_VERTEX_SAMPLE_COUNT = 0x04;
	static final int MAX_VERTEX_SAMPLE_SHIFT = 0x01;

	// MMSC * maxDistance * maxDistance < Integer.MAX_VALUE 
	static final int MAX_MEDIAN_SAMPLE_COUNT = 0x10;

	final class Node {
		final int depth;
		final int start;
		final int count;
		final Node left, right;
		
		final int[] mine, next;
		final int center; // in mine
		final int split;  // in next
		
		final int median;

		private Node(int depth, int start, int count) {
			this.start = start;
			this.count = count;
			this.depth = depth;
			mine = indices(depth);
			if (count <= 2 || !(count <= 5 ? miniCenter(partition) : center(partition))) {
				next = null;
				center = start;
				split = -1;
				median = 0;
				left = right = null;
			} else {
				center = partition.center;
				median = partition.median;
				next = indices(depth + 1);
				split = split();
				if (split == start || split == end()) {
					// report as warning
					left = right = null;
				} else {
					left = new Node(this, split, false);
					right = new Node(this, split, true);
				}
			}
		}

		private boolean miniCenter(Partition partition) {
			int bestEdge = Integer.MAX_VALUE;
			int bestBase = -1;
			int longEdge = Integer.MIN_VALUE;
			int last = end() - 1;
			for (int i = this.start; i < last; ++i) {
				final long bits = sourceAt(mine[i]);
				for (int j = i + 1; j <= last; ++j) {
					int edge = metric.distance(bits, sourceAt(mine[j]));
					if (edge > longEdge) {
						longEdge = edge;
					}
					if (edge < bestEdge) {
						bestEdge = edge;
						bestBase = i;
					}
				}
			}
			partition.center = bestBase;
			partition.median = bestEdge + 1;
			return bestEdge != longEdge;
		}

		private boolean center(Partition partition) {
			/// let the actual splitting occur
			int centers = Math.min(MAX_VERTEX_SAMPLE_COUNT, count >> MAX_VERTEX_SAMPLE_SHIFT);
			int samples = Math.min(MAX_MEDIAN_SAMPLE_COUNT, count);
			float maxVar = -1; // doomed to lose
			int center  = -1;
			for (int c = 0; c < centers; ++c) {
				float totalP = 0;
				float totalQ = 0;
				int local;
				///
				for (int s = 0; s < samples; ++s) {
					final int addEx = random.nextInt(this.count);
					if (s == c) {
						continue;
					}
					local = metric.distance(sourceAt(mine[start + c]), sourceAt(mine[start + addEx]));
					d[c][s] = local;
					totalP += local;
					totalQ += 1. * local * local;
				}
				float var = (totalQ - totalP * totalP / samples) / samples;
				if (maxVar < var) {
					maxVar = var;
					center = c;
				}
			}
			if (maxVar <= 0) {
				return false;
			}
			partition.center = start + center;
			partition.median = Median.linearMedian(d[center], 0, samples);
			return true;
		}

		private int split() {
			long centerBits = sourceAt(mine[center]);
			int sPtr = start;
			int lPtr = start;
			int rPtr = start + count;
			int index;
			while (lPtr != rPtr) {
				index = mine[sPtr++];
				int d1 = metric.distance(centerBits, sourceAt(index));
				if (d1 < median) {
					next[lPtr++] = index;
				} else {
					next[--rPtr] = index;
				}
			}
			return lPtr;
		}

		Node(Node source, int split, boolean upper) {
			this(
					source.depth + 1, 
					upper? split : source.start,
					upper? source.end() - split
						:  split - source.start
			);
		}

		private int end() {
			return start + count;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < depth; ++i) {
				sb.append('\t');
			}
			return String.format("%s%d..%d *%d", sb, start, start+count-1, median);
		}

		public void find(Match match) {
			if (right == null /* && left == null */) {
				for (int index = start; index < end() && match.receptive(); ++index) {
					match.offer(mine[index], sourceAt(mine[index]), metric);
				}
			} else if (match.receptive()) {
				// we have internals and need to visit internals
				final int cDist = metric.distance(match.needle, sourceAt(mine[center]));
				if (cDist - match.distance < median) {
					left.find(match);
				}
				if (cDist + match.distance >= median) {
					right.find(match);
				}
			}
		}
	}

	private final Metric metric;
	private final int length;
	private final LongBuffer source;
	private final List<int[]> splits = new ArrayList<>();
	private final Node root;

	private static class Partition {
		int center; // absolute (source offset)
		int median; // radius of an open sphere
	}
	
	final transient Random random = new Random(0);
	final transient int[][] d = new int[MAX_VERTEX_SAMPLE_COUNT][MAX_MEDIAN_SAMPLE_COUNT];
	final transient Partition partition = new Partition();

	public static class Match {
		private final boolean single; // TODO support multi-match
		private int matchIndex;
		private long matchValue;
		int distance; // package
		private long needle;
		private int offered, updated;

		public Match(boolean single) {
			this.single = single;
		}

		public void setNeedle(long needle) {
			this.needle = needle;
			matchIndex = -1;
			matchValue = -1;
			distance = Integer.MAX_VALUE;
			offered = updated = 0;
		}

		final void offer(int index, long value, Metric metric) {
			++offered;
			final int distance = metric.distance(this.needle, value);
			if (this.distance > distance) {
				++updated;
				this.matchIndex = index;
				this.matchValue = value;
				this.distance = distance;
			}
		}

		final boolean receptive() {
			return distance != 0;
		}

		public long needle() {
			return needle;
		}

		public int matchIndex() {
			return matchIndex;
		}

		public long matchValue() {
			return matchValue;
		}

		public int matchDistance() {
			return distance;
		}

		@Override
		public String toString() {
			return matchIndex >= 0
					? String.format("%016x ~= %016x @%d d=%d |%d offers/%d updates",
						needle, matchValue, matchIndex, distance, offered, updated) 
					: String.format("%016x not found |%d offers",
						needle, offered
			);
		}

		public int offerCount() {
			return offered;
		}

		public int updateCount() {
			return updated;
		}
	}

	public MetricTree(long[] source, int offset, int length, Metric metric) {
		this(LongBuffer.wrap(source, offset, length), metric);
	}
	
	public MetricTree(long[] source, Metric metric) {
		this(LongBuffer.wrap(source), metric);
	}
	
	public MetricTree(LongBuffer source, Metric metric) {
		if ((this.length = source.remaining()) == 0) {
			// fail fast, die young
			throw new NoSuchElementException("Empty!");
		}
		this.metric = metric;
		this.source = source.asReadOnlyBuffer();
		// sort indices to improve "corners"
		splits.add(seedIndices());
		root = new Node(0, 0, length);
	}
	
	private long sourceAt(int index) {
		return source.get(index);
	}

	private int[] seedIndices() {
		int[] indices = new int[length];
		for (int i = 0; i < length; ++i) {
			indices[i] = i + source.position();
		}
		return indices;
	}

	private int[] indices(int depth) {
		while (depth >= splits.size()) {
			splits.add(new int[length]);
		}
		return splits.get(depth);
	}

	public void find(long needle, Match match, int suggestedIndex) {
		match.setNeedle(needle);
		match.offer(suggestedIndex, sourceAt(suggestedIndex), metric);
		root.find(match);
	}
	
	public void find(long needle, Match match) {
		match.setNeedle(needle);
		root.find(match);
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		println(pw, root);
		return sw.getBuffer().toString();
	}

	public void println(PrintWriter pw, Node node) {
		if (node != null) {
			pw.println(node);
			println(pw, node.left);
			println(pw, node.right);
		}
	}
}
