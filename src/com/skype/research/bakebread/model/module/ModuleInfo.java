/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.module;

import java.io.File;

/**
 * A high-level view of an executable module.
 */
public interface ModuleInfo {
	File getOriginalFile(); // host file
	long offsetToVirtual(long fileOffset);
	long virtualToOffset(long virtAddress);
	MethodInfo addr2Func(long fileOffset);
	MethodInfo name2Func(CharSequence name);
}
