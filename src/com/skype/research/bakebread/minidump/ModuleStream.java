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
 * POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms680392(v=vs.85).aspx
 */
public class ModuleStream implements Marshaled {
	private long imageBase;
	private int  imageSize;
	private int  checkSum;
	private int  timeStamp;
	private final RVA nameRva = new RVA();
	private final VersionInfo versionInfo = new VersionInfo();
	private final LocationDescription cvRecordLd = new LocationDescription();
	private final LocationDescription miscRecordLd = new LocationDescription();
	private final long[] reserved = new long[2];
	
	private String moduleName;
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		imageBase = dataInput.readLong();
		imageSize = dataInput.readInt();
		checkSum  = dataInput.readInt();
		timeStamp = dataInput.readInt();
		nameRva.readExternal(dataInput, fileChannel);
		versionInfo.readExternal(dataInput, fileChannel);
		cvRecordLd.readExternal(dataInput, fileChannel);
		miscRecordLd.readExternal(dataInput, fileChannel);
		reserved[0] = dataInput.readLong();
		reserved[1] = dataInput.readLong();
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeLong(imageBase);
		dataOutput.writeInt(imageSize);
		dataOutput.writeInt(checkSum);
		dataOutput.writeInt(timeStamp);
		nameRva.writeExternal(dataOutput, fileChannel);
		versionInfo.writeExternal(dataOutput, fileChannel);
		cvRecordLd.writeExternal(dataOutput, fileChannel);
		miscRecordLd.writeExternal(dataOutput, fileChannel);
		dataOutput.writeLong(reserved[0]);
		dataOutput.writeLong(reserved[1]);
	}
	
	// TODO extract the "post-read" callback when LDs/RVAs need to be visited after sequential reading of the header
	public void readName(DataInput dataInput, FileChannel connection) throws IOException {
		nameRva.navigate(connection);
		// TODO extract as WideString
		final int length = dataInput.readInt();
		assert length % 2 == 0; // wchar_t
		final char[] chars = new char[length >> 1];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = dataInput.readChar();
		}
		moduleName = new String(chars);
	}
	
	public long getImageBase() {
		return imageBase;
	}
	
	public int getImageSize() {
		return imageSize;
	}
	
	public int getCheckSum() {
		return checkSum;
	}
	
	public int getTimeStamp() {
		return timeStamp;
	}
	
	public RVA getNameRva() {
		return nameRva;
	}
	
	public VersionInfo getVersionInfo() {
		return versionInfo;
	}
	
	public LocationDescription getCvRecordLd() {
		return cvRecordLd;
	}
	
	public LocationDescription getMiscRecordLd() {
		return miscRecordLd;
	}
	
	public String getModuleName() {
		return moduleName;
	}
	
	@Override
	public String toString() {
		return moduleName + ": " + 
				PrettyPrint.hexWordSlim(imageBase) + ":" + 
				PrettyPrint.hexWordSlim(imageBase + imageSize) + ", " +
				"[crc " + PrettyPrint.hexWord(checkSum) + ']';
	}
}
