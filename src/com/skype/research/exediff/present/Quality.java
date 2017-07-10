/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.util.quality.Considerable;

/**
 * Human-understandable range match quality.
 */
public enum Quality implements Considerable<Quality> {
	EXACT_SAME("Contents are identical."),
	PLACE_SAME("In-place content changes but no skew detected."),
	PLACE_VARY("Zero skew near start and end, but varies in between."),
	DRIFT_SAME("Contents are skewed along at a constant skew."),
	DRIFT_VARY("Same skew near start and end, but varies in between."),
	DRIFT_AWAY("Insertions or deletions cause skew to accumulate."),
	MATCH_FAIL("Cannot correlate contents at all."),
	;

	private final String displayName;

	Quality(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return String.format("%d (%s)", ordinal(), displayName);
	}
	
	@Override
	public Quality consider(Quality consideration) {
		return ordinal() < consideration.ordinal() ? consideration : this;
	}

	public boolean isGood() {
		return this != MATCH_FAIL;
	}
}
