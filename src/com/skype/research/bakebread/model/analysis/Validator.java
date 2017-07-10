/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemLoad;

/**
 * Validate a {@link MemLoad} against its expected contents.
 */
public interface Validator {
	void compare(MemLoad modified, MemLoad original);
}
