/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.CoreDumpLoadType;
import com.skype.research.bakebread.config.OutConfig;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.EnumListOptions;
import com.skype.util.cmdline.RecognitionException;

import java.util.Iterator;

/**
 * Core dump richness configuration options.
 */
public class CoreOutOptions extends EnumListOptions<CoreDumpLoadType> implements OutConfig {
	public CoreOutOptions() {
		super('O', "out", CoreDumpLoadType.class);
	}

	@Override
	public CoreDumpLoadType recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'm': return CoreDumpLoadType.DUMP;
			case 'r': return CoreDumpLoadType.SURE;
			case 'w': return CoreDumpLoadType.COPY;
			case 'a': return CoreDumpLoadType.FILL;
			default:
				return null;
		}
	}

	@Override
	public CoreDumpLoadType recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "mdp":    
				return CoreDumpLoadType.DUMP;
			case "ro":
				return CoreDumpLoadType.SURE;
			case "rw":
				return CoreDumpLoadType.COPY;
			case "all":
				return CoreDumpLoadType.FILL;
			default:
				return null;
		}
	}

	@Override
	protected void onOptionSet(CoreDumpLoadType key) throws ConfigurationException {
		enforceMutualExclusion(key);
	}

	@Override
	public boolean shallWrite(MemLoad memLoad) {
		Iterator<CoreDumpLoadType> itr = enumSet.iterator();
		CoreDumpLoadType policy = itr.hasNext() ? itr.next() : CoreDumpLoadType.SURE;
		return policy.shallWrite(memLoad);
	}
}
