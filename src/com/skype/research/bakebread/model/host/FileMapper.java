/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.host;

import com.skype.research.bakebread.model.analysis.LoadRegistrar;
import com.skype.research.bakebread.model.analysis.ModuleAnalyzer;
import com.skype.research.bakebread.model.memory.MapInfo;

/**
 * Retrieves mapped ranges according to a file and mapping information.
 * Reports failures as "memory fog".
 */
public interface FileMapper extends ModuleAnalyzer {
	void mapRegion(MapInfo mapInfo, FileFinder fileFinder, LoadRegistrar registrar);
}
