/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Data do not matter.
 */
public class SkipChunk extends Chunk {
	@Override
	public void doTransfer(DataInput input, FileChannel in, TrimChannel out) throws IOException {
		out.position(out.position() + getTargetBytes());
	}
}
