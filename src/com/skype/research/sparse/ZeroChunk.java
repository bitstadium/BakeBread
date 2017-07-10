/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Fill with zeroes.
 */
public class ZeroChunk extends Chunk {
	@Override
	public void doTransfer(DataInput input, FileChannel in, TrimChannel out) throws IOException {
		ByteBuffer block = ByteBuffer.allocate(getBlockSize());
		for (int i = 0; i < getBlockCount(); ++i) {
			out.write(block);
			block.clear();
		}
	}
}
