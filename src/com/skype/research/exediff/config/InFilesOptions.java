/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config;

import com.skype.util.cmdline.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Interpret the element as a file name.
 */
public class InFilesOptions implements Options {
	final List<File> files = new ArrayList<>();
	private final boolean mayNotExist;

	public InFilesOptions() {
		this(false);
	}

	public InFilesOptions(boolean mayNotExist) {
		this.mayNotExist = mayNotExist;
	}

	@Override
	public boolean recognize(String option, Iterator<String> parameters) {
		File file = new File(option);
		if (file.isFile() || mayNotExist) {
			files.add(file);
			return true;
		}
		return false;
	}

	public File getFile(int index) {
		if (index < 0) {
			index = files.size() + index;
		}
		return files.get(index);
	}
	
	public int getFileCount() {
		return files.size();
	}

	public Collection<File> getFiles() {
		return Collections.unmodifiableList(files);
	}
	
	public Collection<File> getFiles(int start, int after) {
		if (start < 0) {
			start = files.size() + start;
		}
		if (after < 0) {
			after = files.size() + after;
		}
		return Collections.unmodifiableList(files.subList(start, after));
	}
}
