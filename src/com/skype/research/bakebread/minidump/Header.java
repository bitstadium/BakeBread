/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.io.Marshaled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

/**
 * A POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms680378(v=vs.85).aspx
 */
public class Header implements Marshaled {
	
	public static final byte[] SIGNATURE = "MDMP".getBytes(Charset.forName("ASCII"));
	public static final int VERSION = 0x0000a793;
	public static final long REDMOND_PACIFIC = 3600 * 8;
	
	private byte[] signature = new byte[4];
	private int version;
	private int streamCount;
	private RVA streamDirectoryRva = new RVA(); // WISDOM RVA is 32 bit, 64-bit RVAs are RVA64
	private int checksum;
	private int msTimestamp; // unsigned int32? Unix epoch?
	private long flags;
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		dataInput.readFully(signature);
		if (!Arrays.equals(SIGNATURE, signature)) {
			throw new MalformedMiniDumpException("signature");
		}
		version = dataInput.readInt();
		if (version != VERSION) {
			throw new MalformedMiniDumpException("version");
		}
		streamCount = dataInput.readInt();
		streamDirectoryRva.readExternal(dataInput, fileChannel);
		checksum = dataInput.readInt();
		msTimestamp = dataInput.readInt();
		flags = dataInput.readLong();
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.write(signature);
		dataOutput.writeInt(version);
		dataOutput.writeInt(streamCount);
		streamDirectoryRva.writeExternal(dataOutput, fileChannel);
		dataOutput.writeInt(checksum);
		dataOutput.writeInt(msTimestamp);
		dataOutput.writeLong(flags);
	}
	
	public int getVersion() {
		return version;
	}
	
	public int getStreamCount() {
		return streamCount;
	}
	
	public RVA getStreamDirectoryRva() {
		return streamDirectoryRva;
	}
	
	public int getChecksum() {
		return checksum;
	}
	
	public long getTimestamp() {
		// reasonable so far, need to check the offset
		return (0xffffffffL & msTimestamp) - REDMOND_PACIFIC;
	}
	
	public Date getDateTime() {
		return new Date(getTimestamp() * 1000);
	}
	
	public long getFlags() {
		return flags;
	}
}
