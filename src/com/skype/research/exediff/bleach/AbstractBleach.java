/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Abstract adjustable bleach.
 */
public abstract class AbstractBleach<B extends Buffer> implements Bleach {

	private final BufferAdapter<B> adapter;

	public AbstractBleach(BufferAdapter<B> adapter) {
		this.adapter = adapter;
		reset();
	}

	@Override
	public void bleach(ByteBuffer source) {
		B wordBuffer = adapter.asWordBuffer(source);
		while (wordBuffer.hasRemaining()) {
			bleachWord(wordBuffer, adapter.feed(wordBuffer));
		}
	}

	protected abstract void bleachWord(B wordBuffer, long read);

	protected void writeBack(B wordBuffer, long word) {
		adapter.write(wordBuffer, wordBuffer.position() - 1, word);
	}
}
