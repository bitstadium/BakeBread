/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.scanner;

/**
 * Scanning parameters. POJO.
 */
public class ScannerSpec implements ScannerConf {

	private int foreAtMost = 16;
	private int scanAtMost = 1024;
	private int goodRating = 10;
	private int cutAtIndex = 0;

	@Override
	public int getForeAtMost() {
		return foreAtMost;
	}

	@Override
	public int getScanAtMost() {
		return scanAtMost;
	}

	@Override
	public int getGoodRating() {
		return goodRating;
	}

	@Override
	public int getCutAtIndex() {
		return cutAtIndex;
	}
	
	public void setForeAtMost(int foreAtMost) {
		this.foreAtMost = foreAtMost;
	}

	public void setScanAtMost(int scanAtMost) {
		this.scanAtMost = scanAtMost;
	}

	public void setGoodRating(int goodRating) {
		this.goodRating = goodRating;
	}

	public void setCutAtIndex(int cutAtIndex) {
		this.cutAtIndex = cutAtIndex;
	}
}
