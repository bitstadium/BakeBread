/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.match;

import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.model.MemSeam;
import com.skype.util.quality.CloseLook;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Hamming (in-place) distance between byte buffers.
 */
public class Hamming {
	private int common, unique;
	private int digits, octets;

	@SuppressWarnings("CloneDoesntCallSuperClone")
	public Hamming clone() {
		Hamming c = new Hamming();
		c.common = common;
		c.unique = unique;
		c.digits = digits;
		c.octets = octets;
		return c;
	}
	
	public void postByte(int xor) {
		octets += xor != 0 ? 1 : 0;
		digits += Integer.bitCount(0xff & xor);
	}

	private void postInt(int xor) {
		octets += xor != 0 ? 1 : 0;
		digits += Integer.bitCount(xor);
	}

	public void compare(ByteBuffer original, ByteBuffer modified) {
		original = original.duplicate();
		modified = modified.duplicate();
		common += Math.min(original.remaining(), modified.remaining());
		while (original.hasRemaining() && modified.hasRemaining()) {
			postByte(original.get() ^ modified.get());
		}
		final int remaining = original.remaining() + modified.remaining();
		unique += remaining;
		octets += remaining;
		digits += remaining << 3;
	}

	@Override
	public String toString() {
		return String.format("{common=%d unique=%d !> bytes=%d bits=%d}",
				common, unique, octets, digits);
	}

	public int getCommon() {
		return common;
	}

	public int getUnique() {
		return unique;
	}

	public int getDigits() {
		return digits;
	}
	
	public int getOctets() {
		return octets;
	}

	public void add(Hamming hamming) {
		this.common += hamming.common;
		this.unique += hamming.unique;
		this.octets += hamming.octets;
		this.digits += hamming.digits;
	}

	public void add(Einsteinian matter) {
		compare(matter.original.bb, matter.modified.bb);
	}

	public void add(Map<MemArea, MemSeam> seamMap,
	                ByteBuffer bbOriginal,
	                ByteBuffer bbModified,
	                CloseLook<MemSeam, Hamming> listener) {
		bbModified = bbModified.duplicate();
		bbOriginal = bbOriginal.duplicate();
		for (MemSeam modSeam : seamMap.values()) {
			Hamming partial = new Hamming();
			partial.compare(modSeam.getRange(bbModified), modSeam.inverse().getRange(bbOriginal));
			if (listener != null) {
				listener.onPartial(modSeam, partial);
			}
			add(partial);
		}
	}
}