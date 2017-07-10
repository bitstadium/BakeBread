/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Resembles a sequence of memory elements following each other without overlapping.
 * Intersection is counted as "comparison equality" and triggers an exception.
 */
public class MemHeap<M extends MemArea> extends TreeMap<MemArea, M> {

	public static final Comparator<MemArea> stickyMemCmp = new Comparator<MemArea>() {
		@Override
		public int compare(MemArea left, MemArea right) {
			if (left.getEndAddress() <= right.getStartAddress()) {
				return -1;
			}
			if (right.getEndAddress() <= left.getStartAddress()) {
				return 1;
			}
			return 0;
		}
	};

	public MemHeap() {
		super(stickyMemCmp);
	}
	
	public MemHeap(Collection<? extends M> source) {
		super(stickyMemCmp);
		addAll(source);
	}

	/**
	 * Mind the implicit guarantee that subtract cannot return a multi-range
	 * because the length of the subtrahend is always greater than the length of the minuend.
	 * @param memArea original range
	 */
	public MemArea trimToUnique(MemArea memArea) {
		SortedMap<MemArea, M> map = subMap(memArea, true, true, false);
		for (MemArea covered : map.keySet()) {
			memArea = Areas.subtract(memArea, covered);
			if (Areas.isEmpty(memArea)) {
				break;
			}
		}
		return memArea;
	}

	public M map(long address) {
		M mapping = get(pointMapping(address));
		if (mapping == null) {
			throw new NoSuchElementException("Address not mapped: " + Areas.longToHex(address));
		}
		return mapping;
	}
	
	public boolean add(M mapping) {
		if (Areas.isEmpty(mapping)) {
			return false;
		}
		M older = put(mapping, mapping);
		if (older != null) {
			throw new IllegalStateException("Mapping " + mapping + " overlaps with older mapping " + older);
		}
		return true;
	}

	public MemArea pointMapping(long address) {
		return new ResolvedMemArea(address, address + 1);
	}

	public MemArea emptyMapping(long address) {
		return new ResolvedMemArea(address, address);
	}

	/**
	 * @param collection elements to offer
	 * @return whether the contents of this have been modified 
	 */
	public boolean addAll(Collection<? extends M> collection) {
		boolean changed = false;
		if (!collection.isEmpty()) {
			for (M mapping : collection) {
				changed |= add(mapping);
			}
		}
		return changed;
	}

	/**
	 * Get a sequence of mappings covering the range, possibly non-contiguously.
	 * @param memArea range to represent as a sequence of mappings
	 * @param mayBeLoose allow the returned sequence not to fit the provided range tightly
	 * @param mayBeEmpty allow the returned sequence to be empty
	 * @param contiguous require the returned sequence to be contiguous
	 * @return a sorted "identity" map representing the chain 
	 * @throws NoSuchElementException if there are no mappings in the range
	 */
	public SortedMap<MemArea, M> subMap(MemArea memArea, boolean mayBeLoose, boolean mayBeEmpty, boolean contiguous) {
		return subMap(memArea.getStartAddress(), memArea.getEndAddress(), mayBeLoose, mayBeEmpty, contiguous);
	}

	/**
	 * Get a sequence of mappings covering the range, possibly non-contiguously.
	 * @param startAddress start of the range to represent as a sequence of mappings
	 * @param endAddress end of the range to represent as a sequence of mappings
	 * @param mayBeLoose allow the returned sequence not to fit the provided range tightly
	 * @param mayBeEmpty allow the returned sequence to be empty
	 * @param contiguous require the returned sequence to be contiguous
	 * @return a sorted "identity" map representing the chain 
	 * @throws NoSuchElementException if there are no mappings in the range, or can't map otherwise
	 */
	public SortedMap<MemArea, M> subMap(long startAddress, long endAddress,
	                                    boolean mayBeLoose,
	                                    boolean mayBeEmpty,
	                                    boolean contiguous) {
		if (startAddress == endAddress) {
			if (mayBeEmpty) {
				return Collections.unmodifiableSortedMap(new TreeMap<MemArea, M>());
			} else {
				throw new NoSuchElementException("No mappings in the range");
			}
		}
		// in one case we may want to allow empty but forbid loose: when the original range is empty. is it of any use?..
		SortedMap<MemArea, M> map = subMap(
				tryMap(startAddress, mayBeLoose), true,
				tryMap(endAddress-1, mayBeLoose), true);
		if (!mayBeEmpty && map.isEmpty()) {
			throw new NoSuchElementException("No mappings in the range");
		}
		if (contiguous && Areas.isNonContiguous(map.keySet())) {
			throw new NoSuchElementException("Range is not contiguous");
		}
		return map;
	}

	private MemArea tryMap(long address, boolean loose) {
		return loose ? pointMapping(address) : map(address);
	}

	// WISDOM breaks iterators and can't be used in iterator loops as an in-place change. use navigation loops.
	public M overwrite(M memArea) {
		M old = remove(memArea);
		add(memArea);
		return old;
	}
}
