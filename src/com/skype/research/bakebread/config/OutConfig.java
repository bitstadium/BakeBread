/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import com.skype.research.bakebread.model.memory.MemLoad;

/**
 * Reliability and non-redundancy requirements for a {@link MemLoad} to be written.
 */
public interface OutConfig {
	boolean shallWrite(MemLoad memLoad);
}
