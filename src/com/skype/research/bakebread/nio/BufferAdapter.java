/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;

/**
 * Operates on buffers of arbitrary type. (This is due to
 * absence of primitive generic type parameters in Java.)
 */
public interface BufferAdapter<B extends Buffer> {
	B asWordBuffer(ByteBuffer source);
	long feed(B ib);
	void write(B ib, int pos, long word);
	
	public class Stateless {
		public static final BufferAdapter<IntBuffer> INT_BAD = new IntBufferAdapter();
		public static final BufferAdapter<CharBuffer> CHAR_BAD = new CharBufferAdapter();
	}
}
