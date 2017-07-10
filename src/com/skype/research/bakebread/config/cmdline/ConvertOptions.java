/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.Conversion;
import com.skype.util.cmdline.OpenEnumOptions;
import com.skype.util.cmdline.RecognitionException;

import java.io.File;

/**
 * Converter target config.
 *  -CS <DIR>, --convert-split=<DIR>   Split the dump into individual streams.
 *  -CC <FILE>, --convert-core=<FILE>  Produce a core dump file.
 */
public class ConvertOptions extends OpenEnumOptions<Conversion> {

	public ConvertOptions() {
		super('C', "convert", Conversion.class, null); // single value
	}

	public File getTargetFile(Conversion target) {
		return new File(getValue(target));
	}

	@Override
	public Conversion recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'C': return Conversion.CORE_FILE;
			case 'S': return Conversion.SPLIT_DIR;
			default:
				return null;
		}
	}

	@Override
	public Conversion recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "core": return Conversion.CORE_FILE;
			case "split": return Conversion.SPLIT_DIR;
			default:
				return null;
		}
	}
}
