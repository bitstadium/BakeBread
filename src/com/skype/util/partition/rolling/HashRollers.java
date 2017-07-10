/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.rolling;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.LongBuffer;

/**
 * Proximity hash array evaluators (factories of Impression).
 */
public enum HashRollers implements HashRoller {
	APPEND_HALFWORD(2, 4, 3) {
		@Override
		public RollingHash index(ByteBuffer source) {
			CharBuffer halfWds = source.asCharBuffer();
			final int wordCount = halfWds.capacity();
			final long[] hashes = new long[wordCount];
			LongBuffer writable = LongBuffer.wrap(hashes);

			long rolling = 0;
			while (halfWds.hasRemaining()) {
				rolling <<= 16;
				rolling |= halfWds.get();
				writable.put(rolling);
			}
			return new ResolvedRollingHash(this, hashes);
		}

	},
	TAMIEN_HALFWORD(2, 16, 15) {
		@Override
		public RollingHash index(ByteBuffer source) {
			CharBuffer halfWds = source.asCharBuffer();
			// the position of halfWds is independent on the original buffer's,
			// but the byte order is inferred and reused
			final int wordCount = halfWds.capacity();
			final long[] hashes = new long[wordCount];
			// this is mere convenience - the buffer will care of the pointer.
			LongBuffer writable = LongBuffer.wrap(hashes);
			Tamien window = new Tamien(2, 1, 32);
			while (halfWds.hasRemaining()) {
				final char value = halfWds.get(); // reading from zero
				window.put(value);
				window.put(value & 0xff00);
				writable.put(window.getTamien());
			}
			return new ResolvedRollingHash(this, writable);
		}
	};

	private final int singleStepInBytes;
	private final int windowSizeInSteps;
	private final int warmUpWindowSteps;

	HashRollers(int singleStepInBytes, int windowSizeInSteps, int warmUpWindowSteps) {
		this.singleStepInBytes = singleStepInBytes;
		this.windowSizeInSteps = windowSizeInSteps;
		this.warmUpWindowSteps = warmUpWindowSteps;
	}

	@Override
	public RollingHash index(byte[] source, int beginIndex, int endIndex, ByteOrder byteOrder) {
		return index(ByteBuffer.wrap(source, beginIndex, endIndex).order(byteOrder));
	}

	@Override
	public RollingHash index(byte[] source, ByteOrder byteOrder) {
		return index(source, 0, source.length, byteOrder);
	}

	@Override
	public Rolling getHashAlgorithm() {
		return this;
	}

	@Override
	public int getWarmUpWindowBytes() {
		return getWarmUpWindowSteps() * getSingleStepInBytes();
	}

	@Override
	public int getWarmUpWindowSteps() {
		return warmUpWindowSteps;
	}

	@Override
	public int getWindowSizeInBytes() {
		return getWindowSizeInSteps() * getSingleStepInBytes();
	}

	@Override
	public int getWindowSizeInSteps() {
		return windowSizeInSteps;
	}

	@Override
	public int getSingleStepInBytes() {
		return singleStepInBytes;
	}
}
