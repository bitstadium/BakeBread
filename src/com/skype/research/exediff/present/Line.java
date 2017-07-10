/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.frame.Newtonian;

import java.util.BitSet;

/**
 * A single comparable line of bytes. Printable as string.
 */
public class Line {
	final int lineLength;
	final ChangeType chg;
	final BitSet defined;
	final byte[] rawData;
	
	private final StringBuilder sb;
	private boolean dirty;
	private long lineAddr;

	Line(ChangeType chg, int length) {
		this.chg = chg;
		this.lineLength = length;
		defined = new BitSet(length);
		rawData = new byte[length];
		sb = PrettyPrint.lineBuilder(length);
	}
	
	boolean hasDefined() {
		return defined.cardinality() > 0;
	}

	void readFrom(Newtonian data, long lineAddr, MemArea defArea) {
		// assert line type is INS or DEL
		this.lineAddr = lineAddr;
		long absStart = defArea.getStartAddress() + data.refPoint;
		long absAfter = defArea.getEndAddress()   + data.refPoint;
		for (int offset = 0; offset < lineLength; ++offset) {
			long addr = lineAddr + offset;
			boolean within = absStart <= addr && addr < absAfter;
			defined.set(offset, within);
			if (within) {
				rawData[offset] = data.getByte(addr);
			}
		}
		dirty = true;
	}

	void readComp(Line modified, Line original) {
		// assert line type is XOR
		if (modified.lineLength != lineLength || original.lineLength != lineLength) {
			throw new ClassCastException(byte[].class.getName());
		}
		for (int i = 0; i < lineLength; ++i) {
			final boolean def = modified.defined.get(i);
			if (def != original.defined.get(i)) {
				throw new ClassCastException(boolean[].class.getName());
			}
			boolean differ = def && original.defined.get(i) &&
					(rawData[i] = (byte) (modified.rawData[i] ^ original.rawData[i])) != 0;
			defined.set(i, differ);
		}
		dirty = true;
	}

	@Override
	public String toString() {
		if (dirty) {
			sb.delete(0, sb.length());
			sb.append(chg.prompt);
			sb.append(chg == ChangeType.SIDE_SIDE 
					? PrettyPrint.hexWordSkip 
					: PrettyPrint.hexWordSlim(lineAddr));
			sb.append(PrettyPrint.fieldDelimiter);
			PrettyPrint.hexBytes(sb, rawData, defined);
			sb.append(PrettyPrint.fieldDelimiter);
			PrettyPrint.ascBytes(sb, chg, rawData, defined);
			sb.append(PrettyPrint.fieldDelimiter);
			dirty = false;
		}
		return sb.toString();
	}
}
