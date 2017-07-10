/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.config;

import com.skype.research.sparse.scanner.ScannerConf;
import com.skype.research.sparse.scanner.ScannerType;

import java.io.File;
import java.util.Collection;

/**
 * Configures heuristic scanning options.
 */
public interface SparseConfig {
	Collection<File> getIns();
	File getOut();
	ScannerType getScannerType();
	ScannerConf getScannerConf();
	boolean hasAnythingToDo();
}
