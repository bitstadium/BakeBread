/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.scanner;

import com.skype.research.sparse.DataChunk;
import com.skype.research.sparse.trimmer.NeedTrimmingException;
import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;

/**
 * Scans the output for something.
 */
public interface Scanner {
	void reset(); // clear state
	void setScannerCfg(ScannerConf conf);
	void setTrimmer(TrimChannel trimmer);
	long scan(DataChunk dataChunk, DataInput input, FileChannel in, SeekableByteChannel out) throws IOException, NeedTrimmingException;
	void report(PrintStream printWriter);
}
