/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.rolling;

import java.nio.LongBuffer;

/**
 * A rolling hash definition backed by a long[] array and a predefined {@link Rolling} type.
 */
public class ResolvedRollingHash implements RollingHash {
	private final Rolling rollingType;
	private final LongBuffer computed;

	public ResolvedRollingHash(Rolling rollingType, LongBuffer values) {
		this.rollingType = rollingType;
		this.computed = values;
		this.computed.clear().position(rollingType.getWarmUpWindowSteps());
	}

	public ResolvedRollingHash(Rolling rollingType, long[] values) {
		this(rollingType, LongBuffer.wrap(values));
	}

	@Override
	public Rolling getHashAlgorithm() {
		return rollingType;
	}

	public int getWarmUpWindowBytes() {
		return rollingType.getWarmUpWindowBytes();
	}

	public int getWarmUpWindowSteps() {
		return rollingType.getWarmUpWindowSteps();
	}

	public int getWindowSizeInBytes() {
		return rollingType.getWindowSizeInBytes();
	}

	public int getWindowSizeInSteps() {
		return rollingType.getWindowSizeInSteps();
	}

	public int getSingleStepInBytes() {
		return rollingType.getSingleStepInBytes();
	}

	@Override
	public LongBuffer computed() {
		return computed;
	}
}
