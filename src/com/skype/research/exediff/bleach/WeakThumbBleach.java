/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

/**
 * A shier Thumb bleach only accepting reasonably near jumps, to reduce false positives.
 */
public class WeakThumbBleach extends ThumbBleach {
	@Override
	protected boolean shouldStart(long read, long head) {
		head = read & 0xff00;
		return read == 0xf000 || read == 0xf700;
	}
}
