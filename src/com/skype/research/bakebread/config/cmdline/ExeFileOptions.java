/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.util.cmdline.ValueOptions;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reference binary paths, 
 *  such as target system root and application native library folders.
 * Both mappable contents and symbols are used.
 * 
 *  -P <PATH>, --path=PATH     Binary and symbol file path.
 */
public class ExeFileOptions extends ValueOptions {
	public ExeFileOptions() {
		super('P', "path");
	}

	private final Set<File> folderSet = new LinkedHashSet<>();
	private final Set<String> undefinedElements = new LinkedHashSet<>();

	@Override
	protected boolean recognizeValue(String path) {
		String[] pathElements = path.split(File.pathSeparator);
		for (String pathElement : pathElements) {
			File folder = new File(pathElement);
			if (folder.isDirectory()) {
				folderSet.add(folder);
			} else {
				undefinedElements.add(pathElement);
			}
		}
		return true;
	}

	public Set<File> getFolderSet() {
		return folderSet;
	}

	public Set<String> getUndefinedElements() {
		return undefinedElements;
	}
}
