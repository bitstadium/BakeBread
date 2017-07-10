/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config;

/**
 * Configured by user.
 */
public class MutableThresholds implements Thresholds {
	private float maxOutlierRatio = 0.5f;
	private float minOrderedRatio = 0.4f;
	private float maxHammingRatio = 0.9f;
	private float bitHammingRatio = 0.3f;

	@Override
	public float getMaxOutlierRatio() {
		return maxOutlierRatio;
	}

	@Override
	public float getMinOrderedRatio() {
		return minOrderedRatio;
	}

	@Override
	public float getMaxHammingRatio() {
		return maxHammingRatio;
	}

	@Override
	public float getBitHammingRatio() {
		return bitHammingRatio;
	}

	public void setMaxOutlierRatio(float maxOutlierRatio) {
		this.maxOutlierRatio = maxOutlierRatio;
	}

	public void setMinOrderedRatio(float minOrderedRatio) {
		this.minOrderedRatio = minOrderedRatio;
	}

	public void setMaxHammingRatio(float maxHammingRatio) {
		this.maxHammingRatio = maxHammingRatio;
	}
	
	public void setBitHammingRatio(float bitHammingRatio) {
		this.bitHammingRatio = bitHammingRatio;
	}
}
