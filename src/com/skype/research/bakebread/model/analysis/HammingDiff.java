/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.match.Diff;
import com.skype.research.exediff.model.MemSeam;

import java.util.Collections;
import java.util.SortedMap;

/**
 * Common left subrange.
 */
public class HammingDiff implements Diff {

	private final SortedMap<MemArea, MemSeam> roOrdered;
	private final SortedMap<MemArea, MemSeam> roOverall;
	private final SortedMap<MemArea, MemSeam> roOOOrder;

	public HammingDiff(Einsteinian frame) {
		MemHeap<MemSeam> ordered = frame.sideBySide();
		roOrdered = Collections.unmodifiableSortedMap(ordered);
		roOverall = Collections.unmodifiableSortedMap(ordered);
		roOOOrder = Collections.unmodifiableSortedMap(new MemHeap<MemSeam>());
	}

	@Override
	public SortedMap<MemArea, MemSeam> getOrdered() {
		return roOrdered;
	}

	@Override
	public SortedMap<MemArea, MemSeam> getOOOrder() {
		return roOOOrder;
	}

	@Override
	public SortedMap<MemArea, MemSeam> getOverall() {
		return roOverall;
	}
}
