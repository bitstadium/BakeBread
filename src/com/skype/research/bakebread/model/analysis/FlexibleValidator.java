/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.exediff.AbstractDiffFacade;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.match.Cost;
import com.skype.research.exediff.match.Diff;
import com.skype.research.exediff.match.SeamBase;
import com.skype.research.exediff.match.SeamDiff;
import com.skype.research.exediff.present.DamageMeter;
import com.skype.research.exediff.present.HammingStat;
import com.skype.research.exediff.present.Hexualizer;
import com.skype.research.exediff.present.PrettyTotal;
import com.skype.research.exediff.present.Quality;
import com.skype.util.partition.rolling.RollingHash;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.InputMismatchException;

public class FlexibleValidator extends AbstractDiffFacade implements Validator {

	private final boolean relax;

	public FlexibleValidator(boolean relax) {
		this.relax = relax;
	}

	@Override
	public void compare(MemLoad modified, MemLoad original) {
		// WISDOM the linker may have modified .data.rel.ro, so skip it
		// WISDOM ModuleAnalysisType.ELF => relink .data.rel.ro to real
		// WISDOM http://www.airs.com/blog/archives/189 .text & .rodata
		if (!modified.getMapInfo().isRunnable()) {
			return; // until relro
		}
		// inlining Areas.bytesEqual(l, r) here:
		// we will need multiple outputs from it
		// and I am not quite ready for a lambda
		// allocation here - will be too complex
		MemArea common = modified.trimTo(original);
		if (!Areas.isEmpty(common)) {
			try {
				byte[] baModified = Areas.windowToByteArray(modified, common);
				byte[] baOriginal = Areas.windowToByteArray(original, common);
				if (Arrays.equals(baModified, baOriginal)) {
					return;
				}
				// hash
				ByteBuffer bbOriginal = ByteBuffer.wrap(baOriginal);
				ByteBuffer blOriginal = bleach(bbOriginal);
				final RollingHash rhOriginal = roller.index(blOriginal);
				final SeamBase seamBase = new SeamBase(rhOriginal, metric);
				// diff
				ByteBuffer bbModified = ByteBuffer.wrap(baModified);
				ByteBuffer blModified = bleach(bbModified);
				final RollingHash rhModified = roller.index(blModified);
				final SeamDiff diff = seamBase.approximate(rhModified);
				// heal
				final Cost fineCost = diff.newBlankCost();
				if (greedyHeal) {
					diff.healGaps(fineCost, bbOriginal, bbModified);
				}
				// verify
				final Einsteinian frame = new Einsteinian(
						bbOriginal, deduceReferencePoint(original, common),
						bbModified, deduceReferencePoint(modified, common)
				);
				HammingStat stat = new HammingStat(diff, frame);
				Quality quality = new DamageMeter(thresholds).assess(diff, stat);
				if (summarize) {
					diffplay.println(HAMMING_INFO);
					PrettyTotal.displayDetailedHamming(diffplay, stat);
					PrettyTotal.displayBulkMappedBytes(diffplay, stat);
					PrettyTotal.displayOutlierPercents(diffplay, diff);
				}
				if (showCosts) {
					diffplay.println(COST_INITIAL);
					diffplay.println(diff.getBaseCost());
					diffplay.println(COST_HEALING);
					diffplay.println(fineCost);
				}
				diffplay.printf(__KEY_VALUE_, MAPPING_INFO, modified.getMapInfo());
				diffplay.printf(__KEY_VALUE_, QUALITY_INFO, quality);
				Diff show;
				if (quality.isGood()) {
					show = diff;
				} else {
					if (relax) {
						stat = new HammingStat(frame);
						show = new HammingDiff(frame);
					} else {
						throw new InputMismatchException(modified + " != " + original);
					}
				}
				//noinspection SynchronizeOnNonFinalField
				synchronized (diffFile) {
					FileWriter fw = new FileWriter(diffFile, true);
					PrintWriter writer = new PrintWriter(fw);
					// write header
					writer.printf(__KEY_VALUE_, MAPPING_INFO, modified.getMapInfo());
					writer.printf(__KEY_VALUE_, QUALITY_INFO, quality);
					writer.println(INTRODUCTION);
					writer.println("--- " + original.getData());
					writer.println("+++ " + modified.getData());
					// write data
					Hexualizer hexualizer = new Hexualizer(writer, frame);
					// I don't want to suppress displaced display by now
					hexualizer.displayLineByteChanges(show, stat, true);
					writer.println();
					writer.flush();
					writer.close();
				}
			} catch (IOException ioe) {
				throw new RuntimeException(modified + " <> " + original, ioe);
			} finally {
				diffplay.flush();
			}
		}
	}

	private static long deduceReferencePoint(MemLoad original, MemArea trimmedTo) {
		final long startAddress = trimmedTo.getStartAddress();
		if (original.isDumpData()) {
			return startAddress;
		} else if (original.isHostData()) {
			final MapInfo mapInfo = original.getMapInfo();
			return mapInfo.getFileOffset() + startAddress - mapInfo.getStartAddress();
		} else {
			return 0; // beginning of the chunk itself
		}
	}
}