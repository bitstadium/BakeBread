/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.exception;

/**
 * The differencing/matching/proximity engine has failed to retrieve
 *  common features of objects or sequences being compared. 
 */
public class UnreliableMatchException extends HeuristicException {
	public UnreliableMatchException() {
	}

	public UnreliableMatchException(String message) {
		super(message);
	}

	public UnreliableMatchException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnreliableMatchException(Throwable cause) {
		super(cause);
	}
}
