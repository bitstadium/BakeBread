/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.model;

import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.ResolvedMemArea;
import com.skype.research.bakebread.model.memory.MemArea;

/**
 * A correlated gap, mismatch, misalignment between correlated seams.
 */
public class MemHole extends ResolvedMemArea implements MemPair<MemHole> {
	private final MemHole inverse;
	
	public MemHole(long curStartAddress, long curEndAddress, long invStartAddress, long invEndAddress) {
		super(curStartAddress, curEndAddress);
		this.inverse = new MemHole(invStartAddress, invEndAddress, this);
	}
	
	public MemHole(MemArea current, long invStartAddress, long invEndAddress) {
		super(current);
		this.inverse = new MemHole(invStartAddress, invEndAddress, this);
	}

	private MemHole(long curStartAddress, long curEndAddress, MemHole inverse) {
		super(curStartAddress, curEndAddress, true);
		this.inverse = inverse;
	}
	
	public MemHole(long curStartAddress, long curEndAddress, MemArea inverse) {
		super(curStartAddress, curEndAddress);
		this.inverse = new MemHole(inverse, this);
	}
	
	private MemHole(MemArea current, MemHole inverse) {
		super(current, true);
		this.inverse = inverse;
	}
	
	public MemHole(MemArea current, MemArea inverse) {
		super(current);
		this.inverse = new MemHole(inverse, this);
	}

	@Override
	public MemHole inverse() {
		return inverse;
	}
	
	public boolean isPureInsert() {
		return Areas.isEmpty(inverse);
	}
	
	public boolean isPureDelete() {
		return Areas.isEmpty(this);
	}
	
	public boolean isPureStitch() {
		return isPureInsert() && isPureDelete();
	}

	public boolean isSomeInsert() {
		return getEndAddress() > getStartAddress();
	}

	public boolean isSomeDelete() {
		return inverse.getEndAddress() > inverse.getStartAddress();
	}

	public boolean isNontrivial() {
		return !isPureStitch();
	}
}
