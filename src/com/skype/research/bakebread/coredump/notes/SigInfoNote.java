/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.PtrSize;
import com.skype.research.bakebread.model.AppInfo;
import com.skype.research.bakebread.model.SigInfo;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Exception information note. See ex.: http://www.mkssoftware.com/docs/man5/siginfo_t.5.asp
 */
public final class SigInfoNote extends TypedNote {
	public SigInfoNote(PtrSize ptrSize) {
		super(ptrSize, Type.NT_SIGINFO);
		setDescLen(4 + 4 + ptrSize.pointerSize + 4 + 4 + 4 + ptrSize.pointerSize + 4 + 4);
	}
	
	int sigNo;
	int sigCode;
	long sigData; // ptr
	int errNo;
	int pid;
	int uid;
	long sigAddr;
	int sigStatus;
	int sigBand;

	public void setSigInfo(SigInfo sigInfo) {
		sigNo = sigInfo.getSigNo();
		sigAddr = sigInfo.getSigAddr();
		sigData = sigInfo.getSigData();
	}
	
	public void setAppInfo(AppInfo<?> appInfo) {
		pid = AppInfo.Utils.getIntStats(appInfo, "PID");
		uid = AppInfo.Utils.getIntStats(appInfo, "UID");
	} 

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		dataOutput.writeInt(sigNo);
		dataOutput.writeInt(sigCode);
		ptrSize.writePtr(dataOutput, sigData);
		dataOutput.writeInt(errNo);
		dataOutput.writeInt(pid);
		dataOutput.writeInt(uid);
		ptrSize.writePtr(dataOutput, sigAddr);
		dataOutput.writeInt(sigStatus);
		dataOutput.writeInt(sigBand);
	}
}
