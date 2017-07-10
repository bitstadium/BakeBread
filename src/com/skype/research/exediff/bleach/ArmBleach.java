/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.IntBuffer;

/**
 * Bleach full-word ARM code.
 */
public class ArmBleach extends CodeBleach<IntBuffer> {
	public ArmBleach() {
		super(BufferAdapter.Stateless.INT_BAD, 0xeb000000, 0xffffff, 1);
	}
}
