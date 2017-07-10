/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Reverses the byte order in the underlying DataOutput.
 */
public class ReverseEndianDataOutput implements DataOutput {
	private final DataOutput delegate;
	
	public ReverseEndianDataOutput(DataOutput DataOutput) {
		delegate = DataOutput;
	}
	
	@Override
	public void write(int b) throws IOException {
		delegate.write(b);
	}
	
	@Override
	public void write(byte[] bytes) throws IOException {
		delegate.write(bytes);
	}
	
	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		delegate.write(bytes, offset, length);
	}
	
	@Override
	public void writeBoolean(boolean value) throws IOException {
		delegate.writeBoolean(value);
	}
	
	@Override
	public void writeByte(int value) throws IOException {
		delegate.writeByte(value);
	}
	
	
	@Override
	public void writeShort(int value) throws IOException {
		delegate.writeShort(Short.reverseBytes((short) value));
	}
	
	@Override
	public void writeChar(int c) throws IOException {
		delegate.writeChar(Character.reverseBytes((char) c));
	}
	
	@Override
	public void writeInt(int value) throws IOException {
		delegate.writeInt(Integer.reverseBytes(value));
	}
	
	@Override
	public void writeLong(long value) throws IOException {
		delegate.writeLong(Long.reverseBytes(value));
	}
	
	@Override
	public void writeFloat(float value) throws IOException {
		writeInt(Float.floatToIntBits(value));
	}
	
	@Override
	public void writeDouble(double value) throws IOException {
		writeLong(Double.doubleToLongBits(value));
	}

	@Override
	public void writeBytes(String s) throws IOException {
		delegate.writeBytes(s); // single-octet ASCII
	}

	@Override
	public void writeChars(String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			writeChar(s.charAt(i)); // bi-octet
		}
	}

	@Override
	public void writeUTF(String value) throws IOException {
		// endianness must not affect UTF-8, which is normally the case.
		delegate.writeUTF(value);
	}
	
	public static ByteOrder byteOrder(DataOutput source) {
		if (source instanceof ReverseEndianDataOutput) {
			return ByteOrder.LITTLE_ENDIAN;
		} else {
			return ByteOrder.BIG_ENDIAN; // network order
		}
	}
	
	public static DataOutput reverse(DataOutput source) {
		if (source instanceof ReverseEndianDataOutput) {
			return ((ReverseEndianDataOutput) source).delegate;
		} else {
			return new ReverseEndianDataOutput(source);
		}
	}
	
	public static DataOutput ensureByteOrder(DataOutput source, ByteOrder target) {
		return (byteOrder(source) == target) ? source : reverse(source);
	}
}
