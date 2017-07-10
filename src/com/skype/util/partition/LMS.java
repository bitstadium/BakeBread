/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Longest Monotonic (increasing or decreasing) Subsequence in an int[] array.
 * http://www.geeksforgeeks.org/longest-monotonically-increasing-subsequence-size-n-log-n/
 * http://www.geeksforgeeks.org/construction-of-longest-monotonically-increasing-subsequence-n-log-n/
 * Unlike the original, supports non-unique values in the input integer array.
 */
public class LMS {
	private final int capacity;
	private final int[] better;
	private final int[] behind;
	private final long[] values;

	public LMS(int capacity) {
		this.capacity = capacity;
		better = new int[capacity];
		values = new long[capacity];
		behind = new int[capacity];
	}

	public final int subSeq(int[] input, int[] outArr, boolean values) {
		return presentAs(input, outArr, values, subSeq(input));
	}

	public int presentAs(int[] input, int[] outArr, boolean values, int maxLen) {
		int trgIndex = maxLen - 1;
		int srcIndex = better[trgIndex];
		while (srcIndex >= 0) {
			outArr[trgIndex--] = values ? input[srcIndex] : srcIndex;
			srcIndex = behind[srcIndex];
		}
		return maxLen;
	}

	// bit-setting values makes very little sense
	public final int subSeq(int[] input, BitSet outSet) {
		return presentAs(outSet, subSeq(input));
	}

	public int presentAs(BitSet outSet, int maxLen) {
		outSet.clear();
		int trgIndex = maxLen - 1;
		int srcIndex = better[trgIndex];
		while (srcIndex >= 0) {
			outSet.set(srcIndex);
			srcIndex = behind[srcIndex];
		}
		return maxLen;
	}
	
	public int subSeq(int[] input) {
		final int length = input.length;
		if (length == 0) {
			return 0;
		}
		if (length > capacity) {
			throw new IllegalArgumentException("Capacity " + capacity + " exceeded");
		}
		for (int index = 0; index < length; ++index) {
			better[index] = 0;
			behind[index] = Integer.MIN_VALUE;
		}
		values[0] = (long) input[0] << 32;
		int addIndex = 0;
		int insFloor;
		long value;
		for (int index = 1; index < length; ++index) {
			value = ((long) input[index] << 32) + index;
			if (values[0] > value) {
				better[0] = index;
				values[0] = value;
			} else if (values[addIndex] < value) {
				behind[index] = better[addIndex++];
				better[addIndex] = index;
				values[addIndex] = value;
			} else {
				insFloor = ~Arrays.binarySearch(values, 0, addIndex + 1, value) - 1;
				assert insFloor >= 0;
				behind[index] = better[insFloor++];
				better[insFloor] = index;
				values[insFloor] = value;
			}
		}
		return addIndex + 1;
	}
}
