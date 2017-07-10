/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.exception;

/**
 * Checked exception representing narrow-minded logic that may fail
 */
public class HeuristicException extends Exception {
	public HeuristicException() {
	}

	public HeuristicException(String message) {
		super(message);
	}

	public HeuristicException(String message, Throwable cause) {
		super(message, cause);
	}

	public HeuristicException(Throwable cause) {
		super(cause);
	}
}
