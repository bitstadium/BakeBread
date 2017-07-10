/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.PtrSize;
import com.skype.research.bakebread.io.FromToUtils;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

/**
 * Auxiliary ("genesis age") vector. 
 */
public final class AuxVecNote<G extends Buffer> extends TypedNote {
	public AuxVecNote(PtrSize ptrSize) {
		super(ptrSize, Type.NT_AUXV);
	}
	
	/*  // NT_AUXV // http://articles.manugarg.com/aboutelfauxiliaryvectors (32 bit!!!)
		https://github.com/torvalds/linux/blob/v3.19/include/uapi/linux/auxvec.h
		https://github.com/torvalds/linux/blob/v3.19/arch/ia64/include/uapi/asm/auxvec.h
	 */

	private G auxV; // FIXME pointer size; will be a LongBuffer on ARM64
	
	public void setDesc(G auxV) {
		this.auxV = auxV;
		setDescLen(auxV.capacity() * ptrSize.pointerSize);
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		if (ptrSize == PtrSize.INT) {
			FromToUtils.marshal(dataOutput, (IntBuffer) auxV); // fixme fixme
		} else if (ptrSize == PtrSize.LONG) {
			FromToUtils.marshal(dataOutput, (LongBuffer) auxV); // fixme fixme
		}
	}
}
