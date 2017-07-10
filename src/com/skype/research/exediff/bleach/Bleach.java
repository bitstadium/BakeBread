/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.ByteBuffer;

/**
 * Wipes out position awareness from CPU opcodes,
 * e.g. eliminating static linkage dependent bits.
 */
public interface Bleach {
	void bleach(ByteBuffer source);
	void reset();

	enum Dumb implements Bleach {
		NO_OP {
			@Override
			public void bleach(ByteBuffer source) {}
		},
		CLEAR {
			@Override
			public void bleach(ByteBuffer source) {
				ByteBuffer dupe = source.duplicate();
				while (dupe.hasRemaining()) {
					dupe.put((byte) 0);
				}
			}
		}
		;

		// reusable without limit, so stateless
		@Override
		public void reset() {}
	}
	
	/**
	 * Factories of common {@link Bleach} implementations.
	 */
	enum Type {
		NONE {
			@Override
			public Bleach create() {
				return Dumb.NO_OP;
			}
		},
		ARM_BL {
			@Override
			public Bleach create() {
				return new ArmBleach();
			}
		},
		THUMB_BL {
			@Override
			public Bleach create() {
				return new ThumbBleach();
			}
		},
		CHAR_REP {
			@Override
			public Bleach create() {
				return new DataBleach<>(BufferAdapter.Stateless.CHAR_BAD);
			}
		},
		WORD_REP {
			@Override
			public Bleach create() {
				return new DataBleach<>(BufferAdapter.Stateless.INT_BAD);
			}
		}
		;
		public abstract Bleach create();
	}
}
