/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.util.cmdline.Options;
import com.skype.util.cmdline.RecognitionException;

import java.util.Iterator;

/**
 * Should not reach here.
 */
public class UnknownOptions implements Options {
	@Override
	public boolean recognize(String option, Iterator<String> parameters) throws RecognitionException {
		throw new RecognitionException(option, parameters);
	}
}
