/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Enum list option family.
 */
public abstract class OpenEnumOptions<E extends Enum<E>> extends AbstractOptions implements EnumRouter<E> {
	
	protected final String separator;
	protected final EnumMap<E, String> enumMap;
	
	public OpenEnumOptions(char abbrForm, String longForm, Class<E> enumClass, String separator) {
		super(abbrForm, longForm);
		enumMap = new EnumMap<>(enumClass);
		this.separator = separator;
	}

	@Override
	protected boolean recognizeAbbr(String substring, Iterator<String> parameters) throws RecognitionException, ConfigurationException {
		E key;
		return substring.length() == 1
				&& parameters.hasNext()
				&& (key = recognizeAbbr(substring.charAt(0))) != null 
				&& recognizePair(key, parameters.next());
	}

	@Override
	protected boolean recognizeLong(String substring, Iterator<String> parameters) throws RecognitionException, ConfigurationException {
		return substring.startsWith(FINE_PREFIX)
				&& recognizeFine(substring.substring(FINE_PREFIX.length()));
	}

	private boolean recognizeFine(String substring) throws RecognitionException, ConfigurationException {
		int equalPos;
		E key;
		return (equalPos = substring.indexOf(LONG_EQUALS)) >= 0 
				&& (key = recognizeLong(substring.substring(0, equalPos))) != null 
				&& recognizePair(key, substring.substring(equalPos + LONG_EQUALS.length()));
	}

	protected boolean recognizePair(E key, String substring) throws ConfigurationException {
		if (substring.length() > 0) {
			if (separator == null) {
				setValue(key, substring);
			} else {
				for (String value : substring.split(separator)) {
					setValue(key, value);
				}
			}
		}
		return true;
	}

	private void setValue(E key, String value) throws ConfigurationException {
		enumMap.put(key, value);
		onValueSet(key, value);
	}

	protected void onValueSet(E key, String value) throws ConfigurationException {};

	protected void enforceMutualExclusion(E key, String value) throws ConfigurationException {
		if (enumMap.size() > 1) {
			throw new ConfigurationException(enumMap.toString(), key, value);
		}
	}

	public boolean isValueSet(E key) {
		return enumMap.containsKey(key);
	}
	
	public String getValue(E key) {
		return enumMap.get(key);
	}
	
	// copy out
	public Collection<E> getOptions() {
		return enumMap.isEmpty() ? Collections.<E>emptySet(): EnumSet.copyOf(enumMap.keySet());
	}
}
