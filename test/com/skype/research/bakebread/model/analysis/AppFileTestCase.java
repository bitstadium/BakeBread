/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.host.FileFinder;
import com.skype.research.bakebread.model.host.HostFileFinder;

import java.io.File;
import java.io.IOException;

/**
 * Create default folder and file structure.
 */
public abstract class AppFileTestCase extends FileTestCase {

	public static final int LIBC_SIZE = 4080;
	public static final int BASE_SIZE = 2040;

	File systemRoot;
	File sysLibRoot;
	File appLibRoot;

	FileFinder fileFinder;

	@Override
	protected void setUp(File tempFolderFile) throws IOException {
		// place files
		systemRoot = mkdirs(tempFolderFile, "root");
		sysLibRoot = mkdirs(systemRoot, "system", "lib");
		mkFile(sysLibRoot, "libc.so", LIBC_SIZE, new byte[] {'l', 'i', 'b', 'c'});
		appLibRoot = mkdirs(tempFolderFile, "data");
		mkFile(appLibRoot, "base.apk", BASE_SIZE, new byte[] {'.', 'a', 'p', 'k'});

		fileFinder = new HostFileFinder(systemRoot, appLibRoot);
	}
}
