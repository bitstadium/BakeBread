/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.match;

import com.skype.util.partition.MetricTree;
import com.skype.util.partition.rolling.RollingHash;

/**
 * A pre-indexed comparison base ("original").
 */
public interface Base {
	RollingHash getHash();
	MetricTree getTree();
}
