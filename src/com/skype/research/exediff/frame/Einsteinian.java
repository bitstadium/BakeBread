/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.frame;

import com.skype.research.bakebread.model.analysis.MemHeap;
import com.skype.research.exediff.model.MemSeam;

import java.nio.ByteBuffer;
import java.util.Collections;

public class Einsteinian {
	public final Newtonian original;
	public final Newtonian modified;

	public Einsteinian(ByteBuffer bbOriginal, long originalRefPoint,
	                   ByteBuffer bbModified, long modifiedRefPoint) {
		original = new Newtonian(bbOriginal, originalRefPoint);
		modified = new Newtonian(bbModified, modifiedRefPoint);
	}

	public long getTranslation() {
		return this.original.refPoint - this.modified.refPoint;
	}

	public MemHeap<MemSeam> sideBySide() {
		final long commonSide = Math.min(original.bb.capacity(), modified.bb.capacity());
		return new MemHeap<>(Collections.singleton(new MemSeam(0, commonSide, 1, 0, true)));
	}
}
