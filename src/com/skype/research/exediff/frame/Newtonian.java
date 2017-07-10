/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.frame;

import java.nio.ByteBuffer;

public class Newtonian {
	public final ByteBuffer bb;
	public final long refPoint;
	public final long refLimit;

	public Newtonian(ByteBuffer bb, long refPoint) {
		this.bb = bb;
		this.refPoint = refPoint;
		this.refLimit = refPoint + bb.capacity();
	}
	
	public boolean isWithin(long address) {
		return address >= refPoint && address < refLimit;
	}

	public int asOffset(long address) {
		return (int) (address - refPoint);
	}

	public byte getByte(long address) {
		return isWithin(address) ? bb.get(asOffset(address)) : -1;
	}
}