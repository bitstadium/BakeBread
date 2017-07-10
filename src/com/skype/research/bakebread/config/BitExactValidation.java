/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

/**
 * Comparisons between binary data available from multiple sources.
 */
public enum BitExactValidation {
	DUMP_INTERNAL,
	HOST_AND_DUMP,
	STRICT_CHECKS,
	LOOSEN_CHECKS,
}
