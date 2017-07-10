/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.present;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.exediff.match.SeamDiff;

import java.io.PrintWriter;

/**
 * Print various statistics.
 */
public class PrettyTotal {
	private PrettyTotal() {}

	public static void displayBulkMappedBytes(PrintWriter writer, HammingStat stat) {
		long mappedOrd = stat.ordered.getCommon();
		long mappedOOO = stat.ooOrder.getCommon();
		long mappedAll = mappedOrd + mappedOOO;
		long modLength = stat.control.getCommon() + stat.control.getUnique();
		writer.println(String.format("Monotonic matches: %d bytes (%.04f%%)",
				mappedOrd, PrettyPrint.percents(mappedOrd, modLength)));
		writer.println(String.format("Displaced matches: %d bytes (%.04f%%)",
				mappedOOO, PrettyPrint.percents(mappedOOO, modLength)));
		writer.println(String.format("All matches found: %d bytes (%.04f%%)",
				mappedAll, PrettyPrint.percents(mappedAll, modLength)));
	}

	public static void displayDetailedHamming(PrintWriter writer, HammingStat stat) {
		writer.println("control=" + stat.control);
		writer.println("ordered=" + stat.ordered);
		writer.println("ooOrder=" + stat.ooOrder);
		writer.println("overall=" + stat.overall);
	}

	public static void displayOutlierPercents(PrintWriter writer, SeamDiff diff) {
		int outliers = diff.getOutlierCount();
		int stitches = diff.getStitchCount();

		writer.println(String.format("%d of %d, or %.04f%% unconfirmed matches",
				outliers, stitches, PrettyPrint.percents(outliers, stitches)));
	}
}
