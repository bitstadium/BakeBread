/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.nio.Memory;

/**
 * Resolved memory area with contents.
 */
public class ResolvedMemData extends ResolvedMemArea implements MemData {
	private final Memory data;

	public ResolvedMemData(MemArea memArea, Memory data) {
		super(memArea);
		this.data = data;
	}

	public ResolvedMemData(long start, long end, Memory data) {
		super(start, end);
		this.data = data;
	}

	@Override
	public Memory getData() {
		return data;
	}

	@Override
	public MemData trimTo(MemArea memArea) {
		final MemArea common = Areas.trim(this, memArea);
		if (common == this) {
			return this;
		}
		return new ResolvedMemData(common, data.transform(this, common));
	}

	@Override
	public String toString() {
		return super.toString() + ":" + getData();
	}
}
