/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.arch;

import com.skype.research.bakebread.config.FogConfig;
import com.skype.research.bakebread.config.MemoryFog;
import com.skype.research.bakebread.coredump.ELF;
import com.skype.research.bakebread.coredump.MalformedElfMagicException;
import com.skype.research.bakebread.coredump.Program;
import com.skype.research.bakebread.coredump.Section;
import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.io.ReverseEndianDataInput;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.Credibility;
import com.skype.research.bakebread.model.analysis.FillLoad;
import com.skype.research.bakebread.model.analysis.LoadRegistrar;
import com.skype.research.bakebread.model.analysis.MemHeap;
import com.skype.research.bakebread.model.analysis.ModuleAnalyzer;
import com.skype.research.bakebread.model.analysis.ResolvedMemArea;
import com.skype.research.bakebread.model.host.FileLoad;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemLoad;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Elf analyzer
 */
public class ElfAnalyzer implements ModuleAnalyzer {
	
	public static class ElfDwelling { // TODO implement ModuleInfo
		private final ELF elf;
		private final int elfSectionCount;
		private final MapInfo[] placement;
		private final MemHeap<Section> fileOrder = new MemHeap<>();
		private final MemHeap<Section> virtOrder = new MemHeap<>();

		public ElfDwelling(ELF elf) {
			this.elf = elf;
			elfSectionCount = elf.getSectionCount();
			placement = new MapInfo[elfSectionCount];
			for (int i = 0; i < elfSectionCount; ++i) {
				Section section = elf.getSection(i);
				if (!Areas.isEmpty(section)) { // skip meta
					virtOrder.add(section);
					fileOrder.put(section.asFileData(), section);
				}
			}
		}

		public ELF getElf() {
			return elf;
		}

		public void setMapInfo(int index, MapInfo mapInfo) {
			placement[index] = mapInfo;
		}

		public Section sectionForAddr(MemHeap<Section> memHeap, long relAddress) {
			return memHeap.get(memHeap.pointMapping(relAddress));
		}

		public Section sectionForFile(long fileOffset) {
			return sectionForAddr(fileOrder, fileOffset);
		}

		public long relocateFromFile(MapInfo mapInfo, long fileOffset) {
			return mapInfo.getStartAddress() + fileOffset - mapInfo.getFileOffset();
		}

		public long relocateFromFile(long fileOffset) {
			return relocateFromFile(placement[elf.getSectionIndex(sectionForFile(fileOffset))], fileOffset);
		}

		public Program getRelocatedReadOnly() {
			return elf.getRelocatedReadOnly();
		}
	}

	private final Map<File, ElfDwelling> elves = new LinkedHashMap<>();
	private final Map<File, ElfDwelling> fossilElves = Collections.unmodifiableMap(elves);
	
	private final Set<File> humans = new LinkedHashSet<>();

	private final Map<File, Long> textOffsets = new LinkedHashMap<>();

	private final FogConfig fogConfig;
	private final AutoClose autoClose;

	public ElfAnalyzer(FogConfig fogConfig, AutoClose autoClose) {
		this.fogConfig = fogConfig;
		this.autoClose = autoClose;
	}
	
	@Override
	public void start(MemHeap<MapInfo> memMap) {
		elves.clear();
		humans.clear();
	}

	@Override
	public boolean analyze(FileLoad fileLoad, LoadRegistrar registrar) {
		File file = fileLoad.getFile();
		if (!humans.contains(file)) {
			MapInfo mapInfo = fileLoad.getMapInfo();
			ElfDwelling placement = elves.get(file);
			if (placement == null) {
				try {
					RandomAccessFile raf = autoClose.register(new RandomAccessFile(file, "r"));
					ELF elf = new ELF(ELF.Preset.ANDROID_32, ELF.Type.DYN);
					elf.readExternal(new ReverseEndianDataInput(raf), raf.getChannel());
					placement = new ElfDwelling(elf);
					elves.put(file, placement);
					// call registrar#register()
					// return true;
				} catch (MalformedElfMagicException ignored) {
					humans.add(file); // not an elf
					return false;
				} catch (IOException ignored) {
					// System.err -> redirect!
					ignored.printStackTrace();
					return false;
				}
			}
			ELF elf = placement.getElf();
			long infoFileOffset = mapInfo.getFileOffset();
			long infoFileCutoff = infoFileOffset + Areas.length(mapInfo);
			for (int index = 0; index < elf.getSectionCount(); ++index) {
				Section section = elf.getSection(index);
				MemArea fileData = section.asFileData();
				// TODO extract as AreaGuide.isIn() condition
				if (fileData.getStartAddress() >= infoFileOffset
						&& fileData.getEndAddress() <= infoFileCutoff) {
					placement.setMapInfo(index, mapInfo);
				}
			}
			if (mapInfo.isRunnable()) {
				Section text = elf.getSection(".text");
				if (text != null) {
					// TODO make this logic reusable
					textOffsets.put(file, mapInfo.getStartAddress() - mapInfo.getFileOffset() + text.getFileOffset());
				}
			} else if (!mapInfo.isWritable() && fogConfig.hasMemoryFillingPattern(MemoryFog.MAPPED_WRITABLE)) {
				MemArea relRo = placement.getRelocatedReadOnly().asFileData();
				if (!Areas.isEmpty(relRo)) {
					MemArea relArea = new ResolvedMemArea(
							placement.relocateFromFile(relRo.getStartAddress()),
							placement.relocateFromFile(relRo.getEndAddress())
					);
					MemLoad relLoad = new FillLoad(relArea, 
							fogConfig.getMemoryFillingPattern(MemoryFog.MAPPED_WRITABLE), 
							mapInfo);
					for (MemLoad memLoad : Areas.sliceInto(fileLoad,
							Areas.subtract(Collections.<MemArea>singleton(fileLoad), relLoad))) {
						registrar.register(memLoad, Credibility.Host);
					}
					registrar.register(relLoad, Credibility.Desc);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void flush(LoadRegistrar registrar) {
		// do nothing
	}

	public Map<File, ElfDwelling> getElves() {
		return fossilElves;
	}

	@Deprecated
	public Map<File, Long> getTextOffsets() {
		return textOffsets;
	}
}
