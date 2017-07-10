/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config;

/**
 * Matching proximity/sensitivity tweaks.
 */
public interface Thresholds {
	float getMaxOutlierRatio();
	float getMinOrderedRatio();
	float getMaxHammingRatio();
	float getBitHammingRatio();
}
