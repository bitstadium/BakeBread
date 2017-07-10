/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.nio;

import com.skype.research.bakebread.model.memory.MemArea;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

/**
 * Represents raw memory contents without metadata.
 */
public interface Memory {
	int BLOCK_SIZE = 256; // implementation detail

	long writeTo(OutputStream outputStream) throws IOException;
	long writeTo(WritableByteChannel channel) throws IOException;
	Memory transform(MemArea from, MemArea to);
}
