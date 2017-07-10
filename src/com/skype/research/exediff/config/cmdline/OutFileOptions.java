/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config.cmdline;

import com.skype.util.cmdline.Options;

import java.io.File;
import java.util.Iterator;

/**
 * Interpret the element as a file name.
 */
public class OutFileOptions implements Options {
	File file;

	@Override
	public boolean recognize(String option, Iterator<String> parameters) {
		if (this.file == null) {
			this.file = new File(option);
			return true;
		}
		return false;
	}

	public File getFile() {
		return file;
	}
}
