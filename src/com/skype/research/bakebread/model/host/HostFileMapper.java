/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.host;

import com.skype.research.bakebread.config.FogConfig;
import com.skype.research.bakebread.config.MemoryFog;
import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.Credibility;
import com.skype.research.bakebread.model.analysis.FillLoad;
import com.skype.research.bakebread.model.analysis.LoadRegistrar;
import com.skype.research.bakebread.model.analysis.MemHeap;
import com.skype.research.bakebread.model.analysis.ModuleAnalyzer;
import com.skype.research.bakebread.model.analysis.ResolvedMemArea;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;

import java.io.Closeable;
import java.io.File;
import java.util.Deque;
import java.util.LinkedList;

public class HostFileMapper implements Closeable, FileMapper, ModuleAnalyzer {
	private final FogConfig fogConfig;
	private final AutoClose autoClose;

	private final Deque<ModuleAnalyzer> analyzers = new LinkedList<>();

	public HostFileMapper(FogConfig fogConfig, AutoClose autoClose) {
		this.fogConfig = fogConfig;
		this.autoClose = autoClose;
	}

	@Override
	public void start(MemHeap<MapInfo> memMap) {
		for (ModuleAnalyzer analyzer : analyzers) {
			analyzer.start(memMap);
		}
	}

	@Override
	public void mapRegion(MapInfo mapInfo, FileFinder fileFinder, LoadRegistrar registrar) {
		MemoryFog memoryFog;
		MemArea memArea = mapInfo;
		if (mapInfo.getFd() == 0) {
			memoryFog = MemoryFog.NO_FILE_MAPPED;
		} else if (!mapInfo.isReadable()) {
			if (fogConfig.hasMemoryFillingPattern(MemoryFog.PAGE_UNREADABLE)) {
				memoryFog = MemoryFog.PAGE_UNREADABLE;
			} else {
				return; // don't map anything
			}
		} else if (mapInfo.isWritable() && fogConfig.hasMemoryFillingPattern(MemoryFog.MAPPED_WRITABLE)) {
			memoryFog = MemoryFog.MAPPED_WRITABLE;
		} else {
			File file = fileFinder.find(mapInfo);
			if (file == null) {
				memoryFog = MemoryFog.FILE_NOT_FOUND;
			} else {
				if (mapInfo.getFileOffset() > file.length()) {
					memoryFog = MemoryFog.FILE_END_REACHED;
					// TODO if host validation is enabled, this is a good reason to throw
				} else {
					FileLoad fileLoad = new FileLoad(mapInfo, file, autoClose);
					// we'll figure out how to organize a responsibility chain
					analyze(fileLoad, registrar);
					memArea = new ResolvedMemArea(fileLoad.getEndAddress(), mapInfo.getEndAddress());
					if (Areas.isEmpty(memArea)) {
						return;
					} else {
						memoryFog = MemoryFog.FILE_END_REACHED;
					}
				}
			}
		}
		registrar.register(new FillLoad(memArea, fogConfig.getMemoryFillingPattern(memoryFog), mapInfo), Credibility.Desc);
	}

	@Override
	public boolean analyze(FileLoad fileLoad, LoadRegistrar registrar) {
		for (ModuleAnalyzer analyzer : analyzers) {
			if (analyzer.analyze(fileLoad, registrar)) {
				return true;
			}
		}
		// okay I will wash the dishes myself
		registrar.register(fileLoad, Credibility.Host);
		return true;
	}

	public void addAnalyzer(ModuleAnalyzer analyzer) {
		analyzers.addFirst(analyzer);
	}

	public void removeAnalyzer(ModuleAnalyzer analyzer) {
		analyzers.remove(analyzer);
	}

	@Override
	public void flush(LoadRegistrar registrar) {
		for (ModuleAnalyzer analyzer : analyzers) {
			analyzer.flush(registrar);
		}
	}

	@Override
	public void close() {
		autoClose.close();
	}
}
