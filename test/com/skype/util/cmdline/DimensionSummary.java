/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

/**
 * A dimension summary string
 */
public class DimensionSummary extends ValueOptions {

	private String summaryString;

	public DimensionSummary() {
		super('D', "dimension");
	}

	protected boolean recognizeValue(String value) {
		if (value.isEmpty()) {
			return false;
		}
		summaryString = value;
		return true;
	}

	public String getSummaryString() {
		return summaryString;
	}
}
