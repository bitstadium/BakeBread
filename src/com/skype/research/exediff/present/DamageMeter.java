/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.config.Thresholds;
import com.skype.research.exediff.match.Hamming;
import com.skype.research.exediff.match.SeamDiff;
import com.skype.research.exediff.model.MemSeam;
import com.skype.util.quality.Aggregator;

import java.util.SortedMap;

/**
 * Evaluates quality of matches in ranked grades.
 */
public class DamageMeter extends Aggregator<Quality> {
	private final Thresholds thresholds;

	public DamageMeter(Thresholds thresholds) {
		super(Quality.EXACT_SAME);
		this.thresholds = thresholds;
	}

	public Quality assess(SeamDiff diff, HammingStat stats) {
		if (diff.getOverall().isEmpty()) {
			consider(Quality.MATCH_FAIL);
		} else {
			if (diff.getOutlierCount() > diff.getStitchCount() * thresholds.getMaxOutlierRatio()) {
				consider(Quality.MATCH_FAIL);
			}
			long totalOrdered = 0;
			final SortedMap<MemArea, MemSeam> ordered = diff.getOrdered();
			long driftStart = ordered.get(ordered.firstKey()).getTranslation();
			long driftAfter = driftStart;
			if (driftStart != 0) {
				consider(Quality.DRIFT_SAME);
			}
			for (MemSeam modSeam : ordered.values()) {
				totalOrdered += Areas.length(modSeam);
				driftAfter = modSeam.getTranslation();
				if (driftAfter != driftStart) {
					consider(driftStart == 0 ? Quality.PLACE_VARY : Quality.DRIFT_VARY);
				}
			}
			if (driftAfter != driftStart) {
				consider(Quality.DRIFT_AWAY);
			}
			final int commonLength = stats.control.getCommon();
			final int uniqueLength = stats.control.getUnique();
			final int longerLength = commonLength + uniqueLength;
			if (totalOrdered < longerLength) {
				if (totalOrdered < longerLength * thresholds.getMinOrderedRatio()) {
					consider(Quality.MATCH_FAIL);
				} else {
					consider(Quality.PLACE_VARY);
				}
			}
			if (uniqueLength != 0) {
				consider(Quality.DRIFT_VARY);
			}
			Hamming hamming = stats.overall;
			final int octets = hamming.getOctets();
			if (octets > 0) {
				consider(Quality.PLACE_SAME);
				final int common = hamming.getCommon();
				final int coBits = common << 3;
				if (octets > common * thresholds.getMaxHammingRatio()) {
					consider(Quality.MATCH_FAIL);
				}
				if (hamming.getDigits() > coBits * thresholds.getBitHammingRatio()) {
					consider(Quality.MATCH_FAIL);
				}
			}
		}
		return get();
	}
}
