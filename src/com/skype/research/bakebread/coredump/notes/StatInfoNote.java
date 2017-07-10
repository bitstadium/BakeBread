/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.PtrSize;
import com.skype.research.bakebread.model.AppInfo;
import com.skype.research.bakebread.model.RegBank;
import com.skype.research.bakebread.model.SigInfo;
import com.skype.research.bakebread.model.ThrInfo;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.channels.FileChannel;

/**
 * Thread signal info note. See git/breakpad/src/common/android/include/sys/procfs.h
 */
public final class StatInfoNote<G extends Buffer> extends TypedNote {

	public StatInfoNote(PtrSize ptrSize) {
		super(ptrSize, Type.NT_PRSTATUS);
	};
	
	private SigInfo sigInfo; // elf_siginfo, not siginfo_t !!!
	private short cursig; // Current signal
	private int sigpend;// Set of pending signals
	private int sighold;// Set of held signals

	private AppInfo appInfo;
	private int overrideTid;
	
	private long uTime;  // User time // struct timeval
	private long sTime;  // System time // struct timeval
	private long cUTime; // Cumulative user time // struct timeval
	private long cSTime; // Cumulative system time// struct timeval
	private RegBank<G> reg;    // General purpose registers [48]
	boolean fpValid;// True if math copro being used. // int-aligned

	public void setSigInfo(SigInfo sigInfo) {
		this.sigInfo = sigInfo;
		cursig = (short) sigInfo.getSigNo(); // correct?
	}
	
	public void setAppInfo(AppInfo psIds) {
		this.appInfo = psIds;
		sigpend = (int) AppInfo.Utils.getLongHexStats(psIds, "SigPnd");
		sighold = (int) AppInfo.Utils.getLongHexStats(psIds, "SigBlk");
	}

	public void setThrInfo(ThrInfo<?, ?> thrInfo) {
		this.overrideTid = thrInfo.getThreadId();
	}

	public void setRegBank(RegBank<G> regBank) {
		setDescLen(0x94); // FIXME yes I am evil
		reg = regBank;
	}

	public void setFpValid(boolean fpValid) {
		this.fpValid = fpValid;
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		dataOutput.writeInt(sigInfo.getSigNo());
		dataOutput.writeInt((int) sigInfo.getSigAddr());
		dataOutput.writeInt((int) sigInfo.getSigData());    // 12.
		dataOutput.writeShort(cursig);
		dataOutput.writeChar(0); // skip 2 bytes
		dataOutput.writeInt(sigpend);
		dataOutput.writeInt(sighold);                       // 24.
		int mainThreadId = Integer.parseInt(appInfo.getProcStat("PID"));
		if (overrideTid == mainThreadId) {
			int parentPId = Integer.parseInt(appInfo.getProcStat("PPid"));
			String pGrp = appInfo.getProcStat("PGrp");
			int parentGId = pGrp == null ? 0 : Integer.parseInt(pGrp); // TODO make all parsers soft
			if (parentGId == 0) parentGId = parentPId;
			dataOutput.writeInt(mainThreadId);
			dataOutput.writeInt(parentPId);
			dataOutput.writeInt(parentGId);
		} else {
			dataOutput.writeInt(overrideTid);
			dataOutput.writeInt(mainThreadId);
			dataOutput.writeInt(mainThreadId);
		}
		String sid = appInfo.getProcStat("SID");
		dataOutput.writeInt(sid == null ? 0 : Integer.parseInt(sid));
		// times...
		dataOutput.writeLong(uTime);
		dataOutput.writeLong(sTime);
		dataOutput.writeLong(cUTime);
		dataOutput.writeLong(cSTime);               // 72.
		// ...
		for (int i = 0; i < reg.getRegisterValues().capacity(); ++i) {
			ptrSize.writePtr(dataOutput, reg.getRegisterValue(i));
		}                                                   // 136,
		ptrSize.writePtr(dataOutput, reg.getSpecialValue(RegBank.Special.ProcessorState)); // 140.
		ptrSize.writePtr(dataOutput, 0); // some trash ARM register I don't remember, doesn't matter
		dataOutput.writeInt(fpValid ? 1 : 0);                   // 148.
	}
}
