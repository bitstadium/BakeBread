/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.exception.MalformedException;

/**
 * A mini dump parsing/navigation error.
 */
public class MalformedElfMagicException extends MalformedException {
	public MalformedElfMagicException(String whatFailed) {
		super(whatFailed);
	}
}
