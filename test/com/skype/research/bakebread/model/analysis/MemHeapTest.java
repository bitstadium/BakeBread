/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * Test, {@link MemHeap}, the no-overlap memory map.
 */
public class MemHeapTest extends TestCase {

	final MemHeap<MemArea> memHeap = new MemHeap<>();

	public void testOrdering() throws Exception {
		// with an overlapping range
		assertComparison(0,
				new ResolvedMemArea(1200, 1300), 
				new ResolvedMemArea(1200, 1300));
		assertComparison(0,
				new ResolvedMemArea(1000, 1300),
				new ResolvedMemArea(1200, 1500));
		assertComparison(0,
				new ResolvedMemArea(1200, 1300),
				new ResolvedMemArea(1000, 1500));
		
		// with an inner point
		assertComparison(0,
				new ResolvedMemArea(1270, 1270),
				new ResolvedMemArea(1000, 1500));

		// with a range of nonzero length
		assertComparison(-1,
				new ResolvedMemArea(1000, 1200),
				new ResolvedMemArea(1200, 1500));
		assertComparison(-1,
				new ResolvedMemArea(1000, 1200),
				new ResolvedMemArea(1300, 1500));
		
		// with a point
		assertComparison(-1,
				new ResolvedMemArea(1000, 1200),
				new ResolvedMemArea(1200, 1200));
		assertComparison(-1,
				new ResolvedMemArea(1000, 1200),
				new ResolvedMemArea(1300, 1300));

	}

	public void assertComparison(int expected, ResolvedMemArea left, ResolvedMemArea right) {
		Assert.assertEquals(expected,
				MemHeap.stickyMemCmp.compare(
						left,
						right));
		Assert.assertEquals(-expected,
				MemHeap.stickyMemCmp.compare(
						right,
						left));
	}

	public void testPopulate() throws Exception {
		memHeap.add(new ResolvedMemArea(3000, 4000));
		memHeap.add(new ResolvedMemArea(1000, 2000));
		Iterator<MemArea> keys = memHeap.keySet().iterator();
		Assert.assertEquals(new ResolvedMemArea(1000, 2000), keys.next());
		Assert.assertEquals(new ResolvedMemArea(3000, 4000), keys.next());
		Assert.assertFalse(keys.hasNext());
		try {
			memHeap.add(new ResolvedMemArea(1500, 3500));
			assertFalse(true);
		} catch (IllegalStateException ignored) {
			// must have been thrown
		}
		try {
			memHeap.add(new ResolvedMemArea(500, 5500));
			assertFalse(true);
		} catch (IllegalStateException ignored) {
			// must have been thrown: already present
		}
	}
	
	public void testIdentify() throws Exception {
		memHeap.add(new ResolvedMemArea(3000, 4000));
		memHeap.add(new ResolvedMemArea(1000, 2000));
		Collection<MemArea> snapshot = new LinkedList<>(memHeap.keySet());
		Collection<MemArea> snapData = new LinkedList<>(memHeap.values());
		Assert.assertEquals(snapshot, snapData);
		Assert.assertFalse(new ResolvedMemArea(3000, 4000).equals(memHeap.map(1500)));
		Assert.assertEquals(new ResolvedMemArea(1000, 2000), memHeap.map(1500));
		try {
			memHeap.map(0);
			Assert.assertTrue(false);
			
		} catch (NoSuchElementException ignored) {
			// must have been thrown: unmapped
		}
		try {
			memHeap.map(1999999999L);
			Assert.assertTrue(false);
			
		} catch (NoSuchElementException ignored) {
			// must have been thrown: unmapped
		}
		Collection<MemArea> snapNext = new LinkedList<>(memHeap.keySet());
		Assert.assertEquals(snapshot, snapNext);
	}
	
	public void testSubrange() throws Exception {
		memHeap.add(new ResolvedMemArea(1000, 1500));
		memHeap.add(new ResolvedMemArea(3000, 4000));
		memHeap.add(new ResolvedMemArea(4200, 4500));
		memHeap.add(new ResolvedMemArea(1500, 1700));
		AreaTestUtil.assertEquals(new long[][]{{1000, 1500}, {1500, 1700}},
				memHeap.subMap(1200, 1600, false, false, false));
		try {
			memHeap.subMap(200, 2600, false, false, false);
			assertFalse(true);
		} catch (NoSuchElementException ignored) {
			// must have been thrown: loose
		}
		AreaTestUtil.assertEquals(new long[][]{{1000, 1500}, {1500, 1700}},
				memHeap.subMap(200, 2600, true, false, false));
		AreaTestUtil.assertEquals(new long[][]{{3000, 4000}, {4200, 4500}},
				memHeap.subMap(2000, 4500, true, false, false));
		try {
			memHeap.subMap(2000, 4500, true, false, true);
		} catch (NoSuchElementException ignored) {
			// must have been thrown: contiguous
		}
		try {
			memHeap.subMap(8000, 9500, true, false, false);
		} catch (NoSuchElementException ignored) {
			// must have been thrown: empty
		}
		AreaTestUtil.assertEquals(new long[][] {}, memHeap.subMap(8000, 9500, true, true, false));
		AreaTestUtil.assertEquals(new long[][] {}, memHeap.subMap(8000, 9500, true, true, true));
		// MOREINFO can we allow empty but forbid loose? should we, for any reason?
	}

	public void testSubtract() throws Exception {
		memHeap.add(new ResolvedMemArea(3000, 4000));
		memHeap.add(new ResolvedMemArea(1000, 1500));
		memHeap.add(new ResolvedMemArea(1500, 2000));
		// MemArea memArea =;
		Assert.assertEquals(new ResolvedMemArea(2300, 2700),
			memHeap.trimToUnique(new ResolvedMemArea(2300, 2700)));
		Assert.assertEquals(new ResolvedMemArea(2000, 3000),
			memHeap.trimToUnique(new ResolvedMemArea(1500, 3000)));
		Assert.assertEquals(new ResolvedMemArea(2000, 3000),
			memHeap.trimToUnique(new ResolvedMemArea(1200, 3000)));
		try {
			memHeap.trimToUnique(new ResolvedMemArea(500,  4000));
			Assert.assertFalse(true);
		} catch (IllegalArgumentException ignored) {
			// must have been thrown: non-contiguous difference
		}
		try {
			memHeap.trimToUnique(new ResolvedMemArea(1900, 4200));
		} catch (IllegalArgumentException ignored) {
			// must have been thrown: non-contiguous difference
		}
		Assert.assertEquals(new ResolvedMemArea(2000, 2900),
			memHeap.trimToUnique(new ResolvedMemArea(1900, 2900)));
		Assert.assertEquals(new ResolvedMemArea(2000, 2900),
			memHeap.trimToUnique(new ResolvedMemArea(2000, 2900)));
		Assert.assertEquals(new ResolvedMemArea(2100, 3000),
			memHeap.trimToUnique(new ResolvedMemArea(2100, 3100)));
		Assert.assertEquals(new ResolvedMemArea(2100, 3000),
			memHeap.trimToUnique(new ResolvedMemArea(2100, 3000)));
	}

	public void tearDown() throws Exception {
		memHeap.clear();
		super.tearDown();
	}
}
