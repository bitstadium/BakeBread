/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.memory;

/**
 * Memory access permissions.
 */
public interface MemPerm {
	boolean isReadable();
	boolean isWritable();
	boolean isRunnable();
	boolean isShared();
}
