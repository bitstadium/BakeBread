/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.trimmer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

/**
 * Wraps write requests to a FileChannel to make sure the correct number of bytes is skipped.
 * 
 * FIXME this guy needs a unit test set
 */
public class TrimChannel implements SeekableByteChannel {
	private FileChannel channel;
	private long trimTo = 0;
	private long realPos = 0;

	public void setChannel(FileChannel channel) throws IOException {
		this.channel = channel;
		this.realPos = channel.position();
	}

	@Override
	public long position() {
		return realPos + trimTo;
	}

	@Override
	public SeekableByteChannel position(long position) throws IOException {
		position -= trimTo;
		realPos = position;
		if (position < 0) {
			position = 0;
		}
		channel.position(position);
		return this;
	}

	public void ensureTrim(long trimTo) throws NeedTrimmingException, IOException {
		if (this.trimTo != trimTo) {
			this.trimTo = trimTo;
			truncate(trimTo);
			throw new NeedTrimmingException(trimTo);
		}
	}

	public long size() throws IOException {
		return channel.size() + trimTo;
	}

	public SeekableByteChannel truncate(long position) throws IOException {
		position -= trimTo;
		if (realPos > position) {
			realPos = position;
		}
		if (position < 0) {
			position = 0;
		}
		channel.truncate(position);
		return this;
	}

	public void force(boolean withMeta) throws IOException {
		channel.force(withMeta);
	}

	@Override
	public int read(ByteBuffer byteBuffer) throws IOException {
		throw new UnsupportedOperationException("Write-only");
	}

	public int write(ByteBuffer buffer) throws IOException {
		final int written = buffer.remaining();
		if (realPos < 0) {
			long skip = - realPos;
			int more = (int) Math.min(skip, written);
			realPos += more;
			buffer.position(buffer.position() + more);
		}
		if (buffer.hasRemaining()) {
			realPos += buffer.remaining();
			channel.write(buffer);
		}
		return written;
	}

	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	public void transferFrom(FileChannel in, long offset, long length) throws IOException {
		if (realPos + length < 0) {
			realPos += length;
			return;
		}
		if (realPos < 0) {
			offset -= realPos;
			length += realPos;
			realPos = 0;
		}
		in.transferTo(offset, length, channel);
		realPos += length;
	}
}
