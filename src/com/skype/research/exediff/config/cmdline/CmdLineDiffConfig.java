/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.config.cmdline;

import com.skype.research.bakebread.config.ManConfig;
import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.config.cmdline.AnalyzeOptions;
import com.skype.research.bakebread.config.cmdline.UnknownOptions;
import com.skype.research.exediff.config.InFilesOptions;
import com.skype.research.exediff.config.Thresholds;
import com.skype.util.cmdline.ArgParser;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.RecognitionException;

import java.io.File;
import java.util.Collection;

/**
 * A simplified command line configuration
 * for the dedicated entry point.
 */
public class CmdLineDiffConfig implements Thresholds, ManConfig {
	private final ExeDiffOptions exeDiff = new ExeDiffOptions();
	private final AnalyzeOptions manConf = new AnalyzeOptions();
	private final InFilesOptions inFiles = new InFilesOptions();

	private static final UnknownOptions unknown = new UnknownOptions();

	public CmdLineDiffConfig(String[] cmdLine) throws RecognitionException, ConfigurationException {
		ArgParser.parseCommandLine(cmdLine,
				exeDiff,
				manConf,
				inFiles,
				unknown
		);
	}

	@Override
	public float getMaxOutlierRatio() {
		return exeDiff.getThresholds().getMaxOutlierRatio();
	}

	@Override
	public float getMinOrderedRatio() {
		return exeDiff.getThresholds().getMinOrderedRatio();
	}

	@Override
	public float getMaxHammingRatio() {
		return exeDiff.getThresholds().getMaxHammingRatio();
	}

	@Override
	public float getBitHammingRatio() {
		return exeDiff.getThresholds().getBitHammingRatio();
	}
	
	public File getOriginal() {
		return inFiles.getFile(0);
	}
	
	public int getModifiedCount() {
		return inFiles.getFileCount() - 1;
	}

	public File getFile(int index) {
		return inFiles.getFile(index);
	}

	@Override
	public boolean isModuleAnalysisEnabled(ModuleAnalysis man) {
		return manConf.isOptionSet(man);
	}

	@Override
	public Collection<ModuleAnalysis> getModuleAnalysisTypes() {
		return manConf.getOptions();
	}

	public boolean hasNothingToDo() {
		return inFiles.getFileCount() < 1;
	}
}
