/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis.mock;

import com.skype.research.bakebread.model.memory.MemPerm;

/**
 * One of the most common permission sets.
 */
public enum PermSet implements MemPerm {
	RO_DATA(true, false, false, false),
	RW_DATA(true, true, false, false),
	LIBRARY(true, false, true, false),
	PACKAGE(true, false, false, true),
	IPC_DEV(true, true, false, true),
	PRIVATE(false, false, false, false),;

	private final boolean readable;
	private final boolean writable;
	private final boolean runnable;
	private final boolean shared;

	PermSet(boolean readable, boolean writable, boolean runnable, boolean shared) {
		this.readable = readable;
		this.writable = writable;
		this.runnable = runnable;
		this.shared = shared;
	}

	@Override
	public boolean isReadable() {
		return readable;
	}

	@Override
	public boolean isWritable() {
		return writable;
	}

	@Override
	public boolean isRunnable() {
		return runnable;
	}

	@Override
	public boolean isShared() {
		return shared;
	}
}
