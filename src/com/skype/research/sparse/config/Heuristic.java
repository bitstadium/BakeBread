/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.config;

import com.skype.research.sparse.scanner.ScannerSpec;

/**
 * ScannerSpec parameter
 */
public enum Heuristic {
	INDEX_KBytes {
		@Override
		public void apply(ScannerSpec scannerSpec, int value) {
			scannerSpec.setForeAtMost(value);
		}
	},
	SUPER_KBytes {
		@Override
		public void apply(ScannerSpec scannerSpec, int value) {
			scannerSpec.setScanAtMost(value);
		}
	},
	THRESHOLD100 {
		@Override
		public void apply(ScannerSpec scannerSpec, int value) {
			scannerSpec.setGoodRating(value);
		}
	},
	TRIM_HEAD_AT {
		@Override
		public void apply(ScannerSpec scannerSpec, int value) {
			scannerSpec.setCutAtIndex(value);
		}
	},
	;

	public abstract void apply(ScannerSpec scannerSpec, int value);
}
