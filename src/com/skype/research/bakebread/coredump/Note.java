/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.io.PackedString;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * ELF Note record.
 */
public abstract class Note implements Marshaled {
	
	protected final PtrSize ptrSize;

	private int nameLen;
	private int descLen;
	private int type;
	private CharSequence name;

	public Note(PtrSize ptrSize) {
		this.ptrSize = ptrSize;
	}

	public final void setTypeAndName(Type type) {
		this.type = type.code;
		this.name = new PackedString(type.name);
		this.nameLen = name.length();
	}

	public int getDescLen() {
		return descLen;
	}

	protected void setDescLen(int descLen) {
		this.descLen = descLen;
	}

	// https://sourceware.org/git/gitweb.cgi?p=binutils-gdb.git;a=blob;f=include/elf/common.h
	
	protected enum Type {
		// 43 4f 52 45 = 'CORE'
		NT_PRSTATUS("CORE", 1), // <-- 0 // actual dump [1] [4]... "CORE" length 0x94
		NT_PRFPREG(2),
		NT_PRPSINFO("CORE", 3), // <-- actual dump [0] (the one before the loop) <*** "CORE"
		NT_TASKSTRUCT(4),
		NT_AUXV("CORE", 6),     // // actual dump  [-2] (second last)   <*** "CORE"
		NT_PSTATUS(10),		/* Has a struct pstatus */
		NT_FPREGS(12),		/* Has a struct fpregset */
		NT_PSINFO(13),		/* Has a struct psinfo */
		NT_LWPSTATUS(16),		/* Has a struct lwpstatus_t */
		NT_LWPSINFO(17),		/* Has a struct lwpsinfo_t */
		// NT_WIN32PSTATUS(18),		/* Has a struct win32_pstatus */
		NT_ARM_VFP("LINUX", 0x400),  // <-- 1 // actual dump [2] "LINUX" length 0x104
		NT_ARM_TLS(0x401),
		NT_ARM_HW_BREAK(0x402),
		NT_ARM_HW_WATCH(0x403),
		NT_PRXFPREG(0X46E62B7F), // AKA NT_FPREGSET
		NT_SIGINFO("CORE", 0x53494749), // <-- 2 "SIGI" // actual dump [3] "CORE" length 0x80
		NT_FILE("CORE", 0X46494C45),    // <-- 3 "FILE" // actual dump [-1] (last) "CORE"
		;
		
		final String name;
		final int code;

		Type(int code) {
			this(null, code);
		}

		Type(String name, int code) {
			this.name = name;
			this.code = code;
		}
	}
	
	// Alignment and padding: http://lkml.iu.edu/hypermail/linux/kernel/0611.0/0521.html
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		nameLen = dataInput.readInt();
		descLen = dataInput.readInt();
		type = dataInput.readInt();
		name = ptrSize.readPacked(dataInput, nameLen);
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt(nameLen);
		dataOutput.writeInt(descLen);
		dataOutput.writeInt(type);
		ptrSize.writePadded(dataOutput, name);
	}

	public long getFileSize() {
		return PtrSize.INT.pointerSize * 3 + ptrSize.roundUp(nameLen) + ptrSize.roundUp(descLen);
	}
}
