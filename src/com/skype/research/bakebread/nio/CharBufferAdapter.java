/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * Integer buffer adapter for per-word access.
 */
public class CharBufferAdapter implements BufferAdapter<CharBuffer> {
	@Override
	public CharBuffer asWordBuffer(ByteBuffer source) {
		return source.asCharBuffer();
	}

	@Override
	public long feed(CharBuffer buf) {
		return buf.get();
	}

	@Override
	public void write(CharBuffer buf, int pos, long word) {
		buf.put(pos, (char) word);
	}
}
