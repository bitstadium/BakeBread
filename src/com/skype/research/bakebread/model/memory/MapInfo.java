/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.memory;

/**
 * Represents a single memory mapping (one line in the memory map).
 */
public interface MapInfo extends MemArea, MemPerm {
	long getFileOffset();
	short[] getPartition();
	int getFd();
	String getName(); // may be a file name
}
