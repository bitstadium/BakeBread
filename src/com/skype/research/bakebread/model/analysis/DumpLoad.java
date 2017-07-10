/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.model.memory.MemLoad;

import java.io.File;

/**
 * Represent a trimmed chunk of data.
 */
public class DumpLoad extends ResolvedMemData implements MemLoad {
	private final MapInfo mapInfo;

	public DumpLoad(MapInfo mapping, MemData memData) {
		super(memData, memData.getData());
		this.mapInfo = mapping;
	}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public boolean isDumpData() {
		return true;
	}

	@Override
	public boolean isHostData() {
		return false;
	}

	@Override
	public boolean isReliable() {
		return true;
	}

	public MapInfo getMapInfo() {
		return mapInfo;
	}

	@Override
	public MemLoad trimTo(MemArea memArea) {
		final MemArea common = Areas.trim(this, memArea);
		if (common == this) {
			return this;
		}
		return new ResolvedMemLoad(common, getData().transform(this, common), this);
	}
	
	@Override
	public String toString() {
		return super.toString()
				+ "/" + PrettyPrint.printFlags(getMapInfo())
				+ (getFile() != null ? "/" + getFile().getName() : "");
	}
}
