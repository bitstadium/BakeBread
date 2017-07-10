/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.memory.MemArea;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * A data source backed by a NIO buffer.
 */
public class ByteMemory implements Memory {
	private final ByteBuffer buf;

	public ByteMemory(ByteBuffer buf) {
		this.buf = buf;
	}

	@Override
	public long writeTo(OutputStream outputStream) throws IOException {
		if (buf.hasArray()) {
			outputStream.write(buf.array(), buf.arrayOffset(), buf.capacity());
		} else if (outputStream instanceof FileOutputStream) { 
			writeTo(((FileOutputStream) outputStream).getChannel());
		} else {
			for (int i = 0; i < buf.capacity(); ++i) {
				outputStream.write(buf.get(i));
			}
		}
		return buf.capacity();
	}

	@Override
	public long writeTo(WritableByteChannel channel) throws IOException {
		int all = buf.capacity(), cum = 0;
		do cum += channel.write(buf); while (cum < all);
		return all;
	}

	@Override
	public Memory transform(MemArea from, MemArea to) {
		if (Areas.length(from) != buf.capacity()) {
			throw new IllegalArgumentException("Original length mismatch");
		}
		buf.position((int) (to.getStartAddress() - from.getStartAddress()));
		buf.limit((int) (to.getEndAddress() - from.getStartAddress()));
		return new ByteMemory(buf.slice());
	}
}
