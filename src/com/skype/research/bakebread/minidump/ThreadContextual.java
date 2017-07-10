/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

/**
 * Embeds a thread context definition.
 */
public interface ThreadContextual {
	LocationDescription getContextLocation();
	ThreadContext getThreadContext();
}
