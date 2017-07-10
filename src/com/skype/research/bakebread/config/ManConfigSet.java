/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

/**
 * A simple implementation of {@link ManConfig} in the form of an {@link EnumSet}.
 */
public class ManConfigSet implements ManConfig {
	private final EnumSet<ModuleAnalysis> manCfg;
	private final Collection<ModuleAnalysis> roManCfg;
	
	public ManConfigSet(EnumSet<ModuleAnalysis> manCfg) {
		this.manCfg = manCfg;
		roManCfg = Collections.unmodifiableCollection(this.manCfg);
	}

	@Override
	public boolean isModuleAnalysisEnabled(ModuleAnalysis man) {
		return manCfg.contains(man);
	}

	@Override
	public Collection<ModuleAnalysis> getModuleAnalysisTypes() {
		return roManCfg;
	}
}
