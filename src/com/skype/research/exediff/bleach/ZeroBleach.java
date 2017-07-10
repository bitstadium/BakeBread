/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.Buffer;

/**
 * Only "bleach" repeating zeroes.
 */
public class ZeroBleach<B extends Buffer> extends DataBleach<B> {
	public ZeroBleach(BufferAdapter<B> adapter) {
		super(adapter);
	}

	@Override
	protected boolean shouldBleach(long read) {
		return read == 0;
	}
}
