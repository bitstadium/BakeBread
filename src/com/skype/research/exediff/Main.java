/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff;

import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.nio.BufferAdapter;
import com.skype.research.exediff.bleach.ArmBleach;
import com.skype.research.exediff.bleach.DataBleach;
import com.skype.research.exediff.bleach.WeakThumbBleach;
import com.skype.research.exediff.config.cmdline.CmdLineDiffConfig;
import com.skype.util.partition.metric.Metrics;
import com.skype.util.partition.rolling.HashRollers;
import sun.misc.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * File comparison, as long as we have the diff engine anyway.
 * Amenity #1.
 */
public class Main {
	public static void main(String[] args) throws IOException {
		final PrintStream printStream = System.out;
		if (args.length == 0) {
			showHelp(printStream, "TAMIEN.txt");
			System.exit(1);
		}
		try {
			CmdLineDiffConfig configuration = new CmdLineDiffConfig(args);
			if (configuration.hasNothingToDo()) {
				showHelp(printStream, "README.txt");
				printStream.println();
				printStream.println("Error: no file specified to process!");
				System.exit(1);
			}
			runTask(configuration, printStream);
		} catch (Exception parseException) {
			parseException.printStackTrace(printStream);
			System.exit(3);
		}
	}

	private static void runTask(CmdLineDiffConfig configuration, PrintStream printStream) throws IOException {
		try (AutoClose autoClose = new AutoClose()) {
			final MultiDiff multiDiff = new MultiDiff(autoClose);
			multiDiff.setRoller(HashRollers.TAMIEN_HALFWORD);
			multiDiff.setMetric(Metrics.ShortRadialMetric);
			multiDiff.setGreedyHeal(true);
			multiDiff.setThresholds(configuration);
			if (configuration.isModuleAnalysisEnabled(ModuleAnalysis.ARM)) {
				multiDiff.addBleach(new ArmBleach());
				multiDiff.addBleach(new WeakThumbBleach());
			}
			multiDiff.addBleach(new DataBleach<>(BufferAdapter.Stateless.INT_BAD));
			multiDiff.setSummarize(false);
			multiDiff.setShowCosts(false);
			multiDiff.setDiffplay(printStream);

			PrintWriter printWriter = autoClose.register(new PrintWriter(printStream));
			multiDiff.setOriginal(configuration.getOriginal());
			for (int i = 1; i <= configuration.getModifiedCount(); ++i) {
				File modified = configuration.getFile(i); // 1-based
				multiDiff.compare(modified, printWriter);
			}
		}
	}

	private static void showHelp(PrintStream printStream, String name) throws IOException {
		printStream.write(IOUtils.readFully(Main.class.getResourceAsStream(name), Short.MAX_VALUE, false));
	}
}
