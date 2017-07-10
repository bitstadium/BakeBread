/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.match;

import com.skype.research.bakebread.model.analysis.AreaProp;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.MemHeap;
import com.skype.research.bakebread.model.analysis.ResolvedMemArea;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.model.MemSeam;
import com.skype.util.partition.LMS;
import com.skype.util.partition.MetricTree;
import com.skype.util.partition.rolling.Rolling;
import com.skype.util.partition.rolling.RollingHash;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.SortedMap;

/**
 * A seam-driven difference.
 */
public class SeamDiff implements Diff {
	// a single reusable match ATM, multiple matches tbd if needed
	private final MetricTree.Match match = new MetricTree.Match(true);

	// stats/accounting
	private final Cost baseCost;
	private final RollingHash rhModified;
	private final RollingHash rhOriginal;
	private final MetricTree tree;

	// retained for reusability, e.g. for healing intervals
	class Inquiry {
		private final transient Random random = new Random(0); // seed set for predictability

		private final int offset;
		private final int length;

		private final int stitches;
		private final int[] indices;
		private final int[] matches;
		private final int[] drifted;

		// statistic
		private final Cost cost;

		Inquiry(int stitches, int offset, int length, Cost cost) {
			this.stitches = stitches;
			this.offset = offset;
			this.length = length;
			this.cost = cost;
			indices = new int[stitches];
			matches = new int[stitches];
			drifted = new int[stitches];
		}

		public int[] leftToRight() {
			// pre-fill unique indices
			fillInUniqueIndices();
			// WISDOM using more than one historical drift level does not improve much: 
			// WISDOM offers/stitches=5.5742188 vs. offers/stitches=5.6445312
			int drift = 0;
			for (int stitch = 0; stitch < stitches; ++stitch) {
				drift = probe(stitch, drift);
			}
			return drifted;
		}

		public void fillInUniqueIndices() {
			final BitSet steps = new BitSet(length);
			int bitIndex;
			// FIXME: think of a cheaper way to fill in
			for (int stitch = 0; stitch < stitches; ) {
				bitIndex = random.nextInt(length);
				if (!steps.get(bitIndex)) {
					steps.set(bitIndex);
					indices[stitch++] = offset + bitIndex;
				}
			}
			Arrays.sort(indices);
		}

		private int probe(int stitch, int drift) {
			int suggestion = indices[stitch] + drift;
			long needle = rhModified.computed().get(indices[stitch]);
			if (suggestion >= 0 && suggestion < rhOriginal.computed().limit()) {
				tree.find(needle, match, suggestion);
			} else {
				tree.find(needle, match);
			}
			// extract a statistic object?
			matches[stitch] = match.matchIndex();
			drifted[stitch] = drift = matches[stitch] - indices[stitch];
			cost.recordMappingCosts(match);
			return drift;
		}

		private int group() {
			final PriorityQueue<MemSeam> bySize = new PriorityQueue<>(4, AreaProp.LENGTH.dec);

			final int NONE = Integer.MIN_VALUE;

			int outlierCount = 0;

			// MOREINFO extract as strategy
			// ROADMAP this is a simple FSM, so define states explicitly
			int currentDrift = NONE;
			int currentRange = NONE;
			int updatedDrift = NONE;
			int stitch = 0;
			while (stitch <= stitches) {
				// a drift change or the end of the universe is considered a range boundary
				if (stitch == stitches || (updatedDrift = drifted[stitch]) != currentDrift) {
					// close a contiguous range
					if (currentRange != NONE) {
						bySize.add(postRange(currentRange, stitch - 1, growing.get(currentRange), outlierCount));
						currentRange = NONE;
					} else if (currentDrift != NONE) {
						// no range open, so the previous one was an outlier
						++outlierCount;
					}
					currentDrift = updatedDrift;
				} else if (currentRange == NONE) {
					currentRange = stitch - 1; // previous
				}
				stitch++;
			}

			MemSeam memSeam;
			while ((memSeam = bySize.poll()) != null) {
				addToEventualTarget(memSeam);
			}
			final long inverseStart = 0;
			final long inverseAfter = originalBytes;
			for (MemSeam trimmed : overall.values()) {
				trimmed.trimInverseTo(inverseStart, inverseAfter);
				baseCost.recordMappedBytes(Areas.length(trimmed));
			}
			return outlierCount;
		}

		private MemSeam postRange(final int rangeStart,
		                          final int rangeAfter,
		                          final boolean mainSequence,
		                          final int outlierCount) {
			// "you guy have a clean track record..."
			int rangeStartStep = outlierCount == 0 ? warmUpStepMark : indices[rangeStart];
			int rangeAfterStep = indices[rangeAfter];
			// resolve steps to addresses, add to list
			long startAddress = Rolling.Utils.hashIndexToWindowStart(rhModified, rangeStartStep);
			long endAddress = Rolling.Utils.hashIndexToAfterWindow(rhModified, rangeAfterStep);
			final int drift = drifted[rangeStart];
			return new MemSeam(startAddress, endAddress, rhModified.getSingleStepInBytes(), drift, mainSequence);
		}

		private void addToEventualTarget(MemSeam memSeam) {
			MemArea trimmed = overall.trimToUnique(memSeam);
			if (!Areas.isEmpty(trimmed)) {
				if (trimmed != memSeam) {
					memSeam.set(trimmed);
				}
				//// TODO DODODO
				(memSeam.isMainSequence() ? ordered : ooOrder).add(memSeam);
				overall.add(memSeam);
			}
		}

		public void longestGrowing(BitSet growing) {
			new LMS(stitches).subSeq(matches, growing);
		}
	}

	private final int stitches;

	// byte count
	private final int warmUpStepMark;
	private final long modifiedBytes;
	private final long originalBytes;
	
	// reportable stats
	private final int outliers; // matches ignored at 1st pass
	private final int[] drifted;

	private final BitSet growing;
	private final MemHeap<MemSeam> ordered = new MemHeap<>();
	private final MemHeap<MemSeam> ooOrder = new MemHeap<>();
	private final MemHeap<MemSeam> overall = new MemHeap<>();

	// read-only properties
	private SortedMap<MemArea, MemSeam> roOrdered = Collections.unmodifiableSortedMap(ordered);
	private SortedMap<MemArea, MemSeam> roOOOrder = Collections.unmodifiableSortedMap(ooOrder);
	private SortedMap<MemArea, MemSeam> roOverall = Collections.unmodifiableSortedMap(overall);

	public SeamDiff(Base base, RollingHash rhModified, int stitches) {
		this(base.getHash(), base.getTree(), rhModified, stitches);
	}

	SeamDiff(RollingHash rhOriginal, MetricTree tree, RollingHash rhModified, int stitches) {
		Rolling.Utils.checkHashAlgorithm(rhOriginal, rhModified);
		this.rhOriginal = rhOriginal;
		this.tree = tree;
		this.rhModified = rhModified;
		originalBytes = Rolling.Utils.byteLength(rhOriginal);
		modifiedBytes = Rolling.Utils.byteLength(rhModified);
		baseCost = newBlankCost();
		final LongBuffer computed = rhModified.computed();
		warmUpStepMark = computed.position();
		final int rhSize = computed.remaining();
		stitches = Math.min(stitches, Math.min(rhSize, rhOriginal.computed().remaining()));
		this.stitches = stitches;
		final Inquiry inquiry = new Inquiry(stitches, warmUpStepMark, rhSize, baseCost);
		drifted = inquiry.leftToRight();
		growing = new BitSet(stitches);
		inquiry.longestGrowing(growing);
		outliers = inquiry.group();
	}

	public Cost newBlankCost() {
		return new Cost(modifiedBytes);
	}

	public Cost getBaseCost() {
		return baseCost;
	}

	public void healGaps(ByteBuffer bbOriginal, ByteBuffer bbModified) {
		healGaps(newBlankCost(), bbOriginal, bbModified);
	}
	
	public void healGaps(Cost fineCost, ByteBuffer bbOriginal, ByteBuffer bbModified) {
		if (overall.isEmpty()) {
			return;
		}
		Map.Entry<MemArea, MemSeam> prev = overall.firstEntry();
		// extend first entry down to lowest possible
		final MemSeam memHead = prev.getValue();
		if (memHead.isMainSequence()) {
			final long left = memHead.getStartAddress();
			final long head = Math.min(left, left + memHead.getTranslation());
			if (head > 0) {
				memHead.set(memHead.getStartAddress() - head, memHead.getEndAddress());
			}
		}
		Map.Entry<MemArea, MemSeam> next;
		while ((next = overall.higherEntry(prev.getKey())) != null) {
			final MemSeam memPrev = prev.getValue(), memNext = next.getValue();
			final int translationPrev = (int) memPrev.getTranslation();
			final int translationNext = (int) memNext.getTranslation();
			if (translationPrev == translationNext) {
				overall.remove(memNext);
				(memNext.isMainSequence() ? ordered : ooOrder).remove(memNext);
				memPrev.set(memPrev.getStartAddress(), memNext.getEndAddress());
			} else {
				if (memPrev.isMainSequence() && memNext.isMainSequence() && translationPrev > translationNext) {
					// this is a special case, as the actual collapsible gap is smaller than we might expect:
					// obviously, we don't want to represent insertion as repetition, so we must allow a gap
					// as tight as our sources allow. this, in turn, means, that we want to "fasten" the gap
					// between the ranges of bbOriginal, rather than between the ranges of bbModified.
					MemSeam invPrev = memPrev.inverse(), invNext = memNext.inverse();
					final long startAddress = invPrev.getEndAddress();
					final long endAddress = invNext.getStartAddress();
					if (startAddress < endAddress) {
						fasten(fineCost, bbModified, bbOriginal, invPrev, invNext, -translationPrev, -translationNext);
					} // MOREINFO or do we? that's loss of information, in fact.
					else if (startAddress > endAddress) {
						loosen(fineCost, bbModified, bbOriginal, invPrev, invNext, -translationPrev, -translationNext);
					}
					memPrev.set(invPrev.inverse());
					memNext.set(invNext.inverse());
				} else {
					fasten(fineCost, bbOriginal, bbModified, memPrev, memNext, translationPrev, translationNext);
				}
				prev = next;
			}
		}
		// extend last entry up to highest possible
		final MemSeam memTail = prev.getValue();
		if (memTail.isMainSequence()) {
			final long last = memTail.getEndAddress();
			final long tail = Math.min(bbModified.capacity() - last,
					bbOriginal.capacity() - (last + memTail.getTranslation()));
			if (tail > 0) {
				memTail.set(memTail.getStartAddress(), memTail.getEndAddress() + tail);
			}
		}
	}

	private void loosen(Cost fineCost, ByteBuffer bbOriginal, ByteBuffer bbModified,
	                   MemSeam memPrev, MemSeam memNext, int translationPrev, int translationNext) {
		; // fixme actually connect the gaps
	}
	
	private void fasten(Cost fineCost, ByteBuffer bbOriginal, ByteBuffer bbModified,
	                   MemSeam memPrev, MemSeam memNext, int translationPrev, int translationNext) {
		final long startAddress = memPrev.getEndAddress();
		final long endAddress = memNext.getStartAddress();
		final int gap = (int) (endAddress - startAddress);
		if (gap > 0) {
			MemArea gapArea = new ResolvedMemArea(startAddress, endAddress);
			MemArea srcArea = new ResolvedMemArea(0, bbOriginal.capacity());
			gapArea = Areas.trim(gapArea, memPrev.inverse().translate(srcArea)); // can add to prev
			gapArea = Areas.trim(gapArea, memNext.inverse().translate(srcArea)); // can add to both
			int healedGapStart = (int) gapArea.getStartAddress();
			int healedGapAfter = (int) gapArea.getEndAddress();
			if (healedGapAfter - healedGapStart > 0) {

				Hamming hamPrev = new Hamming();
				Hamming hamNext = new Hamming();
				int aimFunction = 0;
				int aimArgument = healedGapStart;
				int inexactPrev = 0;
				int inexactNext = 0;
				int comparisons = 0;
				int adjustments = 0;
				for (int modPos = healedGapStart; modPos < healedGapAfter; ++modPos) {
					hamPrev.postByte(bbModified.get(modPos) ^ bbOriginal.get(modPos + translationPrev));
					hamNext.postByte(bbModified.get(modPos) ^ bbOriginal.get(modPos + translationNext));
					if (modPos % rhOriginal.getSingleStepInBytes() == 0) { // word boundaries
						final int prevIsACleanerMatchByBits = hamPrev.getDigits() - hamNext.getDigits();
						comparisons++;
						if (prevIsACleanerMatchByBits < aimFunction) {
							aimFunction = prevIsACleanerMatchByBits;
							aimArgument = modPos;
							inexactPrev = hamPrev.getOctets();
							inexactNext = hamNext.getOctets();
							adjustments++;
						}
					}
				}
				final int mismatches = hamNext.getOctets() - inexactNext + inexactPrev;
				fineCost.reportHealingCosts(mismatches, comparisons, adjustments);
				healedGapStart = healedGapAfter = aimArgument;
			}
			recordAndSave(fineCost, memPrev, memNext, startAddress, endAddress, healedGapStart, healedGapAfter);
		} else if (gap < 0) {
			throw new IllegalArgumentException("fasten called with a negative gap");
		}
	}

	private void recordAndSave(Cost fineCost, MemSeam memPrev, MemSeam memNext, long startAddress, long endAddress, int healedGapStart, int healedGapAfter) {
		if (healedGapStart != startAddress) {
			fineCost.recordMappedBytes(healedGapStart - startAddress);
			memPrev.set(memPrev.getStartAddress(), healedGapStart);
		}
		if (healedGapAfter != endAddress) {
			fineCost.recordMappedBytes(endAddress - healedGapAfter);
			memNext.set(healedGapAfter, memNext.getEndAddress());
		}
	}

	@Override
	public SortedMap<MemArea, MemSeam> getOrdered() {
		return roOrdered;
	}

	@Override
	public SortedMap<MemArea, MemSeam> getOOOrder() {
		return roOOOrder;
	}

	@Override
	public SortedMap<MemArea, MemSeam> getOverall() {
		return roOverall;
	}

	public int getStitchCount() {
		return stitches;
	}

	public int getOutlierCount() {
		return outliers;
	}

	public final int[] drift() {
		return drifted; // safe to expose - never used
	}
}
