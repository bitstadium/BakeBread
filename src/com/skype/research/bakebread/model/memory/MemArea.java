/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.memory;

/**
 * Represents a virtual memory area.
 */
public interface MemArea {
	long getStartAddress();
	long getEndAddress();
}
