/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.exediff.config.MutableThresholds;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Default configuration is "Display all, write nothing".
 */
public class DefaultConfiguration extends MutableThresholds implements Configuration {
	private final File file;

	public DefaultConfiguration(File file) {
		this.file = file;
	}

	@Override
	public boolean hasNothingToDo() {
		return false;
	}

	@Override
	public boolean isDisplaySectionEnabled(DisplaySection section) {
		return true;
	}

	@Override
	public boolean shouldConvertTo(Conversion conversionType) {
		return false;
	}

	@Override
	public File getConversionTarget(Conversion conversionType) {
		return null;
	}

	@Override
	public Collection<File> getModulePaths() {
		return Collections.emptyList();
	}

	@Override
	public Collection<String> getUndefinedPathElements() {
		return Collections.emptyList();
	}

	@Override
	public boolean isModuleAnalysisEnabled(ModuleAnalysis man) {
		return false;
	}

	@Override
	public Collection<ModuleAnalysis> getModuleAnalysisTypes() {
		return Collections.emptyList();
	}

	@Override
	public boolean isValidationTypeEnabled(BitExactValidation val) {
		// dumps are usually ok but firmware matching is error-prone
		return val != BitExactValidation.DUMP_INTERNAL; /// paranoid
	}

	@Override
	public boolean hasMemoryFillingPattern(MemoryFog fogType) {
		return true;
	}

	@Override
	public byte[] getMemoryFillingPattern(MemoryFog fogType) {
		return new byte[] { 0 };
	}

	@Override
	public File getDumpFile() {
		return file;
	}

	@Override
	public boolean shallWrite(MemLoad memLoad) {
		// reasonably large (all meaningful data in), however not ridiculously bloated
		return CoreDumpLoadType.SURE.shallWrite(memLoad);
	}
}
