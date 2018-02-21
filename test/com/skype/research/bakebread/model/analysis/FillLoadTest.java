/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MapInfo;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

/**
 * Test filling
 */
public class FillLoadTest extends TestCase {
	final MapInfo mapInfo = new MapInfo() {
		@Override
		public long getFileOffset() {
			return 0;
		}

		@Override
		public short[] getPartition() {
			return new short[0];
		}

		@Override
		public long getFd() {
			return 0;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public long getStartAddress() {
			return 129;
		}

		@Override
		public long getEndAddress() {
			return 256;
		}

		@Override
		public boolean isReadable() {
			return true;
		}

		@Override
		public boolean isWritable() {
			return false;
		}

		@Override
		public boolean isRunnable() {
			return true;
		}

		@Override
		public boolean isShared() {
			return false;
		}
	};
	
	public void testFilling() throws IOException {
		for (byte[] pattern : new byte[][] {
				{'A', 'B', 'C', 'D'},
				{'E', 'F'},
				{'G'},
		}) {
			validatePattern(pattern);
		}
	}

	public void validatePattern(byte[] pattern) throws IOException {
		FillLoad fillLoad = new FillLoad(mapInfo, pattern);
		assertEquals(mapInfo.getStartAddress(), fillLoad.getStartAddress());
		assertEquals(mapInfo.getEndAddress(), fillLoad.getEndAddress());
		assertFalse(fillLoad.isDumpData());
		assertFalse(fillLoad.isHostData());
		assertFalse(fillLoad.isReliable());
		validateBytes(pattern, fillLoad, writeToOutputStream(fillLoad));
		validateBytes(pattern, fillLoad, writeToLocalChannel(fillLoad));
	}

	private byte[] writeToLocalChannel(FillLoad fillLoad) throws IOException {
		final byte[] ba = new byte[(int) Areas.length(fillLoad)];
		WritableByteChannel channel = new PreReservedByteChannel(ba);
		fillLoad.getData().writeTo(channel);
		return ba;
	}

	private byte[] writeToOutputStream(FillLoad fillLoad) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		fillLoad.getData().writeTo(os);
		return os.toByteArray();
	}

	private void validateBytes(byte[] pattern, FillLoad fillLoad, byte[] bb) {
		long expectedLength = Areas.length(fillLoad);
		final String describe = Arrays.toString(pattern);
		Assert.assertEquals(describe, expectedLength, bb.length);
		for (int i = 0; i < expectedLength; ++i) {
			assertEquals(describe, pattern[(int) ((i + fillLoad.getStartAddress()) %  pattern.length)], bb[i]);
		}
	}

	private static class PreReservedByteChannel implements WritableByteChannel {
		private final byte[] ba;
		private boolean open = true;

		public PreReservedByteChannel(byte[] ba) {
			this.ba = ba;
		}

		@Override
		public int write(ByteBuffer byteBuffer) throws IOException {
			int startPosition = byteBuffer.position();
			byteBuffer.get(ba);
			return byteBuffer.position() - startPosition;
		}

		@Override
		public boolean isOpen() {
			return open;
		}

		@Override
		public void close() throws IOException {
			open = false;
		}
	}
}
