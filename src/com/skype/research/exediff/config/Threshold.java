/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config;

/**
 * Threshold options.
 */
public enum Threshold {
	MAX_OUTLIER {
		@Override
		public void setValue(MutableThresholds thresholds, float value) {
			thresholds.setMaxOutlierRatio(value);
		}
	},
	MIN_GROWING {
		@Override
		public void setValue(MutableThresholds thresholds, float value) {
			thresholds.setMinOrderedRatio(value);
		}
	},
	MAX_HAMMING {
		@Override
		public void setValue(MutableThresholds thresholds, float value) {
			thresholds.setMaxHammingRatio(value);
		}
	},
	BIT_HAMMING {
		@Override
		public void setValue(MutableThresholds thresholds, float value) {
			thresholds.setBitHammingRatio(value);
		}
	},
	;

	public abstract void setValue(MutableThresholds thresholds, float value);
}
