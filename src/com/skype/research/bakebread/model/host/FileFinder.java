/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.host;

import com.skype.research.bakebread.model.memory.MapInfo;

import java.io.File;

/**
 * File locator by its mapped name.
 */
public interface FileFinder {
	File find(MapInfo name);
}
