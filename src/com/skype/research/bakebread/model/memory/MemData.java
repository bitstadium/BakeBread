/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.memory;

import com.skype.research.bakebread.nio.Memory;

/**
 * A memory area containing values immediately known.
 */
public interface MemData extends MemArea {
	Memory getData();
	// TODO extract the trimTo idiomatic expression
	MemData trimTo(MemArea memArea);
}
