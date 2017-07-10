/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * Conversions between various I/O primitives.
 */
public class FromToUtils {
	public static Reader asAsciiReader(byte[] section) throws UnsupportedEncodingException {
		return new InputStreamReader(new ByteArrayInputStream(section), "ASCII");
	}
	
	public static DataInput asDataInput(byte[] buf) {
		return new DataInputStream(new ByteArrayInputStream(buf));
	}

	public static ByteBuffer asByteBuffer(IntBuffer src, ByteOrder desired) {
		ByteBuffer bb = ByteBuffer.allocate(src.capacity() * 4).order(desired);
		src = src.duplicate();
		src.clear();
		bb.asIntBuffer().put(src);
		return bb;
	}
	public static ByteBuffer asByteBuffer(LongBuffer src, ByteOrder desired) {
		ByteBuffer bb = ByteBuffer.allocate(src.capacity() * 8).order(desired);
		src = src.duplicate();
		src.clear();
		bb.asLongBuffer().put(src);
		return bb;
	}
	
	public static void marshal(DataOutput dataOutput, IntBuffer intBuffer) throws IOException {
		int oldPos = intBuffer.position();
		while (intBuffer.hasRemaining()) {
			dataOutput.writeInt(intBuffer.get());
		}
		intBuffer.position(oldPos);
	}
	
	public static void marshal(DataOutput dataOutput, LongBuffer longBuffer) throws IOException {
		int oldPos = longBuffer.position();
		while (longBuffer.hasRemaining()) {
			dataOutput.writeLong(longBuffer.get());
		}
		longBuffer.position(oldPos);
	}
}
