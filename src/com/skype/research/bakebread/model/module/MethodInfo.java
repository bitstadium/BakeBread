/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.module;

import com.skype.research.bakebread.model.memory.MemArea;

/**
 * Function/method information.
 */
public interface MethodInfo extends MemArea {
	CharSequence name(boolean humanReadable);
	MemArea asFileArea(); // MOREINFO asFileData?
}
