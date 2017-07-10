/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

/**
 * "Memory fog" configuration.
 */
public interface FogConfig {
	boolean hasMemoryFillingPattern(MemoryFog fogType);
	byte[] getMemoryFillingPattern(MemoryFog fogType);
}
