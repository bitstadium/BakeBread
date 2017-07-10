/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.CharBuffer;

/**
 * Bleach reduced (halfword) ARM code.
 */
public class ThumbBleach extends CodeBleach<CharBuffer> {

	public ThumbBleach() {
		super(BufferAdapter.Stateless.CHAR_BAD, 0xf000, 0x7ff, 2);
	}

	@Override
	protected long bleachTail(long read, int wordNumber) {
		final long tail = read & ~mask;
		return tail == 0xf800 ? tail : read;
	}
}
