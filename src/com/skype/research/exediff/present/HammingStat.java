/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.research.bakebread.model.analysis.AreaProp;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.match.Diff;
import com.skype.research.exediff.match.Hamming;
import com.skype.research.exediff.model.MemSeam;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Common totals and subtotals.
 */
public class HammingStat {
	public final Hamming ooOrder = new Hamming();
	public final Hamming ordered = new Hamming();
	public final Hamming overall = new Hamming();
	public final Hamming control = new Hamming();
	
	private final Map<MemSeam, Hamming> subOrdered;
	private final Map<MemSeam, Hamming> subOOOrder;
	
	public HammingStat(Einsteinian frame) {
		this(frame.sideBySide(), frame);
	}
	
	public HammingStat(Diff diff, Einsteinian frame) {
		this(diff.getOverall(), frame);
	}

	public HammingStat(SortedMap<MemArea, MemSeam> seams, Einsteinian frame) {
		Map<MemSeam, Hamming> subOrdered = new TreeMap<>(AreaProp.START.inc);
		Map<MemSeam, Hamming> subOOOrder = new TreeMap<>(AreaProp.START.inc);
		ByteBuffer bbOriginal = frame.original.bb.duplicate();
		ByteBuffer bbModified = frame.modified.bb.duplicate();
		for (MemSeam modSeam : seams.values()) {
			Hamming partial = new Hamming();
			partial.compare(modSeam.getRange(bbModified), modSeam.inverse().getRange(bbOriginal));
			boolean isMainSeq = modSeam.isMainSequence();
			(isMainSeq ? subOrdered : subOOOrder).put(modSeam, partial);
			(isMainSeq ? ordered : ooOrder).add(partial);
		}
		overall.add(ordered);
		overall.add(ooOrder);
		this.subOrdered = Collections.unmodifiableMap(subOrdered);
		this.subOOOrder = Collections.unmodifiableMap(subOOOrder);
		control.add(frame);
	}

	public Map<MemSeam, Hamming> getSubOrdered() {
		return subOrdered;
	}

	public Map<MemSeam, Hamming> getSubOOOrder() {
		return subOOOrder;
	}
}
