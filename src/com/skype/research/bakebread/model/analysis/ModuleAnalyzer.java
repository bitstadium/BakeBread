/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.host.FileLoad;
import com.skype.research.bakebread.model.memory.MapInfo;

/**
 * Alter, suppress or defer processing of a proposed {@link FileLoad} within the provided session context.
 */
public interface ModuleAnalyzer {
	void start(MemHeap<MapInfo> memMap);
	boolean analyze(FileLoad fileLoad, LoadRegistrar registrar);
	void flush(LoadRegistrar registrar);
}
