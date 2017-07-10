/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.util.Iterator;

/**
 * Element of the option responsibility chain.
 */
public interface Options {
	boolean recognize(String option, Iterator<String> parameters) 
			throws RecognitionException, ConfigurationException;
}
