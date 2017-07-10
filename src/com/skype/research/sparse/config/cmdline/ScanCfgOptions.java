/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.config.cmdline;

import com.skype.research.sparse.config.Heuristic;
import com.skype.research.sparse.scanner.ScannerConf;
import com.skype.research.sparse.scanner.ScannerSpec;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.OpenEnumOptions;
import com.skype.util.cmdline.RecognitionException;

/**
 * Scanning heuristic.
 */
public class ScanCfgOptions extends OpenEnumOptions<Heuristic> {

	private final ScannerSpec scannerSpec = new ScannerSpec();

	public ScanCfgOptions() {
		super('S', "scan", Heuristic.class, null);
	}
	
	@Override
	public Heuristic recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'c': return Heuristic.TRIM_HEAD_AT;
			case 't': return Heuristic.THRESHOLD100;
			case 'i': return Heuristic.INDEX_KBytes;
			case 's': return Heuristic.SUPER_KBytes;
			default:
				return null;
		}
	}

	@Override
	public Heuristic recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "cut": return Heuristic.TRIM_HEAD_AT;
			case "threshold": return Heuristic.THRESHOLD100;
			case "index": return Heuristic.INDEX_KBytes;
			case "super": return Heuristic.SUPER_KBytes;
			default:
				return null;
		}
	}

	@Override
	protected void onValueSet(Heuristic key, String value) throws ConfigurationException {
		key.apply(scannerSpec, Integer.parseInt(value));
	}

	public ScannerConf getConf() {
		return scannerSpec;
	}
}
