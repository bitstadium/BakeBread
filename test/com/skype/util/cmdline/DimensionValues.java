/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

/**
 * A sample multiple choice configuration class.
 */
class DimensionValues extends OpenEnumOptions<Dimension> {

	public DimensionValues() {
		super('D', "dimension", Dimension.class, null);
	}
	
	final float[] dimensionVector = new float[Dimension.values().length];

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

	@Override
	protected void onValueSet(Dimension key, String value) throws ConfigurationException {
		try {
			dimensionVector[key.ordinal()] = Float.parseFloat(value);
		} catch (NumberFormatException nfe) {
			throw new ConfigurationException(enumMap.toString(), key, value);
		}
	}

	public float[] getVector() {
		return dimensionVector;
	}
}
