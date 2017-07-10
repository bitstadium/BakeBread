/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.memory;

import java.io.File;

/**
 * Represents a memory mapped area. Page-aligned, optionally mapped and mapping-aware. Converts to LOAD.
 */
public interface MemLoad extends MemData {
	File getFile(); // resolved to a local file or null
	boolean isDumpData(); // dumped remote mem
	boolean isHostData(); // mapped local file
	boolean isReliable(); // either of the above
	MapInfo getMapInfo();

	@Override
	MemLoad trimTo(MemArea memArea);
}
