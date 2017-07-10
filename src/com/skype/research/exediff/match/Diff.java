/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.match;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.model.MemSeam;

import java.util.SortedMap;

/**
 * Represents a computed edit sequence.
 */
public interface Diff {
	// read-only
	SortedMap<MemArea, MemSeam> getOrdered();

	// read-only
	SortedMap<MemArea, MemSeam> getOOOrder();

	// copy out
	SortedMap<MemArea, MemSeam> getOverall();
}
