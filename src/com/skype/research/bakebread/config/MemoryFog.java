/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

/**
 *  -Fr, --fill-run-time       [1] areas not mapped to any file (e.g. heap)
 *  -Fn, --fill-not-found      [2] mapped to a file not found in the provided path
 *  -Fe, --fill-file-end       [3] mapped to a file but extending beyond its end
 *  -Fu, --fill-unreliable         writable data (possibly modified after reading)
 *  -Fp, --fill-private-pg         unreadable private page, can't have been dumped
 */
public enum MemoryFog {
	NO_FILE_MAPPED  ((byte) 0xea),
	FILE_NOT_FOUND  ((byte) 0x04),
	FILE_END_REACHED((byte) 0xe0, (byte) 0xff),
	MAPPED_WRITABLE (null),
	PAGE_UNREADABLE (null),
	;

	private final byte[] defaultFill;

	MemoryFog(byte... defaultFill) {
		this.defaultFill = defaultFill;
	}

	public byte[] getDefaultFill() {
		return defaultFill;
	}
}
