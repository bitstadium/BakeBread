/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.host;

import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.ResolvedMemData;
import com.skype.research.bakebread.model.analysis.ResolvedMemLoad;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.bakebread.nio.FileMemory;

import java.io.File;

/**
 * Mapped host file.
 */
public class FileLoad extends ResolvedMemData implements MemLoad {
	private final File file;
	private final MapInfo mapInfo;
	
	public FileLoad(MapInfo mapInfo, File file, AutoClose autoClose) {
		this(Areas.trim(mapInfo, file.length() - mapInfo.getFileOffset()), file, mapInfo, autoClose);
	}
	
	public FileLoad(MemArea memArea, File file, MapInfo mapInfo, AutoClose autoClose) {
		super(memArea, new FileMemory(file, mapInfo.getFileOffset(), Areas.length(memArea), autoClose));
		this.mapInfo = mapInfo;
		this.file = file;
	}

	@Override
	public File getFile() {
		return file;
	}

	@Override
	public boolean isDumpData() {
		return false;
	}

	@Override
	public boolean isHostData() {
		return true;
	}

	@Override
	public boolean isReliable() {
		return !getMapInfo().isWritable();
	}

	public MapInfo getMapInfo() {
		return mapInfo;
	}

	// TODO extract into AbstractMemLoad
	@Override
	public MemLoad trimTo(MemArea memArea) {
		final MemArea common = Areas.trim(this, memArea);
		if (common == this) {
			return this;
		}
		return new ResolvedMemLoad(common, getData().transform(this, common), this);
	}
}
