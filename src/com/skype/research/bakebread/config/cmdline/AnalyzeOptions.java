/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.minidump.ModuleStream;
import com.skype.util.cmdline.EnumListOptions;
import com.skype.util.cmdline.RecognitionException;

/**
 * Split or join module mappings inferred from {@link ModuleStream} entries
 */
public class AnalyzeOptions extends EnumListOptions<ModuleAnalysis> {

	public AnalyzeOptions() {
		super('M', "modules", ModuleAnalysis.class);
	}

	@Override
	public ModuleAnalysis recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'r': return ModuleAnalysis.RAW;
			case 'e': return ModuleAnalysis.ELF;
			case 'a': return ModuleAnalysis.ARM;
			default:
				return null;
		}
	}

	@Override
	public ModuleAnalysis recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "raw": return ModuleAnalysis.RAW;
			case "elf": return ModuleAnalysis.ELF;
			case "arm": return ModuleAnalysis.ARM;
			default:
				return null;
		}
	}
}
