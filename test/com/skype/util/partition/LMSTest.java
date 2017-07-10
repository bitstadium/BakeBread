/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.BitSet;
import java.util.Random;

/**
 * Longest monotonic subsequence tests.
 */
public class LMSTest extends TestCase {

	private static int[] referenceLMS(int input[]) {
		final long SHIFT = 1L << 32;
		
		final int length = input.length;
		int better[] = new int[length];
		int behind[] = new int[length];
		int i, j, max = 0, maxPos = -1;
 
		for ( i = 0; i < length; i++ ) {
			better[i] = 1;
			behind[i] = Integer.MIN_VALUE;
		}
 
		for ( i = 1; i < length; i++ ) {
			for (j = 0; j < i; j++) {
				final int newMax = better[j] + 1;
				if ((SHIFT * input[i] + i)  > (SHIFT * input[j] + j) && newMax > better[i]) {
					better[i] = newMax;
					behind[i] = j;
					if (max < newMax) {
						max = newMax;
						maxPos = i;
					}
				}
			}
		}
 
		int[] seq = new int[max];
		while (max > 0) {
			seq[--max] = input[maxPos];
			maxPos = behind[maxPos];
		}
		return seq;
	}

	static final int CAPACITY = 256;
	static final int SHUFFLES = 1024;

	final int[] equals = new int[CAPACITY];
	final int[] unique = new int[CAPACITY];
	final int[] zeroLn = new int[0];
	final int[] outInd = new int[CAPACITY];
	final int[] outVal = new int[CAPACITY];
	final BitSet outSet = new BitSet(CAPACITY);
	
	// component being tested
	final LMS lms = new LMS(CAPACITY);
	
	// restarted random number generator
	final Random random = new Random();

	public void setUp() throws Exception {
		super.setUp();
		random.setSeed(0);
		seed(equals);
		seed(unique);
		for (int i = 0; i < SHUFFLES; i++) {
			final int thomas = random.nextInt(CAPACITY);
			final int jeremy = random.nextInt(CAPACITY);
			equals[thomas] = equals[jeremy];
			swap(unique, thomas, jeremy);
		}
	}

	private static void seed(int[] array) {
		for (int i = 0; i < array.length; i++) {
			array[i] = i + 64000;
		}
	}

	private static void swap(int[] array, int thomas, int jeremy) {
		int stored = array[thomas];
		array[thomas] = array[jeremy];
		array[jeremy] = stored;
	}

	public void testEmpty() throws Exception {
		Assert.assertEquals(0, lms.subSeq(zeroLn));
	}
	
	public void testReference() throws Exception {
		assertReferenceMonotonic(unique);
		assertReferenceMonotonic(equals);
		assertReferenceMonotonic(zeroLn);
	}

	public static void assertReferenceMonotonic(int[] input) {
		int[] reference = referenceLMS(input);
		Assert.assertTrue(reference.length <= input.length);
		assertMonotonic(reference, reference.length);
	}

	private static void assertMonotonic(int[] reference, int l1) {
		int prev = Integer.MIN_VALUE;
		int value;
		for (int i = 0; i < l1; i++) {
			value = reference[i];
			Assert.assertTrue(prev <= value);
			prev = value;
		}
	}

	public void testUnique() throws Exception {
		assertOptimizedCorrect(unique);
	}
	
	public void testEquals() throws Exception {
		// TODO break ties with offset in the lower halfword
		assertOptimizedCorrect(equals);
	}

	public void assertOptimizedCorrect(int[] input) {
		int l1 = lms.subSeq(input, outSet);
		int l2 = lms.subSeq(input, outInd, false);
		int l3 = lms.subSeq(input, outVal, true);
		// use a dumb algorithm to compute LMS the most error-prone way
		int[] reference = referenceLMS(input);
		Assert.assertEquals(reference.length, l1);
		Assert.assertEquals(reference.length, l2);
		Assert.assertEquals(reference.length, l3);
		Assert.assertEquals(reference.length, outSet.cardinality());
		for (int i = 0; i < l1; i++) {
			Assert.assertTrue(outSet.get(outInd[i]));
			Assert.assertEquals(input[outInd[i]], outVal[i]);
		}
	}
}
