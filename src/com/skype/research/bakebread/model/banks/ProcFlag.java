/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.banks;

/**
 * // (0 = executing, not superuser) WISDOM ptr-size + gap
 */ 
public enum ProcFlag {
	Elevated(4),
	JustForked(1),
	;
	
	public final int value;

	ProcFlag(int value) {
		this.value = value;
	}
}
