/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemLoad;

/**
 * Registers a single MemLoad at a known Credibility level
 */
public interface LoadRegistrar {
	void register(MemLoad stream, Credibility credibility);
}
