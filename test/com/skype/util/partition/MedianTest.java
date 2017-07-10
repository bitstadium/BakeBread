/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Random;

/**
 * Linear median tests.
 */
public class MedianTest extends TestCase {
	
	private final Random random = new Random();

	public void setUp() throws Exception {
		super.setUp();
		random.setSeed(0);
	}

	public void testExchange() throws Exception {
		int[] i = new int[] { 5, 10, 15 };
		Median.exchange(i, 0, 2);
		Assert.assertEquals(i[0], 15);
		Assert.assertEquals(i[1], 10);
		Assert.assertEquals(i[2], 5);
	}
	
	public void testPartition() throws Exception {
		for (int i = 0; i < 256; ++i) {
			int length = random.nextInt(1024) + 1; // can't be 0
			int[] arr = new int[length];
			for (int j = 0; j < length; ++j) {
				arr[j] = random.nextInt(1024);
			}
			for (int q = 0; q < 256; ++q) {
				arr[random.nextInt(length)] = arr[random.nextInt(length)];
			}
			int pivotIndex = random.nextInt(length);
			pivotIndex = Median.partition(arr, 0, length, pivotIndex);

			final int beginIndex = 0;
			//noinspection UnnecessaryLocalVariable
			final int endIndex = length;
			final int pivotValue = arr[pivotIndex];

			boolean wasEqual = false, wasGreater = false;
			for (int index = beginIndex; index < endIndex; ++index) {
				if (arr[index] == pivotValue) {
					wasEqual = true;
				}
				if (wasEqual == arr[index] < pivotValue)
					throw new IllegalStateException("<");
				if (index == pivotIndex && !(arr[index] == pivotValue))
					throw new IllegalStateException("=");
				if (arr[index] > pivotValue) {
					wasGreater = true;
				}
				if (wasGreater == arr[index] <= pivotValue) {
					throw new IllegalStateException(">");
				}
			}

			if (arr[pivotIndex] != pivotValue) {
				throw new IllegalStateException("pivot not captured");
			}
		}
	}

	static class Check {
		int sum = 0, or = 0, xor = 0;

		private void aggregate(int[] arr) {
			for (int rel : arr) {
				this.sum += rel;
				this.or  |= rel;
				this.xor ^= rel;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Check)) return false;

			Check check = (Check) o;

			return sum == check.sum && or == check.or && xor == check.xor;

		}

		@Override
		public int hashCode() {
			int result = sum;
			result = 31 * result + or;
			result = 31 * result + xor;
			return result;
		}
	}
	
	enum Distribution {
		AS_IT_GOES, REPETITIVE, UNIQUE
	}
	
	public void testReferenceAsItGoes() throws Exception {
		testReference(Distribution.AS_IT_GOES);
	}
	
	public void testReferenceRepetitive() throws Exception {
		testReference(Distribution.REPETITIVE);
	}
	
	public void testReferenceUnique() throws Exception {
		testReference(Distribution.UNIQUE);
	}
	
	public void testReference(Distribution distribution) throws Exception {
		for (int i = 0; i < 512; ++i) {
			int length = random.nextInt(1024) + 1; // can't be 0
			int[] arr = new int[length];
			switch (distribution) {
				case AS_IT_GOES: {
					for (int j = 0; j < length; ++j) {
						arr[j] = random.nextInt(1024);
					}
					break;
				}
				case UNIQUE: {
					// 1/ deliberately unique
					for (int j = 0; j < length; ++j) {
						arr[j] = (random.nextInt(1024) << 10) + j;
					}
					break;
				}
				case REPETITIVE: {
					// 2/ deliberately repetitive
					for (int j = 0; j < length; ++j) {
						arr[j] = random.nextInt(1024);
					}
					for (int j = 0; j < length; ++j) {
						arr[random.nextInt(length)] = arr[random.nextInt(length)];
					}
					break;
				}
			}
			int[] ref = Arrays.copyOf(arr, length);
			int[] boo = new int[2560];
			int start = random.nextInt(512);
			System.arraycopy(arr, 0, boo, start, length);
			Check check = new Check();
			check.aggregate(arr);
			// go!
			int simpleInd = Median.simpleIndex(ref, 0, length);
			int simpleMed = ref[simpleInd];
			assertMidElementCaptured("reference", ref, simpleMed);

			int linearInd = Median.linearIndex(arr, 0, length);
			int linearMed = arr[linearInd];

			int offsetInd = Median.linearIndex(boo, start, start + length);
			Assert.assertEquals(linearInd + start, offsetInd);

			Check swap = new Check();
			swap.aggregate(arr);
			// contents not damaged by reordering
			Assert.assertEquals(check, swap);
			// external contents not damaged
			for (int b = 0; b < start; ++b) {
				Assert.assertEquals(0, boo[b]);
			}
			for (int b = start + length; b < boo.length; ++b) {
				Assert.assertEquals(0, boo[b]);
			}

			// now that contents are intact, check the returned median
			assertMidElementCaptured("linearMed", arr, linearMed);
		}
	}

	private void assertMidElementCaptured(String message, int[] arr, int med) {
		int length = arr.length;
		int half = (length + 1) >> 1;
		int less = 0, over = 0;
		for (int rel : arr) {
			if (rel > med) {
				over ++;
			} else if (rel < med) {
				less ++;
			}
		}
		Assert.assertTrue(String.format("%s: %d[less] <= %d in %d", message, less, half, length), less <= half);
		Assert.assertTrue(String.format("%s: %d[over] <= %d in %d", message, over, half, length), over <= half);
	}

	private String of(int[] arr, int ind) {
		return "arr["+ind+"]=" + arr[ind];
	}

	public void testTrivialMedian() {
		assertEquals(0, Median.linearMedian(new int[7150], 0, 7150));
	}

	public void testLoneTreeMedian() {
		final int[] array = new int[7150];
		array[715] = 1;
		assertEquals(0, Median.linearMedian(array, 0, array.length));
	}

	public void testThreeTreesMedian() {
		final int[] array = new int[7150];
		array[15] = 1;
		array[16] = 1;
		array[91] = 1;
		assertEquals(0, Median.linearMedian(array, 0, array.length));
	}
}
