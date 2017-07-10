/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.exception.MalformedException;

/**
 * A mini dump parsing/navigation error.
 */
public class MalformedMiniDumpException extends MalformedException {
	public MalformedMiniDumpException(String whatFailed) {
		super(whatFailed);
	}
}
