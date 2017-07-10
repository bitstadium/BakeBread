/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.memory.MemArea;

/**
 * A dynamically adjustable memory area.
 */
public class MutableMemArea implements MemArea {

	private long startAddress;
	private long endAddress;

	@Override
	public long getStartAddress() {
		return startAddress;
	}

	@Override
	public long getEndAddress() {
		return endAddress;
	}

	public void setStartAddress(long startAddress) {
		this.startAddress = startAddress;
	}

	public void setEndAddress(long endAddress) {
		this.endAddress = endAddress;
	}

	@Override
	public String toString() {
		return PrettyPrint.hexRangeSlim(this);
	}

	public void set(long startAddress, long endAddress) {
		setStartAddress(startAddress);
		setEndAddress(endAddress);
	}
	
	public void set(MemArea memArea) {
		set(memArea.getStartAddress(), memArea.getEndAddress());
	}
}
