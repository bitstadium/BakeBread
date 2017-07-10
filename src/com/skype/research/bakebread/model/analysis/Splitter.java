/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.config.BitExactValidation;
import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.config.ValConfig;
import com.skype.research.bakebread.model.host.FileFinder;
import com.skype.research.bakebread.model.host.FileMapper;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.model.memory.MemLoad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Static analysis methods. We'll figure out where we host them.
 */
public class Splitter implements LoadRegistrar {

	private final MemHeap<MapInfo> mapping;
	private final MemHeap<MemLoad> streams;
	private final ValConfig valConfig;
	private final Validator validator;

	/**
	 * Given available dump and host data, reconstruct the big memory picture
	 * according to {@link Credibility}-defined priorities.
	 * <ul>
	 *   <li>Dump data</li>
	 *   <li>Host data</li>
	 *   <li>Fill data</li>
	 * </ul>
	 * @param mapping process memory map. If unknown, generate with {@link ModuleAnalysis} from module list.
	 * @param streams streams readily available as data
	 * @param valConfig validation options for redundant (overlapping) data
	 * @param validator
	 */
	private Splitter(MemHeap<MapInfo> mapping, Collection<? extends MemData> streams, ValConfig valConfig, Validator validator) {
		this.valConfig = valConfig;
		this.validator = validator;
		this.mapping = mapping;
		this.streams = recordStreams(streams, valConfig);
	}

	private MemHeap<MemLoad> recordStreams(Collection<? extends MemData> inStreams, ValConfig valConfig) {
		MemHeap<MemLoad> streams = new MemHeap<>();
		if (!inStreams.isEmpty()) {
			final PriorityQueue<MemData> bySize = new PriorityQueue<>(inStreams.size(), AreaProp.LENGTH.dec);
			bySize.addAll(inStreams); // no deduplication here

			MemHeap<MemData> chain = new MemHeap<>();
			MemData stream;
			while ((stream = bySize.poll()) != null) {
				if (valConfig.isValidationTypeEnabled(BitExactValidation.DUMP_INTERNAL)) {
					validateDumpData(chain, stream);
				}
				MemArea unique = chain.trimToUnique(stream);
				if (!Areas.isEmpty(unique)) {
					chain.add(stream.trimTo(unique));
				}
			}
			for (MemData chunk : chain.values()) {
				for (MapInfo mapInfo : this.mapping.subMap(chunk, false, false, true).values()) {
					streams.add(new DumpLoad(mapInfo, chunk.trimTo(mapInfo)));
				}
			}
		}
		return streams;
	}

	/**
	 * Cross-validate the newly added stream against existing ones, excluding writable.
	 * @param chain existing memory data
	 * @param stream newly added memory chunk
	 */
	private void validateDumpData(MemHeap<MemData> chain, MemData stream) {
		// exclude writable and shared areas as they may have changed
		Collection<? extends MemArea> roRanges = Collections.<MemArea>singleton(stream);
		for (MapInfo mapInfo : this.mapping.subMap(stream, true, true, false).values()) {
			if (mapInfo.isWritable() || mapInfo.isShared()) {
				roRanges = Areas.subtract(roRanges, mapInfo);
			}
		}
		Collection<MemData> roStreams = Areas.sliceInto(stream, roRanges);
		// compare with overlapping recorded data
		SortedMap<MemArea, MemData> map = chain.subMap(stream, true, true, false);
		for (MemData lapped : map.values()) {
			for (MemData roStream : roStreams) {
				if (!Areas.bytesEqual(roStream, lapped)) {
					throw new InputMismatchException(stream + " != " + lapped);
				}
			}
		}
	}

	// preparatory
	private final SortedSet<Boundary> bounds = new TreeSet<>();
	private final List<MemLoad> result = new ArrayList<>(); // encourage RandomAccess
	
	public static List<MemLoad> split(Collection<? extends MapInfo> mapping,
	                                  Collection<? extends MemData> streams,
	                                  FileFinder fileFinder,
	                                  FileMapper fileMapper,
	                                  ValConfig valConfig,
	                                  Validator validator) {
		return new Splitter(new MemHeap<>(mapping), streams, valConfig, validator).split(fileFinder, fileMapper);
	}
	
	/**
	 * @return the full ordered sequence of {@link MemLoad} elements.
	 * @param fileFinder locator of mapped target files on the host machine
	 * @param fileMapper file contents on-demand retriever (File -> Memory)
	 */
	private List<MemLoad> split(FileFinder fileFinder, FileMapper fileMapper) {
		for (MemLoad memLoad : streams.values()) {
			register(memLoad, Credibility.Dump);
		}
		fileMapper.start(mapping);
		for (MapInfo mapInfo : mapping.values()) {
			fileMapper.mapRegion(mapInfo, fileFinder, this);
		}
		fileMapper.flush(this);
		collectBounds();
		return result;
	}

	@Override
	public void register(MemLoad stream, Credibility credibility) {
		Areas.validate(stream);
		bounds.add(new Boundary(credibility, true, stream));
		bounds.add(new Boundary(credibility, false, stream));
	}

	private void collectBounds() {
		MutableMemArea window = new MutableMemArea();
		window.setStartAddress(0);
		final EnumMap<Credibility, Boundary> src = new EnumMap<>(Credibility.class);
		for (Boundary boundary : bounds) {
			Boundary prev = Credibility.mostCredibleOrNull(src);
			if (boundary.start) {
				Boundary existing = src.put(boundary.r, boundary);
				if (existing != null) {
					throw new IllegalStateException("Mappings "
							+ existing + " and " + boundary.chunk + "overlap");
				}
			} else {
				if (src.remove(boundary.r) == null) {
					throw new IllegalStateException("Closing a mapping that isn't open: "
							+ boundary.chunk);
				}
			}
			Boundary next = Credibility.mostCredibleOrNull(src);
			if (prev != next) {
				if (boundary.start) {
					// added and changed; we are the most credible now
					if (prev != null) {
						/// add the previous most credible one
						window.setEndAddress(boundary.chunk.getStartAddress());
						// -see- http://www.airs.com/blog/archives/189
						if (valConfig.isValidationTypeEnabled(BitExactValidation.HOST_AND_DUMP)
								&& boundary.chunk.isReliable() && prev.chunk.isReliable()) {
							validator.compare(boundary.chunk, prev.chunk);
						}
						addLoad(prev.chunk, window);
					}
				} else {
					// removed and changed; we were the most credible
					window.setEndAddress(boundary.chunk.getEndAddress());
					addLoad(boundary.chunk, window);
				}
			}
		}
		if (!src.isEmpty()) {
			throw new IllegalStateException("Not all mappings closed: " + src);
		}
	}

	private void addLoad(MemLoad memLoad, MutableMemArea window) {
		memLoad = memLoad.trimTo(window);
		if (!Areas.isEmpty(memLoad)) {
			result.add(memLoad);
		}
		window.setStartAddress(memLoad.getEndAddress());
	}

	// MOREINFO is it possible to feed an arbitrary (e.g. minidump) file mapping to GDB/BFD? worth a try
	
	static class Boundary implements Comparable<Boundary> {
		final Credibility r;
		final boolean start;
		final MemLoad chunk; // may be null; fill will be used

		Boundary(Credibility credibility, boolean start, MemLoad chunk) {
			this.r = credibility;
			this.start = start;
			this.chunk = chunk;
		}
		
		public long getAddress() {
			return start ? chunk.getStartAddress() : chunk.getEndAddress();
		}

		@Override
		public int compareTo(Boundary other) {
			int cmp = Long.compare(getAddress(), other.getAddress());
			if (cmp != 0) return cmp;
			cmp = Boolean.compare(start, other.start);
			if (cmp != 0) return cmp;
			if (start) {
				return other.r.compareTo(r); // weaker starts first
			} else {
				return r.compareTo(other.r); // weaker lasts longer
			}
		}
	}
}
