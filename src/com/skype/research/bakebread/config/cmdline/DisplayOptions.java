/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.DisplaySection;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.EnumListOptions;
import com.skype.util.cmdline.RecognitionException;

import java.util.EnumSet;

/**
 * Various information sections to show to standard output.
 * 
 *  -Ds, --display=stats       Show file statistic (size, stream count, etc.)
 *  -Dr, --display=roots       Root stream list (standalone top-level streams)
 *  -Dc, --display=crashed     Signal information and crashed thread context
 *  -Dt, --display=threads     All thread contexts (status and registers)
 *  -Dm, --display=mapping     Memory mapping information in /proc/PID/maps format
 *  -Dv, --display=verbose     Display all displayable sections
 */
public class DisplayOptions extends EnumListOptions<DisplaySection> {
	public DisplayOptions() {
		super('D', "display", DisplaySection.class);
	}

	@Override
	public DisplaySection recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 's': return DisplaySection.STATS;
			case 'r': return DisplaySection.ROOTS;
			case 'c': return DisplaySection.CRASHED;
			case 't': return DisplaySection.THREADS;
			case 'm': return DisplaySection.MAPPING;
			case 'h': return DisplaySection.HAMMING;
			case 'v': return DisplaySection.VERBOSE;
			case 'D': return DisplaySection.DEBUG;
			default:
				return null;
		}
	}

	@Override
	public DisplaySection recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "stats":   return DisplaySection.STATS;
			case "roots":   return DisplaySection.ROOTS;
			case "crashed": return DisplaySection.CRASHED;
			case "threads": return DisplaySection.THREADS;
			case "mapping": return DisplaySection.MAPPING;
			case "hamming": return DisplaySection.HAMMING;
			case "verbose": return DisplaySection.VERBOSE;
			case "debug":   return DisplaySection.DEBUG;
			default:
				return null;
		}
	}

	@Override
	protected void onOptionSet(DisplaySection key) throws ConfigurationException {
		if (key == DisplaySection.VERBOSE) {
			enumSet.addAll(EnumSet.complementOf(EnumSet.of(DisplaySection.DEBUG)));
		}
	}
}
