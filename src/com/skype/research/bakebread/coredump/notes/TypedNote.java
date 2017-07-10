/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.Note;
import com.skype.research.bakebread.coredump.PtrSize;

import java.io.DataInput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Typed note. DataInput read operation not supported.
 */
public abstract class TypedNote extends Note {
	public TypedNote(PtrSize ptrSize, Type preset) {
		super(ptrSize);
		setTypeAndName(preset);
	}

	@Override
	public final void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		throw new UnsupportedOperationException();
	}
}
