/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import com.skype.research.exediff.config.Thresholds;

import java.io.File;
import java.util.Collection;

/**
 * BakeBread task configuration.
 */
public interface Configuration extends FogConfig, ValConfig, ManConfig, OutConfig, Thresholds {
	boolean hasNothingToDo();
	boolean isDisplaySectionEnabled(DisplaySection section);
	boolean shouldConvertTo(Conversion conversionType);
	File getConversionTarget(Conversion conversionType);
	Collection<File> getModulePaths();
	Collection<String> getUndefinedPathElements();
	File getDumpFile();
}
