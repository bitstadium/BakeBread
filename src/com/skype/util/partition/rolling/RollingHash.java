/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.rolling;

import java.nio.Buffer;
import java.nio.LongBuffer;

/**
 * A rolling-hash representation of a byte array.
 */
public interface RollingHash extends Rolling {
	/**
	 * Computed hash data. As a matter of convenience,,
	 * {@link Buffer#position()} of the returned buffer
	 * matches {@link #getWarmUpWindowSteps()}, i.e.
	 * will point to the first non-warm-up value.
	 * 
	 * The {@link Buffer#limit()} of the returned buffer
	 * will match its {@link Buffer#capacity()}, as rolling
	 * hashes don't typically need a cool-down period.
	 * 
	 * @return the rolling hash sequence for the byte array
	 * represented. Typically backed by a long[] array, but
	 * clients are not advised to make assumptions on that.
 	 */
	LongBuffer computed();
}
