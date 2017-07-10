/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;
import junit.framework.Assert;

import java.util.Iterator;
import java.util.Map;

/**
 * Verification of intervals.
 */
public class AreaTestUtil {
	private AreaTestUtil() {}

	public static void assertEquals(long[][] expected, Map<? extends MemArea, ?> actual) {
		assertEquals(null, expected, actual);
	}
	
	public static void assertEquals(String message, long[][] expected, Map<? extends MemArea, ?> actual) {
		assertEquals(message, expected, actual.keySet());
	}

	public static void assertEquals(long[][] expected, Iterable<? extends MemArea> areas) {
		assertEquals(null, expected, areas);
	}
	
	public static void assertEquals(String message, long[][] expected, Iterable<? extends MemArea> areas) {
		Iterator<? extends MemArea> inRange = areas.iterator();
		for (long[] area : expected) {
			MemArea result = inRange.next();
			Assert.assertEquals(message, area[0], result.getStartAddress());
			Assert.assertEquals(message, area[1], result.getEndAddress());
		}
		Assert.assertFalse("That's it!", inRange.hasNext());
	}
}
