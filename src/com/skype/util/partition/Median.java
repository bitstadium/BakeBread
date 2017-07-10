/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * MedianOfMedians:
 * https://en.wikipedia.org/wiki/Selection_algorithm
 * https://en.wikipedia.org/wiki/Median_of_medians
 * http://people.csail.mit.edu/rivest/pubs/BFPRT73.pdf
 * https://www.cs.rit.edu/~ib/Classes/CS515_Spring12-13/Slides/022-SelectMasterThm.pdf
 * http://cs.stackexchange.com/questions/1914/to-find-the-median-of-an-unsorted-array
 * 
 * If that's too complicated, feel free to replace with Quickselect.
 */
public class Median {

	static final int MINI_GROUP = 5;

	public static int simpleMedian(final int[] array, final int beginIndex, final int endIndex) {
		return array[simpleIndex(array, beginIndex, endIndex)];
	}

	public static int simpleIndex(int[] array, int beginIndex, int endIndex) {
		return simpleIndex(array, beginIndex, endIndex, (beginIndex + endIndex) >> 1);
	}
	
	public static int simpleIndex(int[] array, int beginIndex, int endIndex, int nth) {
		Arrays.sort(array, beginIndex, endIndex);
		return nth;
	}

	public static int linearMedian(final int[] array, final int beginIndex, final int endIndex) {
		return array[linearIndex(array, beginIndex, endIndex)];
	}

	public static int linearIndex(final int[] array, int beginIndex, int endIndex) {
		return linearIndex(array, beginIndex, endIndex, (beginIndex + endIndex) >> 1);
	}
	
	static int linearIndex(final int[] array, int beginIndex, int endIndex, final int nth) {
		while (true) {
			if (endIndex <= beginIndex) {
				throw new NoSuchElementException(beginIndex + ">=" + endIndex);
			} else if (endIndex - beginIndex == 1) {
				return beginIndex;
			} else if (endIndex - beginIndex <= MINI_GROUP) {
				return simpleIndex(array, beginIndex, endIndex, nth);
			}
			int pivotIndex = medianOfMedians(array, beginIndex, endIndex);
			pivotIndex = partition(array, beginIndex, endIndex, pivotIndex);
			// WISDOM at this point, the array is partially sorted:
			// WISDOM - all elements below pivotIndex <= pivotValue 
			// WISDOM - all elements above pivotIndex >= pivotValue
			
			int pivotValue = array[pivotIndex];
			if (array[nth] == pivotValue) {
				return nth;
			} else if (array[nth] < pivotValue) {
				do endIndex = pivotIndex; while (pivotIndex > nth && array[--pivotIndex] == pivotValue);
			} else {
				do beginIndex = pivotIndex; while (pivotIndex < nth && array[++pivotIndex] == pivotValue);
			}
		}
	}

	static int partition(final int[] array, final int beginIndex, final int endIndex, final int pivotIndex) {
		// http://algs4.cs.princeton.edu/23quicksort/ -> Dijkstra
		final int pivotValue = array[pivotIndex];
		int leftEdge = beginIndex, index = beginIndex, rightEdge = endIndex - 1;
		while (index <= rightEdge) {
			int value = array[index];
			if (value < pivotValue) {
				exchange(array, leftEdge++, index++);
			} else if (value > pivotValue) {
				exchange(array, index, rightEdge--);
			} else {
				index++;
			}
		}
		
		return leftEdge;
	}

	static int medianOfMedians(final int array[], final int beginIndex, final int endIndex) {
		// move the medians of five-element subgroups to the first n/5 positions
		int groupEndIndex;
		int groupBeginIndex = beginIndex, partialMedians = beginIndex;
		while (groupBeginIndex < endIndex) {
			// get the median of the i'th five-element subgroup
			groupEndIndex = groupBeginIndex + MINI_GROUP;
			if (groupEndIndex > endIndex) {
				groupEndIndex = endIndex;
			}

			final int groupMedian = simpleIndex(array, groupBeginIndex, groupEndIndex);
			exchange(array, groupMedian, partialMedians++);
			groupBeginIndex = groupEndIndex;
		}
		// compute the median of the n/5 medians-of-five
		return linearIndex(array, beginIndex, partialMedians);
	}

	static void exchange(final int[] array, final int one, final int two) {
		if (one != two) {
			int value = array[two];
			array[two] = array[one];
			array[one] = value;
		}
	}
}
