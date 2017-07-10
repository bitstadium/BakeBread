/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.Note;
import com.skype.research.bakebread.coredump.PtrSize;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * A note read from an actual ELF file.
 */
public final class ReadableNote extends Note {
	public ReadableNote(PtrSize ptrSize) {
		super(ptrSize);
	}
	
	private byte[] desc;

	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		super.readExternal(dataInput, fileChannel);
		desc = ptrSize.readRawBuf(dataInput, getDescLen());
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		ptrSize.writePadded(dataOutput, desc);
	}
}
