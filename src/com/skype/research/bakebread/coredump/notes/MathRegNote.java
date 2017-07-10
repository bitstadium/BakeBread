/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.PtrSize;
import com.skype.research.bakebread.model.RegBank;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.channels.FileChannel;

/**
 * Extended (e.g. floating point) register note. 
 */
public final class MathRegNote<F extends Buffer> extends TypedNote {
	public MathRegNote(PtrSize ptrSize) {
		super(ptrSize, Type.NT_ARM_VFP);
	}
	
	private RegBank<F> regBank;

	public void setRegBank(RegBank<F> regBank) {
		this.regBank = regBank;
		setDescLen(0x104); // 0x8 * 0x20 + fpscr
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		for (int i = 0; i <regBank.getRegisterValues().capacity(); ++i) {
			dataOutput.writeLong(regBank.getRegisterValue(i));
		}
		dataOutput.writeInt((int) regBank.getSpecialValue(RegBank.Special.ProcessorState));
	}
}
