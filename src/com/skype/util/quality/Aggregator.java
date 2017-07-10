/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.quality;

/**
 * Aggregator of quality metrics.
 */
public class Aggregator<Q extends Considerable<Q>> {
	private Q q;

	public Aggregator(Q defaultQ) {
		this.q = defaultQ;
	}
	
	public Q consider(Q anotherQ) {
		return q = q.consider(anotherQ);
	}

	public Q get() {
		return q;
	}
}
