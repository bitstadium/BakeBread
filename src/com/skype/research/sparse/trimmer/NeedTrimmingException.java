/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.trimmer;

/**
 * Exception-driven control flow, an anti-pattern (yes I know).
 */
public class NeedTrimmingException extends Exception {
	private final long cuttingPoint;

	public NeedTrimmingException(long cuttingPoint) {
		this.cuttingPoint = cuttingPoint;
	}

	public long getCuttingPoint() {
		return cuttingPoint;
	}
}
