/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.NoSuchElementException;

/**
 * A simple implementation of {@link FogConfig} in the form of an {@link EnumMap}.
 * Pattern length validation if performed at read time.
 */
public class FogConfigMap extends EnumMap<MemoryFog, byte[]> implements FogConfig {
	public FogConfigMap() {
		super(MemoryFog.class);
	}

	@Override
	public boolean hasMemoryFillingPattern(MemoryFog fogType) {
		return containsKey(fogType);
	}

	@Override
	public byte[] getMemoryFillingPattern(MemoryFog fogType) {
		byte[] pattern = get(fogType);
		if (pattern == null) {
			throw new NoSuchElementException(fogType.name());
		}
		switch (pattern.length) {
			case 1:
			case 2:
			case 4:
				return pattern;
		}
		throw new IllegalArgumentException(Arrays.toString(pattern));
	}
}
