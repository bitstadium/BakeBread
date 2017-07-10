/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.io.Marshaled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Relative Virtual Address. Essentially, an unsigned int primitive wrapper.
 */
public class RVA implements Marshaled {
	private long virtualAddress;
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt((int) virtualAddress);
	}
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		virtualAddress = 0xffffffffL & dataInput.readInt();
	}
	
	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof RVA && virtualAddress == ((RVA) o).virtualAddress;
	}
	
	@Override
	public int hashCode() {
		return (int) (virtualAddress ^ (virtualAddress >>> 32));
	}
	
	@Override
	public String toString() {
		return PrettyPrint.hexWordSlim(virtualAddress);
	}
	
	public long getVirtualAddress() {
		return virtualAddress;
	}
	
	public void navigate(FileChannel fileChannel) throws IOException {
		fileChannel.position(virtualAddress);
	}
}
