/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.config.cmdline;

import com.skype.research.sparse.scanner.ScannerType;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.EnumListOptions;
import com.skype.util.cmdline.RecognitionException;

/**
 * Defines the scanner type. Single choice.
 */
public class ScannerOptions extends EnumListOptions<ScannerType> {
	public ScannerOptions() {
		super('F', "fs", ScannerType.class);
	}

	@Override
	public ScannerType recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case '4':
				return ScannerType.EXT4;
			default:
				return null;
		}
	}

	@Override
	public ScannerType recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "ext4":
				return ScannerType.EXT4;
			default:
				return null;
		}
	}

	@Override
	protected void onOptionSet(ScannerType key) throws ConfigurationException {
		enforceMutualExclusion(key);
	}
}
