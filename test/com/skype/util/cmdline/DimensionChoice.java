/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

/**
 * A sample multiple choice configuration class.
 */
class DimensionChoice extends EnumListOptions<Dimension> {

	public DimensionChoice() {
		super('D', "dimension", Dimension.class);
	}

	@Override
	public Dimension recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'x':
				return Dimension.ASIDE;
			case 'y':
				return Dimension.AHEAD;
			case 'z':
				return Dimension.ABOVE;
			default:
				return null;
		}
	}

	@Override
	public Dimension recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "aside":
				return Dimension.ASIDE;
			case "ahead":
				return Dimension.AHEAD;
			case "above":
				return Dimension.ABOVE;
			default:
				return null;
		}
	}
}
