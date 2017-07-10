/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.memory.MemArea;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * File position and offset.
 */
public class FileMemory implements Memory {

	private final ChannelSource channelSource;
	private final long offset;
	private final long length;

	public FileMemory(FileChannel channel, long offset, long length) {
		channelSource = new ProvidedChannel(channel);
		this.offset = offset;
		this.length = length;
	}
	
	public FileMemory(File file, long offset, long length, AutoClose autoClose) {
		channelSource = new ObtainedChannel(file, autoClose);
		this.offset = offset;
		this.length = length;
	}
	
	private FileMemory(ChannelSource channelSource, long offset, long length) {
		this.channelSource = channelSource;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public long writeTo(OutputStream outputStream) throws IOException {
		FileChannel channel = channelSource.open();
		channel.position(offset);
		byte[] tmp = new byte[(int) Math.min(BLOCK_SIZE, length)];
		ByteBuffer buf = ByteBuffer.wrap(tmp);
		long transferred = 0;
		while (transferred < length) {
			buf.clear().limit((int) Math.min(length - transferred, buf.capacity()));
			int payload = channel.read(buf);
			if (payload < 0) { // paranoid
				break;
			}
			outputStream.write(tmp, 0, payload);
			transferred += payload;
		}
		return transferred;
	}

	@Override
	public long writeTo(WritableByteChannel channel) throws IOException {
		return channelSource.open().transferTo(offset, length, channel);
	}

	@Override
	public Memory transform(MemArea from, MemArea to) {
		if (Areas.length(from) != length) {
			throw new IllegalArgumentException("Original length");
		}
		if (Areas.areasEqual(from, to)) {
			return this;
		}
		long adjustedOffset = this.offset + to.getStartAddress() - from.getStartAddress();
		return new FileMemory(channelSource, adjustedOffset, Areas.length(to));
	}

	private interface ChannelSource {
		FileChannel open() throws FileNotFoundException;
	}

	private static class ProvidedChannel implements ChannelSource {
		final FileChannel channel;

		private ProvidedChannel(FileChannel channel) {
			this.channel = channel;
		}

		@Override
		public FileChannel open() {
			return channel;
		}

		@Override
		public String toString() {
			return "<open-file>";
		}
	}

	private static class ObtainedChannel implements ChannelSource {
		private final File file;
		private final AutoClose autoClose;
		private FileChannel channel;

		private ObtainedChannel(File file, AutoClose autoClose) {
			this.file = file;
			this.autoClose = autoClose;
		}

		@Override
		public synchronized FileChannel open() throws FileNotFoundException {
			if (channel == null) {
				channel = autoClose.register(new FileInputStream(file).getChannel());
			}
			return channel;
		}

		@Override
		public String toString() {
			return file.getName();
		}
	}

	@Override
	public String toString() {
		return String.format("file:%s@%x+%x", channelSource.toString(), offset, length);
	}
}
