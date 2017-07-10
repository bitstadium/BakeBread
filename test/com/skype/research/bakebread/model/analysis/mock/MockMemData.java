/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis.mock;

import com.skype.research.bakebread.model.analysis.ResolvedMemData;

/**
 * A known but unreadable memory area.
 */
public class MockMemData extends ResolvedMemData {
	public MockMemData(long start, long end) {
		super(start, end, new MockMemory());
	}
}
