/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * A simple implementation of {@link FogConfig} in the form of an {@link EnumMap}.
 * Pattern length validation if performed at read time.
 */
public class ValConfigSet implements ValConfig {
	private final EnumSet<BitExactValidation> bitExactCfg;

	public ValConfigSet(EnumSet<BitExactValidation> bitExactCfg) {
		this.bitExactCfg = bitExactCfg;
	}

	@Override
	public boolean isValidationTypeEnabled(BitExactValidation val) {
		return bitExactCfg.contains(val);
	}
}
