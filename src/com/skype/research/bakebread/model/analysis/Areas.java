/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.bakebread.nio.Memory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link MemArea} helper methods.
 */
public class Areas {
	private Areas() {}

	public static void validate(MemArea memArea) {
		final long startAddress = memArea.getStartAddress();
		if (startAddress < 0) {
			throw new IllegalArgumentException("Addresses " +
					longToHex(startAddress) +
					" must be unsigned long values");
		}
		final long endAddress = memArea.getEndAddress();
		if (startAddress > endAddress) {
			throw new IllegalArgumentException("Start address "
					+ longToHex(startAddress)
					+ " > end address "
					+ longToHex(endAddress));
		}
	}

	public static String longToHex(long address) {
		return "[" + address + " == 0x" + Long.toHexString(address) + "]";
	}

	public static long length(MemArea memArea) {
		return memArea.getEndAddress() - memArea.getStartAddress();
	}

	public static boolean isEmpty(MemArea memArea) {
		return memArea.getStartAddress() == memArea.getEndAddress();
	}

	public static MemArea trim(MemArea area, long toLength) {
		if (toLength >= length(area)) {
			return area;
		}
		return new ResolvedMemArea(area.getStartAddress(), area.getStartAddress() + toLength);
	}
	
	public static MemArea trim(MemArea area, MemArea toRange) {
		long startAddress = Math.max(area.getStartAddress(), toRange.getStartAddress());
		long endAddress = Math.min(area.getEndAddress(), toRange.getEndAddress());
		if (area.getStartAddress() == startAddress && area.getEndAddress() == endAddress) {
			return area;
		}
		if (endAddress < startAddress) {
			endAddress = startAddress = 0;
		}
		return new ResolvedMemArea(startAddress, endAddress);
	}

	public static boolean isIn(long address, boolean isEndAddress, MemArea memArea) {
		return memArea.getStartAddress() <= address && (isEndAddress ? address - 1 : address) < memArea.getEndAddress();
	}

	public static boolean isNonContiguous(Iterable<? extends MemArea> sequence) {
		MemArea last = null;
		for (MemArea area : sequence) {
			if (last != null && area.getStartAddress() > last.getEndAddress()) {
				return true;
			}
			last = area;
		}
		return false;
	}

	public static MemArea subtract(MemArea minuend, MemArea subtrahend) {
		if (MemHeap.stickyMemCmp.compare(minuend, subtrahend) != 0) {
			return minuend; // no overlap
		}
		long startAddress = minuend.getStartAddress();
		long endAddress = minuend.getEndAddress();
		if (isIn(startAddress, false, subtrahend)) {
			startAddress = subtrahend.getEndAddress();
		} else if (isIn(endAddress, true, subtrahend)) {
			endAddress = subtrahend.getStartAddress();
		} else {
			throw new NonContiguousException(minuend, subtrahend);
		}
		if (endAddress < startAddress) {
			// allow an empty memArea
			endAddress = startAddress = 0;
		}
		return new ResolvedMemArea(startAddress, endAddress);
	}
	
	public static Collection<MemArea> subtract(Collection<? extends MemArea> minuend, MemArea subtrahend) {
		List<MemArea> result = new LinkedList<>();
		for (MemArea minArea : minuend) {
			try {
				MemArea difference = subtract(minArea, subtrahend);
				if (!isEmpty(difference)) {
					result.add(difference);
				}
			} catch (NonContiguousException nonContiguous) {
				result.add(new ResolvedMemArea(minArea.getStartAddress(), subtrahend.getStartAddress()));
				result.add(new ResolvedMemArea(subtrahend.getEndAddress(), minArea.getEndAddress()));
			}
		}
		return result;
	}

	public static Collection<? extends MemArea> subtract(Collection<? extends MemArea> minuend, Collection<MemArea> subtrahend) {
		Collection<? extends MemArea> result = minuend;
		for (MemArea subArea : subtrahend) {
			result = subtract(minuend, subArea);
		}
		return result;
	}

	public static boolean areasEqual(MemArea lArea, MemArea rArea) {
		return lArea.getStartAddress() == rArea.getStartAddress()
				&& lArea.getEndAddress() == rArea.getEndAddress();
	}
	
	public static boolean bytesEqual(MemData lData, MemData rData) {
		MemArea common = lData.trimTo(rData);
		if (isEmpty(common)) {
			return true;
		}
		try {
			byte[] lBytes = windowToByteArray(lData, common);
			byte[] rBytes = windowToByteArray(rData, common);
			return Arrays.equals(lBytes, rBytes);
		} catch (IOException ioe) {
			throw new RuntimeException(lData + " <> " + rData, ioe);
		}
	}

	public static byte[] windowToByteArray(MemData lData, MemArea common) throws IOException {
		Memory lMem = lData.getData().transform(lData, common);
		ByteArrayOutputStream los = new ByteArrayOutputStream((int) length(common));
		lMem.writeTo(los);
		return los.toByteArray();
	}

	public static Collection<MemData> sliceInto(MemData stream, Collection<? extends MemArea> mtRange) {
		Collection<MemData> roStreams = new ArrayList<>(mtRange.size());
		for (MemArea roRange : mtRange) {
			roStreams.add(stream.trimTo(roRange));
		}
		return roStreams;
	}

	public static Collection<MemLoad> sliceInto(MemLoad stream, Collection<? extends MemArea> mtRange) {
		Collection<MemLoad> roStreams = new ArrayList<>(mtRange.size());
		for (MemArea roRange : mtRange) {
			roStreams.add(stream.trimTo(roRange));
		}
		return roStreams;
	}

	private static class NonContiguousException extends IllegalArgumentException {
		private final MemArea minuend;
		private final MemArea subtrahend;

		public NonContiguousException(MemArea minuend, MemArea subtrahend) {

			this.minuend = minuend;
			this.subtrahend = subtrahend;
		}

		public MemArea getMinuend() {
			return minuend;
		}

		public MemArea getSubtrahend() {
			return subtrahend;
		}

		@Override
		public String getMessage() {
			return ("Subtrahend " + subtrahend + " lies inside the minuend " + minuend + "; difference non-contiguous");
		}
	}
}
