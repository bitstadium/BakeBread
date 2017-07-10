/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.nio.ByteMemory;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

/**
 * A test suite for the memory analysis package.
 */
public class AreasTest extends TestCase {

	private static final int P_UNDER = 750;
	private static final int M_UNDER = 960;

	private static final int L_BOUND = 1065;

	private static final int LIES_IN = 2031;
	private static final int MORE_IN = 2048;

	private static final int U_BOUND = 3210;
	
	private static final int P_ABOVE = 4096;
	private static final int M_ABOVE = 8192;

	final MemArea memArea = new ResolvedMemArea(L_BOUND, U_BOUND);
	final MemArea around0 = new ResolvedMemArea(0, 0);

	public void testLength() throws Exception {
		assertEquals(U_BOUND - L_BOUND, Areas.length(memArea));
		assertFalse(Areas.isEmpty(memArea));
		assertTrue(Areas.isEmpty(around0));
	}
	
	enum IsInArg {
		a(P_UNDER, true, false),
		b(P_UNDER, false, false),
		c(L_BOUND, true, true),
		d(L_BOUND, false, true),
		e(LIES_IN, true, true),
		f(LIES_IN, false, true),
		g(U_BOUND, true, true),
		h(U_BOUND, false, false),
		i(P_ABOVE, true, false),
		j(P_ABOVE, false, false),
		;
		final int address;
		final boolean isEndAddress;
		final boolean success;

		IsInArg(int address, boolean isEndAddress, boolean success) {
			this.address = address;
			this.isEndAddress = isEndAddress;
			this.success = success;
		}
	}

	public void testIsIn() throws Exception {
		for (IsInArg isInArg : IsInArg.values()) {
			assertEquals(isInArg.success, Areas.isIn(isInArg.address, isInArg.isEndAddress, memArea));
		}
	}
	
	enum TrimArg implements MemArea {
		around(P_UNDER, P_ABOVE, L_BOUND, U_BOUND),
		inside(LIES_IN, MORE_IN, LIES_IN, MORE_IN),
		;
		final MemArea result;
		final long otherStart;
		final long otherEnd;

		TrimArg(long otherStart, long otherEnd, long resultStart, long resultEnd) {
			this.otherStart = otherStart;
			this.otherEnd = otherEnd;
			result = new ResolvedMemArea(resultStart, resultEnd);
		}


		@Override
		public long getStartAddress() {
			return otherStart;
		}

		@Override
		public long getEndAddress() {
			return otherEnd;
		}
	}
	
	enum TrimOut implements MemArea {
		below(P_UNDER, M_UNDER),
		above(P_ABOVE, M_ABOVE),
		;

		final long startAddress;
		final long endAddress;

		TrimOut(long startAddress, long endAddress) {
			this.startAddress = startAddress;
			this.endAddress = endAddress;
		}

		@Override
		public long getStartAddress() {
			return startAddress;
		}

		@Override
		public long getEndAddress() {
			return endAddress;
		}
	}
	
	enum TrimDir {
		LTR {
			@Override
			MemArea trim(MemArea left, MemArea right) {
				return Areas.trim(left, right);
			}
		},
		RTL {
			@Override
			MemArea trim(MemArea left, MemArea right) {
				return Areas.trim(right, left);
			}
		},
		;
		abstract MemArea trim(MemArea left, MemArea right);
	}
	
	public void testTrimArea() throws Exception {
		final MemArea EMPTY = new ResolvedMemArea(0, 0);
		
		for (TrimDir trimDir : TrimDir.values()) {
			for (TrimArg trimArg : TrimArg.values()) {
				assertTrue(Areas.areasEqual(trimArg.result, trimDir.trim(trimArg, memArea)));
			}

			for (TrimOut trimOut : TrimOut.values()) {
				assertEquals(EMPTY, trimDir.trim(trimOut, memArea));
			}
		}
	}
	
	public void testTrimData() throws Exception {
		for (TrimArg trimArg : TrimArg.values()) {
			int length = (int) Areas.length(trimArg);
			byte[] ba = new byte[length];
			for (int i = 0; i < ba.length; i++) {
				ba[i] = (byte) i;
			}
			ByteBuffer buf = ByteBuffer.wrap(ba);
			MemData memData = new ResolvedMemData(trimArg, new ByteMemory(buf));
			// the very test
			MemData trimmed = memData.trimTo(memArea);
			// result validation
			long delta = trimArg.result.getStartAddress() - trimArg.getStartAddress();
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			trimmed.getData().writeTo(os);
			byte[] bytes = os.toByteArray();
			Assert.assertEquals(Areas.length(trimmed), bytes.length);
			for (int pos = 0; pos < bytes.length; ++pos) {
				Assert.assertEquals((byte) (pos + delta), bytes[pos]);
			}
		}
	}
	
	public void testIsContiguous() throws Exception {
		Assert.assertTrue(Areas.isNonContiguous(Arrays.asList(around0, memArea)));
		Assert.assertFalse(Areas.isNonContiguous(Arrays.asList(new ResolvedMemArea(0, memArea.getStartAddress()), memArea)));
		Assert.assertFalse(Areas.isNonContiguous(Arrays.asList(new ResolvedMemArea(0, memArea.getEndAddress()), memArea)));
		Assert.assertFalse(Areas.isNonContiguous(Collections.singletonList(memArea)));
		Assert.assertFalse(Areas.isNonContiguous(Collections.<MemArea>emptyList()));
	}

	public void testSubtract() throws Exception {
		Assert.assertTrue (Areas.areasEqual(memArea, Areas.subtract(memArea, TrimOut.above)));
		Assert.assertTrue (Areas.areasEqual(memArea, Areas.subtract(memArea, TrimOut.below)));
		Assert.assertFalse(Areas.areasEqual(around0, Areas.subtract(memArea, TrimOut.below)));
		MemArea lowerHalf = new ResolvedMemArea(L_BOUND, LIES_IN);
		MemArea upperHalf = new ResolvedMemArea(LIES_IN, U_BOUND);
		MemArea lowerMore = new ResolvedMemArea(P_UNDER, LIES_IN);
		MemArea upperMore = new ResolvedMemArea(LIES_IN, P_ABOVE);
		Assert.assertTrue (Areas.areasEqual(lowerHalf, Areas.subtract(memArea, upperHalf)));
		Assert.assertTrue (Areas.areasEqual(upperHalf, Areas.subtract(memArea, lowerHalf)));
		Assert.assertTrue (Areas.areasEqual(lowerHalf, Areas.subtract(memArea, upperMore)));
		Assert.assertTrue (Areas.areasEqual(upperHalf, Areas.subtract(memArea, lowerMore)));
		Assert.assertTrue (Areas.areasEqual(around0, Areas.subtract(around0, around0)));
		Assert.assertFalse(Areas.areasEqual(memArea, Areas.subtract(around0, around0)));
	}
	
	// subtract
}
