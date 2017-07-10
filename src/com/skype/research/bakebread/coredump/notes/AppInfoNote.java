/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.ProcState;
import com.skype.research.bakebread.coredump.PtrSize;
import com.skype.research.bakebread.io.PackedString;
import com.skype.research.bakebread.model.AppInfo;
import com.skype.research.bakebread.model.banks.ProcFlag;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Starting note.
 */
public final class AppInfoNote extends TypedNote {
	public AppInfoNote(PtrSize ptrSize) {
		super(ptrSize, Type.NT_PRPSINFO);
		setDescLen(4 + ptrSize.pointerSize + 4 + 16 + 16 + 80);
	}

	// WISDOM AUTHORITATIVE git/breakpad/src/common/android/include/sys/procfs.h
	
	// @see also:
	// http://cs-pub.bu.edu/fac/richwest/cs591_w1/notes/linux_process_mgt.PDF [states]
	// http://www.computerhope.com/unix/ups.htm [states]

	private ProcState state;
	private byte nice;		// Nice val.// MOREINFO let's assume 0 (default niceness)
	// 4
	// 4 + ptrSize + 4
	private AppInfo appInfo;
	// .. + 16
	
	private final byte[] name = new byte[16];	// Filename of executable.
	private final PackedString psName = new PackedString(name);
	private final byte[] args = new byte[80]; // Initial part of arg list.
	private final PackedString psArgs = new PackedString(args);
	
	public void setState(ProcState state) {
		this.state = state;
	}

	public void setNice(byte nice) {
		this.nice = nice;
	}

	public void setAppInfo(AppInfo appInfo) {
		this.appInfo = appInfo;
		// optional, we already have a safe default
		setState(AppInfo.Utils.getProcState(appInfo, "State", ProcState.TRACING));
	}

	private static int summarize(AppInfo appInfo) {
		int flags = 0;
		for (ProcFlag flag : ProcFlag.values()) {
			if (appInfo.getFlag(flag)) {
				flags |= flag.value;
			}
		}
		return flags;
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		// ** enum State
		dataOutput.writeByte(state.sCode);
		dataOutput.writeByte(state.sName); // WISDOM sic
		dataOutput.writeByte(state.zombie ? 1 : 0);
		// eo enum State
		dataOutput.writeByte(nice);
		ptrSize.writePtr(dataOutput, summarize(appInfo)); // FIXME ad-hoc: 0x00400140
		dataOutput.writeShort(AppInfo.Utils.getIntStats(appInfo, "UID"));
		dataOutput.writeShort(AppInfo.Utils.getIntStats(appInfo, "GID"));
		dataOutput.writeInt(AppInfo.Utils.getIntStats(appInfo, "PID"));
		dataOutput.writeInt(AppInfo.Utils.getIntStats(appInfo, "PPid"));
		dataOutput.writeInt(AppInfo.Utils.getIntStats(appInfo, "PPid")); // PGRP
		dataOutput.writeInt(AppInfo.Utils.getIntStats(appInfo, "SID")); // SID mb null
		dataOutput.write(name);
		dataOutput.write(args);
	}

	public void setCommandLine(CharSequence appName, CharSequence cmdArgs) {
		pack(appName, this.name);
		pack(cmdArgs, this.args);
	}

	public void pack(CharSequence src, byte[] trg) {
		int i;
		for (i = 0; i < src.length() && i < trg.length; ++i) {
			char c = src.charAt(i);
			trg[i] = (byte) c;
			if (c == 0) {
				break;
			}
		}
		while (i < trg.length) {
			trg[i++] = 0;
		}
	}
}
