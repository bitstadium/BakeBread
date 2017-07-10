/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.io.PackedString;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Pointer size, sizes of various related structures. 
 * TODO: should be reworked into a Cartesian product.
 */
public enum PtrSize implements SizeOf {
	NONE(0),
	INT(4) {
		@Override
		public long readPtr(DataInput dataInput) throws IOException {
			return dataInput.readInt();
		}

		@Override
		public void writePtr(DataOutput dataInput, long word) throws IOException {
			dataInput.writeInt((int) word);
		}
	},
	LONG(8) {
		@Override
		public long readPtr(DataInput dataInput) throws IOException {
			return dataInput.readLong();
		}

		@Override
		public void writePtr(DataOutput dataInput, long word) throws IOException {
			dataInput.writeLong(word);
		}
	};

	PtrSize(int ptrSize) {
		this.pointerSize = (char) ptrSize;
		this.fileHeaderSize = (char) (ELF_IDENT
				+ HALF_SIZE * 2
				+ WORD_SIZE
				+ ptrSize * 3
				+ WORD_SIZE
				+ HALF_SIZE * 6);
		// WISDOM placement of flags is different in 32-bit and 64-bit program header!
		this.progHeaderSize = (char) (
				WORD_SIZE
						+ ptrSize * 3
						+ WORD_SIZE
						+ ptrSize * 3);
		this.sectHeaderSize = (char) (
				WORD_SIZE * 2
						+ ptrSize * 4
						+ WORD_SIZE * 2
						+ ptrSize * 2);
	}

	public byte[] readRawBuf(DataInput dataInput, int len) throws IOException {
		byte[] name = new byte[roundUp(len)];  // four???
		dataInput.readFully(name);
		return name;
	}

	public PackedString readPacked(DataInput dataInput, int len) throws IOException {
		return new PackedString(readRawBuf(dataInput, len), 0, len);
	}

	public void writePadded(DataOutput dataOutput, byte[] sequence) throws IOException {
		dataOutput.write(sequence);
		writePadding(dataOutput, sequence.length);
	}
	
	public void writePadded(DataOutput dataOutput, CharSequence sequence) throws IOException {
		// EQV to DataOutputStream.writeBytes()
		int length = sequence.length();
		for (int i = 0; i < length; ++i) {
			dataOutput.writeByte(sequence.charAt(i));
		}
		writePadding(dataOutput, length);
	}

	public void writePadding(DataOutput dataOutput, int length) throws IOException {
		int padding = getPadding(length);
		for (int i = 0; i < padding; ++i) {
			dataOutput.writeByte(0);
		}
	}

	public long readPtr(DataInput dataInput) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void writePtr(DataOutput dataInput, long word) throws IOException {
		throw new UnsupportedOperationException();
	}

	public final char pointerSize;
	public final char fileHeaderSize;
	public final char progHeaderSize;
	public final char sectHeaderSize;

	public int roundUp(int size) {
		final int bits = pointerSize - 1;
		return (size + bits) & ~bits;
	}

	public int getPadding(int length) {
		return roundUp(length) - length;
	}
}
