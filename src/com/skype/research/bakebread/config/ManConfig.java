/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import java.util.Collection;

/**
 * Module analysis types enabled (e.g. assumptions over the nature of binary modules).
 */
public interface ManConfig {
	boolean isModuleAnalysisEnabled(ModuleAnalysis man);
	Collection<ModuleAnalysis> getModuleAnalysisTypes();
}
