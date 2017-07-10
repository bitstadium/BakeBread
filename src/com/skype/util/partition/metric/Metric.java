/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.metric;

/**
 * A discrete distance metric.
 */
public interface Metric {
	int distance(long l, long r);
}
