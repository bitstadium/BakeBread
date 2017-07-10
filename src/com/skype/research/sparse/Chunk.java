/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.sparse.trimmer.NeedTrimmingException;
import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Denotes a block of data packed in a specific way.
 */
public abstract class Chunk implements Marshaled {

	private long blockCount; // out
	private long sourceBytes; // in

	// Presume the header has been read. Read packed.
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		// 4 bytes with magic
		dataInput.readShort(); // padding
		blockCount = dataInput.readInt();
		sourceBytes = dataInput.readInt();
		// 12 bytes with magic
	}

	// Write unpacked.
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		throw new UnsupportedOperationException();
	}

	private short headerSize;
	private int blockSize;

	public void setHeaderSize(short headerSize) {
		this.headerSize = headerSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public long getBlockCount() {
		return blockCount;
	}

	public long getTargetBytes() {
		return blockCount * blockSize;
	}

	public final long transfer(DataInput input, FileChannel in, TrimChannel out) throws IOException, NeedTrimmingException {
		input.skipBytes(headerSize - 12);
		long beginHeader = in.position() - headerSize;
		long beginTarget = out.position();
		doTransfer(input, in, out);
		long padding = beginHeader + sourceBytes - in.position();
		input.skipBytes((int) padding);
		assertPosition(beginHeader + sourceBytes, in.position());
		assertPosition(beginTarget + getTargetBytes(), out.position());
		return getTargetBytes();
	}

	private static void assertPosition(long expected, long actual) throws IOException {
		if (actual != expected) {
			throw new IllegalStateException(String.format("Position: expected %d actual %d", expected, actual));
		}
	}

	protected abstract void doTransfer(DataInput input, FileChannel in, TrimChannel out) throws IOException, NeedTrimmingException;

	@Override
	public String toString() {
		return String.format("%10d (b) -> %10d (b)", sourceBytes, getTargetBytes());
	}
}
