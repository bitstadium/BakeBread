/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import com.skype.research.bakebread.model.analysis.Credibility;
import com.skype.research.bakebread.model.memory.MemLoad;

/**
 * Chunk types to write to the eventual dump. Richer than
 * {@link Credibility} to allow finer control.
 */
public enum CoreDumpLoadType implements OutConfig {
	DUMP {
		@Override
		public boolean shallWrite(MemLoad memLoad) {
			return memLoad.isDumpData();
		}
	},
	SURE {
		@Override
		public boolean shallWrite(MemLoad memLoad) {
			return memLoad.isReliable();
		}
	},
	COPY {
		@Override
		public boolean shallWrite(MemLoad memLoad) {
			return memLoad.isDumpData() || memLoad.isHostData();
		}
	},
	FILL {
		@Override
		public boolean shallWrite(MemLoad memLoad) {
			return true;
		}
	};
}
