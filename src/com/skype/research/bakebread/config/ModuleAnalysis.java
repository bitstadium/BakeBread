/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

/**
 * Allow module layout analysis to reconstruct memory mapping, or assume contiguous mapping. 
 */
public enum ModuleAnalysis {
	RAW,    // most dumb: use the ModuleStreamList information
	ELF,    // executable and linkable format sections
	ARM,    // "bleach" BL offsets from code before diffing
	;
}
