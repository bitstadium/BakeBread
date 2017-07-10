/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config.cmdline;

import com.skype.research.exediff.config.MutableThresholds;
import com.skype.research.exediff.config.Threshold;
import com.skype.research.exediff.config.Thresholds;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.OpenEnumOptions;
import com.skype.util.cmdline.RecognitionException;

/**
 * Soft differencing options.
 */
public class ExeDiffOptions extends OpenEnumOptions<Threshold> {
	private final MutableThresholds thresholds = new MutableThresholds();

	public ExeDiffOptions() {
		super('S', "soft", Threshold.class, null);
	}

	public Thresholds getThresholds() {
		return thresholds;
	}

	@Override
	public Threshold recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'o': return Threshold.MAX_OUTLIER;
			case 'g': return Threshold.MIN_GROWING;
			case 'h': return Threshold.MAX_HAMMING;
			case 'b': return Threshold.BIT_HAMMING;
			default:
				return null;
		}
	}

	@Override
	public Threshold recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "max-outlier": return Threshold.MAX_OUTLIER;
			case "min-growing": return Threshold.MIN_GROWING;
			case "max-hamming": return Threshold.MAX_HAMMING;
			case "bit-hamming": return Threshold.BIT_HAMMING;
			default:
				return null;
		}
	}

	@Override
	protected void onValueSet(Threshold key, String value) throws ConfigurationException {
		super.onValueSet(key, value);
		final int percent = value.indexOf('%');
		float multiplier = 1.f;
		if (percent >= 0) {
			value = value.substring(0, percent);
			multiplier = 0.01f;
		}
		float threshold = multiplier * Float.parseFloat(value); // throws NumberFormatException
		if (threshold < 0.f || threshold > 1.f) {
			throw new ConfigurationException("Must be between 0 and 1", key, value);
		}
		key.setValue(thresholds, threshold);
	}
}
