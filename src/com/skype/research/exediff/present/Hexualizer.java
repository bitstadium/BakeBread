/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.MutableMemArea;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.frame.Newtonian;
import com.skype.research.exediff.match.Diff;
import com.skype.research.exediff.match.Hamming;
import com.skype.research.exediff.model.MemHole;
import com.skype.research.exediff.model.MemPair;
import com.skype.research.exediff.model.MemSeam;

import java.io.PrintWriter;
import java.util.SortedMap;

public class Hexualizer {
	static final int LINE_LENGTH_AND_ALIGNMENT = 0x10;
	static final long NOT_A_TRANSLATION = Double.doubleToLongBits(Double.NaN);

	public final PrintWriter writer;
	public final Einsteinian matter;
	
	final Line ins, del, xor;
	
	private final transient MutableMemArea l = new MutableMemArea(), r = new MutableMemArea();

	public Hexualizer(PrintWriter writer, Einsteinian refFrm) {
		this.writer = writer;
		this.matter = refFrm;

		ins = new Line(ChangeType.INSERTION, LINE_LENGTH_AND_ALIGNMENT);
		del = new Line(ChangeType.DELETION,  LINE_LENGTH_AND_ALIGNMENT);
		xor = new Line(ChangeType.SIDE_SIDE, LINE_LENGTH_AND_ALIGNMENT);
	}

	public void displayLineByteChanges(Diff diff, HammingStat stat, boolean showDisplaced) {
		// MOREINFO apply special treatment to first, given head drift known? no, we are pedantic
		long lastModified = 0, lastOriginal = 0;
		for (MemSeam modSeam : diff.getOrdered().values()) {
			MemSeam inverse = modSeam.inverse();
			MemHole memHole = new MemHole(lastModified, modSeam.getStartAddress(), lastOriginal, inverse.getStartAddress());
			displayMemoryHole(memHole, diff.getOOOrder(), stat, showDisplaced);
			displaySideBySide(modSeam, stat.getSubOrdered().get(modSeam));
			lastModified = modSeam.getEndAddress();
			lastOriginal = inverse.getEndAddress();
		}
		MemHole memHole = new MemHole(lastModified, matter.modified.bb.capacity(), lastOriginal, matter.original.bb.capacity());
		displayMemoryHole(memHole, diff.getOOOrder(), stat, showDisplaced);
	}

	private void displayMemoryHole(MemHole memHole, SortedMap<MemArea, MemSeam> ooOrder, HammingStat stat, boolean showDisplaced) {
		if (memHole.isNontrivial()) {
			displayUnmatchedHeader(memHole);
			if (memHole.isSomeDelete()) {
				displaySingleArea(del, memHole.inverse(), matter.original);
			}
			if (memHole.isSomeInsert()) {
				displaySingleArea(ins, memHole, matter.modified);
			}
			if (showDisplaced) {
				l.set(memHole.getStartAddress(), memHole.getStartAddress());
				r.set(memHole.getEndAddress(), memHole.getEndAddress());
				SortedMap<MemArea, MemSeam> subMap = ooOrder.subMap(l, r);
				if (!subMap.isEmpty()) {
					for (MemSeam memSeam : subMap.values()) {
						displaySideBySide(memSeam, stat.getSubOOOrder().get(memSeam));
					}
				}
			}
		}
	}

	private void displaySideBySideHeader(MemSeam memPair, Hamming hamming) {
		writer.print("@@");
		displayInsertDelete(memPair);
		displayTranslation(memPair);
		displayHammingStat(hamming);
		writer.print(' ');
		writer.println("@@");
	}

	private void displayUnmatchedHeader(MemHole memPair) {
		writer.print("@@");
		displayInsertDelete(memPair);
		writer.print(' ');
		writer.println("@@");
	}

	private void displayInsertDelete(MemPair<?> memPair) {
		displayEditInfo(memPair.inverse(), matter.original, ChangeType.DELETION);
		displayEditInfo(memPair, matter.modified, ChangeType.INSERTION);
	}

	private void displayEditInfo(MemPair<?> memPair, Newtonian frame, ChangeType changeType) {
		final long length = Areas.length(memPair);
		if (length != 0) { // allow reverse motion
			writer.print(' ');
			writer.print(changeType.prompt);
			writer.print(PrettyPrint.hexWordSlim(memPair.getStartAddress() + frame.refPoint));
			writer.print(',');
			writer.print(length);
		}
	}

	private void displayHammingStat(Hamming hamming) {
		final int octets = hamming.getOctets();
		if (octets != 0) {
			writer.print(' ');
			writer.print(ChangeType.SIDE_SIDE.prompt);
			writer.print(octets);
			writer.print('\'');
			writer.print(hamming.getDigits());
			writer.print('\"');
		}
	}

	private void displayTranslation(MemSeam memPair) {
		final long translation = memPair.isMainSequence() ? memPair.getTranslation() : NOT_A_TRANSLATION;
		if (translation != 0) {
			writer.print(' ');
			writer.print(ChangeType.SIDE_SKEW.prompt);
			if (translation == NOT_A_TRANSLATION) {
				writer.print(ChangeType.RELOCATED.prompt);
			} else {
				writer.printf("%+d", -translation); // from original to modified
			}
		}
	}

	private void displaySideBySide(MemSeam memSeam, Hamming hamming) {
		final boolean edit = hamming.getOctets() > 0;
		final boolean skew = memSeam.getTranslation() != 0;
		final boolean main = memSeam.isMainSequence();
		if (!Areas.isEmpty(memSeam) && (edit || skew || !main)) {
			displaySideBySideHeader(memSeam, hamming);
			if (edit) {
				final MemSeam inverse = memSeam.inverse();
				final long firstAddr = matter.modified.refPoint + memSeam.getStartAddress();
				final long firstLine = firstAddr - (firstAddr % LINE_LENGTH_AND_ALIGNMENT);
				final long lastAfter = matter.modified.refPoint + memSeam.getEndAddress();
				final long translation = memSeam.getTranslation() + matter.getTranslation();
				for (long line = firstLine; line < lastAfter; line += LINE_LENGTH_AND_ALIGNMENT) {
					ins.readFrom(matter.modified, line, memSeam);
					del.readFrom(matter.original, line + translation, inverse);
					xor.readComp(ins, del);
					if (xor.hasDefined()) {
						writer.println(del);
						writer.println(ins);
						writer.println(xor);
					}
				}
			}
		}
	}

	private void displaySingleArea(Line ln, MemArea memArea, Newtonian data) {
		long firstAddr = data.refPoint + memArea.getStartAddress();
		long firstLine = firstAddr - (firstAddr % LINE_LENGTH_AND_ALIGNMENT);
		long lastAfter = data.refPoint + memArea.getEndAddress();
		for (long line = firstLine; line < lastAfter; line += LINE_LENGTH_AND_ALIGNMENT) {
			ln.readFrom(data, line, memArea);
			assert ln.hasDefined();
			writer.println(ln);
		}
	}
}
