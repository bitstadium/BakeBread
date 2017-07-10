/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.exception;

import java.io.IOException;

/**
 * Represents a parsing, validation or navigation exception accessing a resource.
 */
public class MalformedException extends IOException {
	public MalformedException(String whatFailed) {
		super(whatFailed);
	}
}
