/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis.mock;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.nio.Memory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * An unreadable memory area.
 */
public class MockMemory implements Memory {
	@Override
	public long writeTo(OutputStream outputStream) throws IOException {
		throw new UnsupportedOperationException("sorry, just kiddin'");
	}

	@Override
	public long writeTo(WritableByteChannel channel) throws IOException {
		throw new UnsupportedOperationException("sorry, just kiddin'");
	}

	@Override
	public Memory transform(MemArea from, MemArea to) {
		return this;
	}
	//
}
