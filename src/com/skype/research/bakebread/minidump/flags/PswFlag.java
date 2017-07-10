/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.flags;

/**
 * Processor state word flags.
 */
public enum PswFlag implements Flag {
	Negative(0x80000000),
	Zero(0x40000000),
	Overflow(0x20000000),
	Carry(0x10000000),
	Thumb(0x00000010);
	
	final int bit;
	
	PswFlag(int bit) {
		this.bit = bit;
	}
	
	@Override
	public int getBit() {
		return bit;
	}
}
