/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff.bleach;

import com.skype.research.bakebread.nio.BufferAdapter;

import java.nio.Buffer;

/**
 * Known BL coding policies.
 */
public class CodeBleach<B extends Buffer> extends AbstractBleach<B> {

	protected final long code;
	protected final long mask;
	protected final int opCodeCount;
	
	private int subRangeIndex;

	/**
	 * Generic (stateful) bleach.
	 * @param code opcode without operands to detect
	 * @param mask operand (e.g. relative address) bit mask to clean out
	 * @param opCodeCount number of words after the "trigger" instruction to clean further
	 */
	public CodeBleach(BufferAdapter<B> adapter, long code, long mask, int opCodeCount) {
		super(adapter);
		this.code = code;
		this.mask = mask;
		this.opCodeCount = opCodeCount;
	}

	@Override
	public void reset() {
		subRangeIndex = 0;
	}

	private int getRangeIndex() {
		return subRangeIndex;
	}

	/**
	 * Stateless bleach.
	 * @param code opcode without operands to detect
	 * @param mask operand (e.g. relative address) bit mask to clean out
	 */
	public CodeBleach(BufferAdapter<B> adapter, long code, long mask) {
		this(adapter, code, mask, 1);
	}

	protected void bleachWord(B wordBuffer, long read) {
		if (subRangeIndex == 0) {
			long head = bleachHead(read);
			if (shouldStart(read, head)) {
				writeBack(wordBuffer, head);
				stepWithinRange(read, head);
			}
		} else {
			// the second argument is always nonzero
			long tail = bleachTail(read, subRangeIndex);
			writeBack(wordBuffer, tail);
			stepWithinRange(read, tail);
		}
	}

	public void stepWithinRange(long read, long head) {
		++subRangeIndex;
		if (shouldStop(read, head)) {
			subRangeIndex = 0;
		}
	}

	protected boolean shouldStart(long read, long head) {
		return head == code;
	}
	
	protected boolean shouldStop(long read, long tail) {
		return subRangeIndex == opCodeCount;
	}
	
	private long bleachHead(long read) {
		return getRangeIndex() > 0 ? read : read & ~mask;
	}

	protected long bleachTail(long read, int wordNumber) {
		return read;
	}
}
