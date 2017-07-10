/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Integer buffer adapter for per-word access.
 */
public class IntBufferAdapter implements BufferAdapter<IntBuffer> {
	@Override
	public IntBuffer asWordBuffer(ByteBuffer source) {
		return source.asIntBuffer();
	}

	@Override
	public long feed(IntBuffer buf) {
		return buf.get();
	}

	@Override
	public void write(IntBuffer buf, int pos, long word) {
		buf.put(pos, (int) word);
	}
}
