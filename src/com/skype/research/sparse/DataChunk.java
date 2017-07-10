/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.sparse.scanner.Scanner;
import com.skype.research.sparse.trimmer.NeedTrimmingException;
import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Copy data as they are.
 */
public class DataChunk extends Chunk {
	private Scanner scanner;

	public DataChunk(Scanner scanner) {
		this.scanner = scanner;
	}
	
	@Override
	public void doTransfer(DataInput input, FileChannel in, TrimChannel out) throws IOException, NeedTrimmingException {
		if (scanner != null) {
			scanner.scan(this, input, in, out);
		}
		final long offset = in.position();
		final long length = getTargetBytes();
		out.transferFrom(in, offset, length);
		in.position(offset + length);
	}
}
