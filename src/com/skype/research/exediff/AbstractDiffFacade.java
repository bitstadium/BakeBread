/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff;

import com.skype.research.exediff.bleach.Bleach;
import com.skype.research.exediff.config.Thresholds;
import com.skype.util.partition.metric.Metrics;
import com.skype.util.partition.rolling.HashRollers;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Common stuff put together for reuse.
 */
public abstract class AbstractDiffFacade {
	
	// this is mostly printing stuff and it should go to Hexualizer
	protected static final String __KEY_VALUE_ = "%s: %s%n";
	protected static final String MAPPING_INFO = "Mapping";
	protected static final String QUALITY_INFO = "Quality";
	protected static final String HAMMING_INFO = "Hamming:"; // multiline
	protected static final String COST_INITIAL  = "Costs of sequential (coarse) match:"; // multiline
	protected static final String COST_HEALING  = "Costs of refinement (greedy) match:"; // multiline
	protected static final String INTRODUCTION = "==================================================================="; // 68 =s

	// resulting setup
	protected HashRollers roller;
	protected Metrics metric;
	protected boolean greedyHeal;
	protected Thresholds thresholds;
	protected PrintWriter diffplay;
	protected File diffFile;
	protected Collection<Bleach> bleachOps = new LinkedList<>();
	protected boolean summarize;
	protected boolean showCosts;

	protected static ByteBuffer copyOf(ByteBuffer bb) {
		byte[] array = new byte[bb.capacity()];
		bb = bb.duplicate();
		bb.clear();
		bb.get(array);
		return ByteBuffer.wrap(array);
	}

	// the design here is highly experimental and I don't care about decomposition
	// and modularity so far. for now, I am just plugging in some common options.

	public void addBleach(Bleach bleach) {
		bleachOps.add(bleach);
	}

	public void setRoller(HashRollers roller) {
		this.roller = roller;
	}

	public void setMetric(Metrics metric) {
		this.metric = metric;
	}

	public void setGreedyHeal(boolean greedyHeal) {
		this.greedyHeal = greedyHeal;
	}

	public void setThresholds(Thresholds thresholds) {
		this.thresholds = thresholds;
	}

	public void setDiffplay(PrintStream diffplay) {
		this.diffplay = new PrintWriter(diffplay);
	}

	public void setDiffplay(PrintWriter diffplay) {
		this.diffplay = diffplay;
	}

	public void setSummarize(boolean summarize) {
		this.summarize = summarize;
	}

	public void setShowCosts(boolean showCosts) {
		this.showCosts = showCosts;
	}

	public void setDiffFile(File diffFile) {
		//noinspection ResultOfMethodCallIgnored
		diffFile.delete();
		this.diffFile = diffFile;
	}

	protected ByteBuffer bleach(ByteBuffer bb) {
		if (bleachOps.isEmpty()) {
			return bb;
		} else {
			bb = copyOf(bb);
			for (Bleach bleach : bleachOps) {
				bleach.bleach(bb);
			}
		}
		return bb;
	}
}
