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
import com.skype.research.bakebread.nio.FillMemory;

import java.io.File;

/**
 * No data available, filling with a predefined value.
 */
public class FillLoad extends ResolvedMemData implements MemLoad {

	private final MapInfo mapInfo;
	private final byte[] pattern;

	public FillLoad(MapInfo mapInfo, byte[] pattern) {
		this(mapInfo, pattern, mapInfo);
	}

	public FillLoad(MemArea memArea, byte[] pattern, MapInfo mapInfo) {
		super(memArea, new FillMemory(pattern, memArea));
		this.mapInfo = mapInfo;
		this.pattern = pattern;
	}

	@Override
	public File getFile() {
		return null;
	}

	@Override
	public boolean isDumpData() {
		return false;
	}

	@Override
	public boolean isHostData() {
		return false;
	}

	@Override
	public boolean isReliable() {
		return false;
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
		return new FillLoad(common, pattern, mapInfo);
	}

	@Override
	public String toString() {
		return super.toString()
				+ "/" + PrettyPrint.printFlags(getMapInfo())
				+ (getFile() != null ? "/" + getFile().getName() : "");
	}
}
