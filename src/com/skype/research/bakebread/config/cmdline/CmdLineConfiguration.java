/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.BitExactValidation;
import com.skype.research.bakebread.config.Configuration;
import com.skype.research.bakebread.config.Conversion;
import com.skype.research.bakebread.config.DisplaySection;
import com.skype.research.bakebread.config.MemoryFog;
import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.exediff.config.cmdline.ExeDiffOptions;
import com.skype.util.cmdline.ArgParser;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.RecognitionException;

import java.io.File;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Operation config.
 */
public class CmdLineConfiguration implements Configuration {

	private final DisplayOptions display = new DisplayOptions();
	private final ExeFileOptions exeFile = new ExeFileOptions();
	private final AnalyzeOptions mapping = new AnalyzeOptions();
	private final FillingOptions filling = new FillingOptions();
	private final CompareOptions compare = new CompareOptions();
	private final CoreOutOptions outConf = new CoreOutOptions();
	private final ConvertOptions convert = new ConvertOptions();
	private final ExeDiffOptions exeDiff = new ExeDiffOptions();
    private final DmpFileOptions dmpFile = new DmpFileOptions();
    
	private static final UnknownOptions unknown = new UnknownOptions();

	public CmdLineConfiguration(String[] cmdLine) throws RecognitionException, ConfigurationException {
	    ArgParser.parseCommandLine(cmdLine,
			    display,
			    convert,
			    exeFile,
			    mapping,
			    filling,
			    compare,
			    outConf,
			    exeDiff,
			    dmpFile, // must be second last
			    unknown  // must be last
	    );
	    // no validation for completeness. when we fail, we know it.
    }

	@Override
    public File getDumpFile() {
        return dmpFile.getFile();
    }

	@Override
	public boolean hasNothingToDo() {
		return display.getOptions().isEmpty() && convert.getOptions().isEmpty() || dmpFile.getFile() == null;
	}

	@Override
	public boolean isDisplaySectionEnabled(DisplaySection section) {
		return display.isOptionSet(section);
	}
	
	@Override
	public boolean isModuleAnalysisEnabled(ModuleAnalysis manType) {
		return mapping.isOptionSet(manType);
	}

	@Override
	public Collection<ModuleAnalysis> getModuleAnalysisTypes() {
		// complain if undefined. already protected against multiple.
		return mapping.getOptions();
	}
	
	@Override
	public boolean isValidationTypeEnabled(BitExactValidation val) {
		return compare.isOptionSet(val);
	}

	@Override
	public boolean hasMemoryFillingPattern(MemoryFog fogType) {
		return filling.getFilling(fogType) != null;
	}
	
	@Override
	public byte[] getMemoryFillingPattern(MemoryFog fogType) {
		byte[] filling = this.filling.getFilling(fogType);
		if (filling == null) {
			throw new NoSuchElementException(fogType.name());
		}
		return filling;
	}
	
	@Override
	public Collection<File> getModulePaths() {
		return exeFile.getFolderSet();
	}
	
	@Override
	public Collection<String> getUndefinedPathElements() {
		return exeFile.getUndefinedElements();
	}
	
	@Override
	public boolean shouldConvertTo(Conversion conversionType) {
		return convert.isValueSet(conversionType);
	}
	
	@Override
	public File getConversionTarget(Conversion conversionType) {
		return convert.getTargetFile(conversionType);
	}

	@Override
	public boolean shallWrite(MemLoad memLoad) {
		return outConf.shallWrite(memLoad);
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
}
