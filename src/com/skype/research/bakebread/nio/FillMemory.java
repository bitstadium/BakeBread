/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import com.skype.research.bakebread.model.memory.MemArea;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Fill with pattern.
 */
public class FillMemory implements Memory {

	private final byte[] pattern;
	private final MemArea memArea;
	private final ByteBuffer buf;

	public FillMemory(byte[] pattern, MemArea memArea) {
		if (pattern == null) throw new NullPointerException("pattern");
		this.pattern = new byte[BLOCK_SIZE]; // a reasonably small pow(2)
		System.arraycopy(pattern, 0, this.pattern, 0, pattern.length);
		for (int i = pattern.length; i < this.pattern.length; i += i) {
			System.arraycopy(this.pattern, 0, this.pattern, i, i);
		}
		this.memArea = memArea;
		buf = ByteBuffer.wrap(this.pattern);
	}

	@Override
	public long writeTo(OutputStream outputStream) throws IOException {
		return writeTo(new StreamSink(outputStream));
	}

	public long writeTo(Sink sink) throws IOException {
		long cur = memArea.getStartAddress();
		long end = memArea.getEndAddress();
		while (cur < end) {
			int pos = (int) (cur & (BLOCK_SIZE - 1));
			int len = (int) Math.min(BLOCK_SIZE - pos, end - cur);
			sink.write(pos, len);
			cur += len;
		}
		return cur - memArea.getStartAddress();
	}

	@Override
	public long writeTo(WritableByteChannel channel) throws IOException {
		return writeTo(new ChannelSink(channel));
	}

	@Override
	public Memory transform(MemArea from, MemArea to) {
		return new FillMemory(pattern, to);
	}

	private interface Sink {
		void write(int pos, int len) throws IOException;
	}
	
	private class StreamSink implements Sink {

		private final OutputStream outputStream;

		private StreamSink(OutputStream outputStream) {
			this.outputStream = outputStream;
		}

		@Override
		public void write(int pos, int len) throws IOException {
			outputStream.write(pattern, pos, len);
		}
	}
	
	private class ChannelSink implements Sink {
		private final WritableByteChannel channel;

		public ChannelSink(WritableByteChannel channel) {
			this.channel = channel;
		}

		@Override
		public void write(int pos, int len) throws IOException {
			buf.position(pos).limit(pos + len);
			int cum = 0;
			do cum += channel.write(buf); while (cum < len);
		}
	}

	@Override
	public String toString() {
		return String.format("fill:%08X", buf.getInt(0));
	}
}
