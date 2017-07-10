/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.match;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.util.partition.MetricTree;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Cost estimate in comparison and adjustment operations per matched elements.
 */
public class Cost {
	private long offered;
	private long updated;
	private int inexact;
	// denominator
	private int stitches;

	// eventual "byte" result
	private long totalB;
	private long mapped = 0;

	public Cost(long totalB) {
		this.totalB = totalB;
	}

	public void reportHealingCosts(int mismatches, int comparisons, int adjustments) {
		this.stitches+= comparisons;
		this.offered += comparisons;
		this.updated += adjustments;
		this.inexact += mismatches;
	}

	public void recordMappingCosts(MetricTree.Match match) {
		++stitches;
		if (match.matchDistance() > 0) {
			++inexact;
		}
		offered += match.offerCount();
		updated += match.updateCount();
	}

	public void recordMappedBytes(long byteCount) {
		this.mapped += byteCount;
	}

	public void report(PrintWriter pw) {
		pw.println(String.format("comparisons=%d or %.04f per match", offered, PrettyPrint.fraction(offered, stitches)));
		pw.println(String.format("refinements=%d or %.04f per match", updated, PrettyPrint.fraction(updated, stitches)));
		pw.println(String.format("approximate matches=%d or %.04f%%", inexact, PrettyPrint.percents(inexact, stitches)));
		pw.println(String.format("mapped=%db or %.04f%% efficiency %.04fb per offer",
				mapped, PrettyPrint.percents(mapped, totalB), PrettyPrint.fraction(mapped, offered)));
	}

	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		report(pw);
		return sw.getBuffer().toString();
	}
}
