/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.ResolvedMemData;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.nio.FileMemory;
import com.skype.research.bakebread.nio.Memory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * A POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms680384(v=vs.85).aspx
 */
public class MemoryStream implements Marshaled, MemData {
	private long address;
	private final LocationDescription ld = new LocationDescription();
	private FileChannel channel;

	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		address = dataInput.readLong();
		ld.readExternal(dataInput, fileChannel);
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeLong(address);
		ld.writeExternal(dataOutput, fileChannel);
	}
	
	@Override
	public long getStartAddress() {
		return address;
	}

	@Override
	public long getEndAddress() {
		return address + ld.getDataSize();
	}

	@Override
	public Memory getData() {
		return new FileMemory(channel, ld.getRVA().getVirtualAddress(), ld.getDataSize());
	}

	@Override
	public MemData trimTo(MemArea memArea) {
		MemArea common = Areas.trim(this, memArea);
		if (common == this) {
			return this;
		}
		return new ResolvedMemData(common, getData().transform(this, common));
	}

	public LocationDescription getLocationDescription() {
		return ld;
	}
	
	@Override
	public String toString() {
		return String.format("%08x:%08x", address, address + getLocationDescription().getDataSize());
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MemoryStream)) return false;
		
		MemoryStream that = (MemoryStream) o;
		return address == that.address && ld.equals(that.ld);
		
	}
	
	@Override
	public int hashCode() {
		int result = (int) (address ^ (address >>> 32));
		result = 31 * result + ld.hashCode();
		return result;
	}

	public void setChannel(FileChannel channel) {
		this.channel = channel;
	}
}
