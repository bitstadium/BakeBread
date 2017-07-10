/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.io.FromToUtils;
import com.skype.research.bakebread.io.Marshaled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

/**
 * A POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms680383(v=vs.85).aspx
 */
public class LocationDescription implements Marshaled {
	private int dataSize;
	private final RVA rva = new RVA();
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		dataSize = dataInput.readInt();
		rva.readExternal(dataInput, fileChannel);
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt(dataSize);
		rva.writeExternal(dataOutput, fileChannel);
	}
	
	public int getDataSize() {
		return dataSize;
	}
	
	public RVA getRVA() {
		return rva;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LocationDescription)) return false;
		
		LocationDescription that = (LocationDescription) o;
		return dataSize == that.dataSize && rva.equals(that.rva);
		
	}
	
	@Override
	public int hashCode() {
		int result = dataSize;
		result = 31 * result + rva.hashCode();
		return result;
	}
	
	@Override
	public String toString() {
		return rva + ":" + PrettyPrint.hexWordSlim(rva.getVirtualAddress() + dataSize);
	}
	
	public byte[] asByteArray(DataInput dataInput, FileChannel channel) throws IOException {
		getRVA().navigate(channel);
		byte[] section = new byte[getDataSize()];
		dataInput.readFully(section);
		return section;
	}
	
	public Reader asAsciiReader(DataInput dataInput, FileChannel channel) throws IOException {
		return FromToUtils.asAsciiReader(asByteArray(dataInput, channel));
	}

	public ByteBuffer mapOriginalChannel(FileChannel channel) throws IOException {
		return channel.map(FileChannel.MapMode.READ_ONLY, rva.getVirtualAddress(), dataSize);
	}
	
	public void copyOut(File source, File target) throws IOException {
		try (FileChannel readChannel = FileChannel.open(source.toPath(), StandardOpenOption.READ)) {
			//noinspection ResultOfMethodCallIgnored
			target.delete();
			try (FileChannel dumpChannel = FileChannel.open(target.toPath(), StandardOpenOption.WRITE,
					StandardOpenOption.CREATE_NEW)) {
				readChannel.transferTo(rva.getVirtualAddress(), dataSize, dumpChannel);
			}
		}
	}

	public void readMarshaled(Marshaled marshaled, DataInput dataInput, FileChannel fileChannel) throws IOException {
		long position = fileChannel.position();
		try {
			getRVA().navigate(fileChannel);
			marshaled.readExternal(dataInput, fileChannel);
			assertConsumed(fileChannel);
		} finally {
			fileChannel.position(position);
		}
	}

	public void assertConsumed(FileChannel fileChannel) throws IOException {
		assert fileChannel.position() == getRVA().getVirtualAddress() + getDataSize();
	}
}
