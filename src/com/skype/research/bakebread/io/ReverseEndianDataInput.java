/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Reverses the byte order in the underlying DataInput.
 */
public class ReverseEndianDataInput implements DataInput {
	private final DataInput delegate;
	
	public ReverseEndianDataInput(DataInput dataInput) {
		delegate = dataInput;
	}
	
	@Override
	public void readFully(byte[] bytes) throws IOException {
		delegate.readFully(bytes);
	}
	
	@Override
	public void readFully(byte[] bytes, int offset, int length) throws IOException {
		delegate.readFully(bytes, offset, length);
	}
	
	@Override
	public int skipBytes(int i) throws IOException {
		return delegate.skipBytes(i);
	}
	
	@Override
	public boolean readBoolean() throws IOException {
		return delegate.readBoolean();
	}
	
	@Override
	public byte readByte() throws IOException {
		return delegate.readByte();
	}
	
	@Override
	public int readUnsignedByte() throws IOException {
		return delegate.readUnsignedByte();
	}
	
	@Override
	public short readShort() throws IOException {
		return Short.reverseBytes(delegate.readShort());
	}
	
	@Override
	public int readUnsignedShort() throws IOException {
		return readChar();
	}
	
	@Override
	public char readChar() throws IOException {
		return Character.reverseBytes(delegate.readChar());
	}
	
	@Override
	public int readInt() throws IOException {
		return Integer.reverseBytes(delegate.readInt());
	}
	
	@Override
	public long readLong() throws IOException {
		return Long.reverseBytes(delegate.readLong());
	}
	
	@Override
	public float readFloat() throws IOException {
		return Float.intBitsToFloat(readInt());
	}
	
	@Override
	public double readDouble() throws IOException {
		return Double.longBitsToDouble(readLong());
	}
	
	@Override
	public String readLine() throws IOException {
		return delegate.readLine();
	}
	
	@Override
	public String readUTF() throws IOException {
		// endianness must not affect UTF-8, which is normally the case.
		return delegate.readUTF();
	}
	
	public static ByteOrder byteOrder(DataInput source) {
		if (source instanceof ReverseEndianDataInput) {
			return ByteOrder.LITTLE_ENDIAN;
		} else {
			return ByteOrder.BIG_ENDIAN; // network order?
		}
	}
	
	public static DataInput reverse(DataInput source) {
		if (source instanceof ReverseEndianDataInput) {
			return ((ReverseEndianDataInput) source).delegate;
		} else {
			return new ReverseEndianDataInput(source);
		}
	}
	
	public static DataInput ensureByteOrder(DataInput source, ByteOrder target) {
		return (byteOrder(source) == target) ? source : reverse(source);
	}
}
