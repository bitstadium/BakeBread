/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.model;

import com.skype.research.bakebread.model.memory.MemArea;

/**
 * A correlated pair of ranges, not necessarily of the same size, but of the same type.
 */
public interface MemPair<M extends MemArea & MemPair<M>> extends MemArea {
	M inverse();
}
