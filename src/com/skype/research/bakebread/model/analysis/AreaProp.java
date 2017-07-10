/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;

import java.util.Comparator;

/**
 * Properties of {@link MemArea} ranges.
 */
public enum AreaProp {
	START {
		@Override
		public long of(MemArea memArea) {
			return memArea.getStartAddress();
		}
	},
	END {
		@Override
		public long of(MemArea memArea) {
			return memArea.getEndAddress();
		}
	},
	LENGTH {
		@Override
		public long of(MemArea memArea) {
			return Areas.length(memArea);
		}
	},
	;
	public final Comparator<MemArea> inc = new Comparator<MemArea>() {
		@Override
		public int compare(MemArea left, MemArea right) {
			return Long.compare(of(left), of(right));
		}
	};

	public final Comparator<MemArea> dec = new Comparator<MemArea>() {
		@Override
		public int compare(MemArea left, MemArea right) {
			return Long.compare(of(right), of(left));
		}
	};

	public abstract long of(MemArea memArea);
}
