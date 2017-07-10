/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.config.cmdline;

import com.skype.research.bakebread.config.cmdline.UnknownOptions;
import com.skype.research.exediff.config.InFilesOptions;
import com.skype.research.sparse.config.SparseConfig;
import com.skype.research.sparse.scanner.ScannerConf;
import com.skype.research.sparse.scanner.ScannerType;
import com.skype.util.cmdline.ArgParser;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.RecognitionException;

import java.io.File;
import java.util.Collection;

/**
 * Sparse image expand configuration.
 */
public class CmdLineSparseConfig implements SparseConfig {
	
	private final ScannerOptions scanner = new ScannerOptions();
	private final ScanCfgOptions scanCfg = new ScanCfgOptions();
	private final InFilesOptions inFiles = new InFilesOptions(true);

	private static final UnknownOptions unknown = new UnknownOptions();
	
	public CmdLineSparseConfig(String[] cmdLine) throws ConfigurationException, RecognitionException {
		ArgParser.parseCommandLine(cmdLine,
				scanner,
				scanCfg,
				inFiles,
				unknown
		);
	}

	@Override
	public Collection<File> getIns() {
		return inFiles.getFiles(0, -1);
	}

	@Override
	public File getOut() {
		return inFiles.getFile(-1);
	}

	@Override
	public ScannerType getScannerType() {
		return scanner.getOptions().isEmpty() ? ScannerType.EXT4 : scanner.getOptions().iterator().next();
	}
	
	@Override
	public ScannerConf getScannerConf() {
		return scanCfg.getConf();
	}

	@Override
	public boolean hasAnythingToDo() {
		return inFiles.getFileCount() > 1;
	}
}
