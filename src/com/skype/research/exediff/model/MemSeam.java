/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.model;

import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.MutableMemArea;
import com.skype.research.bakebread.model.analysis.ResolvedMemArea;
import com.skype.research.bakebread.model.memory.MemArea;

import java.nio.ByteBuffer;

/**
 * Represents a range with associated translation.
 */
public class MemSeam extends MutableMemArea implements MemPair<MemSeam> {
	private final int step;
	private final int drift;
	
	// extra info that's just convenient to store within the mapping itself
	private final boolean mainSequence;

	public MemSeam(MemArea memArea, int step, int drift, boolean mainSequence) {
		this(memArea.getStartAddress(), memArea.getEndAddress(), step, drift, mainSequence);
	}

	public MemSeam(long startAddress, long endAddress, int step, int drift, boolean mainSequence) {
		setStartAddress(startAddress);
		setEndAddress(endAddress);
		this.step = step;
		this.drift = drift;
		this.mainSequence = mainSequence;
	}

	public MemSeam trimTo(MemArea memArea) {
		MemArea common = Areas.trim(this, memArea);
		if (this == common) {
			return this;
		}
		
		return new MemSeam(common, step, drift, mainSequence);
	}
	
	public void trimInPlaceTo(long start, long after) {
		if (getStartAddress() < start) {
			setStartAddress(start);
		}
		if (getEndAddress() > after) {
			setEndAddress(after);
		}
	}

	public void trimInverseTo(long inverseStart, long inverseAfter) {
		MemSeam inverse = inverse();
		final long overflowStart = inverseStart - inverse.getStartAddress();
		if (overflowStart > 0) {
			setStartAddress(getStartAddress() + overflowStart);
		}
		final long overflowAfter = inverse.getEndAddress() - inverseAfter;
		if (overflowAfter > 0) {
			setEndAddress(getEndAddress() - overflowAfter);
		}
	}
	
	public int getStep() {
		return step;
	}

	public long getTranslation() {
		return step * drift;
	}
	
	public boolean isMainSequence() {
		return mainSequence;
	}

	@Override
	public MemSeam inverse() {
		long translation = getTranslation();
		return new MemSeam(getStartAddress() + translation, getEndAddress() + translation, step, - drift, mainSequence);
	}

	public int getDrift() {
		return drift;
	}

	public MemSeam expandTo(MemArea memArea) {
		return expandTo(memArea.getStartAddress(), memArea.getEndAddress());
	}
	
	public MemSeam expandTo(long startAddress, long endAddress) {
		if (startAddress == getStartAddress() && endAddress == getEndAddress()) {
			return this;
		}
		return new MemSeam(startAddress, endAddress, step, drift, mainSequence);
	}

	// this method could be propagated up to ResolvedMemArea etc. unless MemSeam
	// were the only memory range to represent local buffer or array coordinates
	// rather than some embedding address space
	public ByteBuffer getRange(ByteBuffer byteBuffer) {
		// the order is important - the limit is set first and the position next
		byteBuffer.limit((int) getEndAddress()).position((int) getStartAddress());
		return byteBuffer;
	}

	public MemArea translate(MemArea memArea) {
		final long translation = getTranslation();
		return new ResolvedMemArea(memArea.getStartAddress() + translation, memArea.getEndAddress() + translation, true);
	}

	@Override
	public String toString() {
		return super.toString() + '[' + (mainSequence 
				? (drift == 0 ? "==" : String.format("%+d", getTranslation())) 
				: String.format("=%06x", getStartAddress() + getTranslation()))
				+ ']';
	}
}
