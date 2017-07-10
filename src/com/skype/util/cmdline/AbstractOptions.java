/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.util.Iterator;

/**
 * Defines an option family.
 */
public abstract class AbstractOptions implements Options {
	protected static final String ABBR_PREFIX = "-";
	protected static final String LONG_PREFIX = "--";
	protected static final String LONG_EQUALS = "=";
	protected static final String FINE_PREFIX = "-";
	protected static final String SEP_PATTERN = ",";
	
	final String abbrForm;
	final String longForm;

	public AbstractOptions(char abbrForm, String longForm) {
		this.abbrForm = ABBR_PREFIX + abbrForm;
		this.longForm = LONG_PREFIX + longForm;
	}
	
	@Override
	public boolean recognize(String option, Iterator<String> parameters) 
			throws RecognitionException, ConfigurationException {
		return option.startsWith(longForm) && recognizeLong(option.substring(longForm.length()), parameters)
			|| option.startsWith(abbrForm) && recognizeAbbr(option.substring(abbrForm.length()), parameters);
	}

	protected abstract boolean recognizeAbbr(String substring, Iterator<String> parameters)
			throws RecognitionException, ConfigurationException;
	protected abstract boolean recognizeLong(String substring, Iterator<String> parameters) 
			throws RecognitionException, ConfigurationException;
}
