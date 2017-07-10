/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

/**
 * Transformation of the original dump file.
 * Choose none to display properties only without extracting anything to disk.
 */
public enum Conversion {
	SPLIT_DIR,
	CORE_FILE,
}
