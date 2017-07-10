/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.util.cmdline.Options;

import java.io.File;
import java.util.Iterator;

/**
 * Interpret the element as a file name.
 */
public class DmpFileOptions implements Options {
	File file;

	@Override
	public boolean recognize(String option, Iterator<String> parameters) {
		File file = new File(option);
		if (file.isFile()) {
			this.file = file;
			return true;
		}
		return false;
	}

	public File getFile() {
		return file;
	}
}
