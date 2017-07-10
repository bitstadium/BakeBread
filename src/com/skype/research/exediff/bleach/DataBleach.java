/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.Buffer;

/**
 * Adds repetition count to repeating words.
 */
public class DataBleach<B extends Buffer> extends AbstractBleach<B> {
	private boolean ever;
	private int index;
	private long prev;

	public DataBleach(BufferAdapter<B> adapter) {
		super(adapter);
	}

	@Override
	public void reset() {
		ever = false;
	}

	@Override
	protected void bleachWord(B wordBuffer, long read) {
		if (wordBuffer.position() > 0) {
			if (read == prev) {
				if (ever && shouldBleach(read)) {
					writeBack(wordBuffer, ++index + read);
				}
			} else {
				ever = true;
				index = 0;
			}
		}
		prev = read;
	}

	protected boolean shouldBleach(long read) {
		return true;
	}
}
