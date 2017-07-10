/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.research.bakebread.PrettyPrint;

/**
 * Type of a byte stream correlation.
 */
public enum ChangeType {
	INSERTION('+'),
	DELETION ('-'),
	SIDE_SIDE('^') {
		@Override
		public char printable(int raw) {
			return (char) ('0' + Integer.bitCount(raw & 0xff));
		}
	},
	SIDE_SKEW(':'),
	RELOCATED('?'),
	;
	
	public final char prompt;

	ChangeType(char prompt) {
		this.prompt = prompt;
	}
	
	public char printable(int raw) {
		return PrettyPrint.printable((char) raw);
	}
}
