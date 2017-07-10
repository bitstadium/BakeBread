/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.flags;

/**
 * Thread context type flags.
 */
public enum CtxFlag implements Flag {
	ARM(0x40000000), INT(0x00000002), VFP(0x00000004);
	
	final int bit;
	
	CtxFlag(int bit) {
		this.bit = bit;
	}
	
	@Override
	public int getBit() {
		return bit;
	}
}
