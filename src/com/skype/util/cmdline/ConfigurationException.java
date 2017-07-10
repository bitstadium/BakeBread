/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

/**
 * Thrown when applying a configuration option produces inconsistent configuration.
 */
public class ConfigurationException extends Exception {
	private final String context;
	
	public <E extends Enum<E>> ConfigurationException(String context, E option, String value) {
		super(option.name() + ": " + value);
		this.context = context; 
	}

	public String getContext() {
		return context;
	}
}
