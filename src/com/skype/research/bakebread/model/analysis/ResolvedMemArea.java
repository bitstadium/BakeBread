/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.memory.MemArea;

/**
 * A map area with resolved boundaries
 */
public class ResolvedMemArea implements MemArea {
	private final long startAddress, endAddress;

	public ResolvedMemArea(MemArea memArea) {
		this(memArea.getStartAddress(), memArea.getEndAddress());
	}

	public ResolvedMemArea(long startAddress, long endAddress) {
		this(startAddress, endAddress, false);
	}
	
	public ResolvedMemArea(long startAddress, long endAddress, boolean relaxed) {
		this.startAddress = startAddress;
		this.endAddress = endAddress;
		if (!relaxed) {
			Areas.validate(this);
		}
	}

	public ResolvedMemArea(MemArea memArea, boolean relaxed) {
		this(memArea.getStartAddress(), memArea.getEndAddress(), relaxed);
	}

	@Override
	public long getStartAddress() {
		return startAddress;
	}

	@Override
	public long getEndAddress() {
		return endAddress;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MemArea)) return false;
		MemArea that = (ResolvedMemArea) o;
		return startAddress == that.getStartAddress() && endAddress == that.getEndAddress();
	}

	@Override
	public int hashCode() {
		int result = (int) (startAddress ^ (startAddress >>> 32));
		result = 31 * result + (int) (endAddress ^ (endAddress >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return PrettyPrint.hexRangeSlim(this);
	}
}
