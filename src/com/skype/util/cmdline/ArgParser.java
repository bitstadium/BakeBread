/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Static utility class implementing the responsibility chain in various ways.
 */
public class ArgParser {
	public static void parseCommandLine(String[] cmdLine, Options... sections) throws RecognitionException, ConfigurationException {
		Iterator<String> params = Arrays.asList(cmdLine).iterator();
		while (params.hasNext()) {
			String arg = params.next();
			for (Options option : sections) {
				if (option.recognize(arg, params))
					break;
			}
		}
	}
}
