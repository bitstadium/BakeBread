/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.bakebread.nio.Memory;

import java.io.File;

/**
 * A mapped area with resolved contents and status.
 */
public class ResolvedMemLoad extends ResolvedMemData implements MemLoad {

	private final MemLoad orig;

	public ResolvedMemLoad(MemArea area, Memory data, MemLoad orig) {
		super(area, data);
		this.orig = orig;
	}

	@Override
	public File getFile() {
		return orig.getFile();
	}

	@Override
	public boolean isDumpData() {
		return orig.isDumpData();
	}

	@Override
	public boolean isHostData() {
		return orig.isHostData();
	}

	@Override
	public boolean isReliable() {
		return orig.isReliable();
	}

	public MapInfo getMapInfo() {
		return orig.getMapInfo();
	}

	// TODO extract into AbstractMemLoad
	@Override
	public MemLoad trimTo(MemArea memArea) {
		MemArea common = Areas.trim(this, memArea);
		if (common == this) {
			return this;
		}
		return new ResolvedMemLoad(common, getData().transform(this, common), orig);
	}

	// TODO extract into AbstractMemLoad
	@Override
	public String toString() {
		return super.toString()
				+ "/" + PrettyPrint.printFlags(getMapInfo())
				+ (getFile() != null ? "/" + getFile().getName() : "");
	}
}
