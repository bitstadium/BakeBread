/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model;

/**
 * Signal info, i.e. signal id, data/addresses and user context.
 */
public interface SigInfo {

	// MOREINFO where is the errno of elf_siginfo? is it optional?	
	int getSigNo();
	long getSigAddr();
	long getSigData();
}
