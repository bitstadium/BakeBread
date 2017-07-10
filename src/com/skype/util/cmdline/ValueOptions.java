/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.util.Iterator;

/**
 * Recognize top-level options that don't split into a family of attribute-value properties.
 */
public abstract class ValueOptions extends AbstractOptions {
	public ValueOptions(char abbrForm, String longForm) {
		super(abbrForm, longForm);
	}

	@Override
	protected boolean recognizeAbbr(String substring, Iterator<String> parameters)
			throws RecognitionException, ConfigurationException {
		return substring.isEmpty() && parameters.hasNext() && recognizeValue(parameters.next());
	}

	@Override
	protected boolean recognizeLong(String substring, Iterator<String> parameters)
			throws RecognitionException, ConfigurationException {
		return substring.startsWith(LONG_EQUALS) && recognizeValue(substring.substring(LONG_EQUALS.length()));
	}

	protected abstract boolean recognizeValue(String path);
}
