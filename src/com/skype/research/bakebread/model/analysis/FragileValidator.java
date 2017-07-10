/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemLoad;

import java.util.InputMismatchException;

public class FragileValidator implements Validator {

	@Override
	public void compare(MemLoad modified, MemLoad original) {
		// WISDOM the linker may have modified .data.rel.ro, so skip it
		// WISDOM ModuleAnalysisType.ELF => relink .data.rel.ro to real
		// WISDOM http://www.airs.com/blog/archives/189 .text & .rodata
		if (!modified.getMapInfo().isRunnable()) {
			return; // until relro
		}
		if (!Areas.bytesEqual(modified, original)) {
			////// TODO integrate DiffMatch here
			throw new InputMismatchException(modified + " != " + original);
		}
	}
}