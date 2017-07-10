/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.scanner;

/**
 * Scanning parameters.
 */
public interface ScannerConf {
	int getForeAtMost();
	int getScanAtMost();
	int getGoodRating();
	int getCutAtIndex();
}
