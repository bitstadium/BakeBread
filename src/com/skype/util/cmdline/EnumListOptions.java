/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * Closed list option family.
 */
public abstract class EnumListOptions<E extends Enum<E>> extends AbstractOptions implements EnumRouter<E> {
	protected final EnumSet<E> enumSet;
	
	public EnumListOptions(char abbrForm, String longForm, Class<E> enumClass) {
		super(abbrForm, longForm);
		enumSet = EnumSet.noneOf(enumClass);
	}

	@Override
	protected boolean recognizeAbbr(String substring, Iterator<String> parameters) throws RecognitionException, ConfigurationException {
		final char[] keys = substring.toCharArray();
		return recognizeKeys(keys.length, new IToE<E>() {
			@Override
			public E recognize(int i) throws RecognitionException {
				return recognizeAbbr(keys[i]);
			}
		});
	}

	@Override
	protected boolean recognizeLong(String substring, Iterator<String> parameters) throws RecognitionException, ConfigurationException {
		return substring.startsWith(LONG_EQUALS)
				&& recognizeList(substring.substring(LONG_EQUALS.length()));
	}

	private boolean recognizeList(String substring) throws RecognitionException, ConfigurationException {
		final String[] keys = substring.split(SEP_PATTERN);
		return recognizeKeys(keys.length, new IToE<E>() {
			@Override
			public E recognize(int i) throws RecognitionException {
				return recognizeLong(keys[i]);
			}
		});
	}

	private boolean recognizeKeys(int keyCount, IToE<E> iToE) throws RecognitionException, ConfigurationException {
		for (int i = 0; i < keyCount; i++) {
			E key = iToE.recognize(i);
			if (key != null) {
				enumSet.add(key);
				onOptionSet(key);
			} else {
				return false;
			}
		}
		return true;
	}
	
	protected void enforceMutualExclusion(E key) throws ConfigurationException {
		if (enumSet.size() > 1) {
			throw new ConfigurationException(enumSet.toString(), key, Boolean.toString(true));
		}
	}

	interface IToE<E> {
		E recognize(int i) throws RecognitionException;
	}

	protected void onOptionSet(E key) throws ConfigurationException {};

	public boolean isOptionSet(E key) {
		return enumSet.contains(key);
	}

	// copy out
	public Collection<E> getOptions() {
		return enumSet.clone();
	}
}
