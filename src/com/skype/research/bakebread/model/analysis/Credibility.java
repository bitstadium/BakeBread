/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import java.util.EnumMap;

/**
 * Represents the origin of memory data.
 */
public enum Credibility {
	Dump, // obtained from the remote process
	Host, // decoded from host
	Link, // decoded from host, module analysis (e.g. ELF linkage) applied
	Desc, // described in mapping but undefined
	;

	public static <V> V mostCredibleOrNull(EnumMap<Credibility, V> slots) {
		for (Credibility r : values()) {
			V candidate = slots.get(r);
			if (candidate != null) {
				return candidate;
			}
		}
		return null;
	}
}
