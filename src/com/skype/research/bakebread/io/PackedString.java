/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.nio.charset.Charset;

/**
 * Represents a US-ASCII string tightly packed into a byte array.
 */
public class PackedString implements CharSequence {
	static final Charset US_ASCII = Charset.forName("US-ASCII");
	
	private byte[] data;
	private int from, to;
	private int extent;

	public static final PackedString EMPTY = new PackedString(new byte[0]);

	public PackedString(byte[] data) {
		this(data, 0, data.length);
	}

	public PackedString(byte[] data, int from, int to) {
		if (from < 0 || to < from || to > data.length) {
			throw new ArrayIndexOutOfBoundsException();
		}
		this.data = data;
		this.from = from;
		this.to = to;
		this.extent = 0;
	}

	public PackedString(String name) {
		this(name.getBytes(US_ASCII));
		this.extent = 1; // c_str
	}

	@Override
	public int length() {
		return to - from + extent;
	}

	@Override
	public char charAt(int i) {
		int abs = i + from;
		if (abs >= to + extent) {
			throw new StringIndexOutOfBoundsException(i);
		}
		return abs >= to ? 0 : (char) (data[abs] & 0xff);
	}

	public PackedString subSequence(int beginIndex) {
		int endIndex = indexOf('\0', beginIndex);
		return subSequence(beginIndex, endIndex);
	}

	public int indexOf(char c, int beginIndex) {
		while (beginIndex < length() && charAt(beginIndex) != c) {
			++ beginIndex;
		}
		return beginIndex;
	}

	public boolean startsWith(CharSequence sequence) {
		if (sequence.length() > length()) {
			return false;
		}
		for (int i = 0; i < sequence.length(); ++i) {
			if (sequence.charAt(i) != charAt(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public PackedString subSequence(int beginIndex, int endIndex) {
		if (endIndex < beginIndex) {
			throw new NegativeArraySizeException();
		}
		if (beginIndex < 0 || from + endIndex >= to) {
			throw new StringIndexOutOfBoundsException();
		}
		return new PackedString(data, from + beginIndex, from + endIndex);
	}

	@Override
	public String toString() {
		return new String(data, from, to - from, Charset.forName("US-ASCII"));
	}
}
