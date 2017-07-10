/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemPerm;
import com.skype.research.exediff.present.ChangeType;

import java.util.BitSet;

/**
 * Pretty printing conventions resulting from architecture and ABI conventions
 */
public class PrettyPrint {
	private PrettyPrint() {}

	public static final String hexWordSkip  = "        "; // 8 spaces
	public static final char[] hexDigits    = "0123456789abcdef".toCharArray();
	public static final char fieldDelimiter = '|';
	public static final char nonPrintableCh = '.';
	public static final int  allCharPerByte = 4;
	public static final int  addCharPerLine = 12;

	public static String hexWord(long word) {
		return String.format("0x%08x", (int) word);
	}
	
	public static String hexDWord(long dWord) {
		return String.format("0x%016x", dWord);
	}
	
	public static String hexWordSlim(long word) {
		return String.format("%08x", (int) word);
	}

	public static String hexDWordSlim(long dWord) {
		return String.format("%016x", dWord);
	}
	
	public static String padTo(String string, int paddedTo) {
		final StringBuilder sb = new StringBuilder(string);
		while (sb.length() < paddedTo) {
			sb.append(' ');
		}
		return sb.toString();
	}

	public static String hexRangeSlim(MemArea memArea) {
		return hexRangeSlim(memArea.getStartAddress(), memArea.getEndAddress());
	}

	public static String hexRangeSlim(MemArea memArea, long groundLevel) {
		return hexRangeSlim(memArea.getStartAddress() + groundLevel, memArea.getEndAddress() + groundLevel);
	}

	public static String hexRangeSlim(long startAddress, long endAddress) {
		return hexWordSlim(startAddress) + '-' + hexWordSlim(endAddress);
	}

	public static String printFlags(MemPerm memPerm) {
		char[] flags = new char[4];
		flags[0] = memPerm.isReadable() ? 'r' : '-';
		flags[1] = memPerm.isWritable() ? 'w' : '-';
		flags[2] = memPerm.isRunnable() ? 'x' : '-';
		flags[3] = memPerm.isShared()   ? 's' : 'p';
		return new String(flags);
	}

	public static StringBuilder lineBuilder(int length) {
		return new StringBuilder(length * allCharPerByte + addCharPerLine);
	}
	
	public static void hexBytes(StringBuilder sb, byte[] rawData, BitSet defined) {
		int i = 0;
		boolean d;
		byte b;
		while (true) {
			d = defined.get(i);
			b = rawData[i];
			sb.append(d ? hexDigits[(b >> 4) & 0xf] : ' ');
			sb.append(d ? hexDigits[b & 0xf]        : ' ');
			if (++i == rawData.length) break;
			sb.append(' ');
		}
	}

	public static void ascBytes(StringBuilder sb, ChangeType changeType, byte[] rawData, BitSet defined) {
		for (int i = 0; i < rawData.length; i++) {
			if (defined.get(i)) {
				sb.append(changeType.printable((int) rawData[i]));
			} else {
				sb.append(' ');
			}
		}
	}

	public static char printable(char c) {
		return c < ' ' || c > 0x80 ? nonPrintableCh : c;
	}

	public static float percents(long total, long per) {
		return 100.f * total / per;
	}

	public static float fraction(long total, long per) {
		return 1.f * total / per;
	}
}
