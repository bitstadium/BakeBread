/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.streams;

/**
 * A generic stream definition.
 */
public interface StreamType {
	int type();
	String name();
	int ordinal();
}
