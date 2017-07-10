/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * AuxInfo vector.
 */
public interface AuxInfo<A extends Buffer> {

	enum Type {
		AT_NULL,
		AT_IGNORE,
		AT_EXECFD,
		AT_PHDR,
		AT_PHENT,
		AT_PHNUM,
		AT_PAGESZ,
		AT_BASE,
		AT_FLAGS,
		AT_ENTRY,
		AT_NOTELF,
		AT_UID,
		AT_EUID,
		AT_GID,
		AT_EGID,
		AT_PLATFORM,
		AT_HWCAP,
		AT_CLKTCK,
		AT_FPUCW,
		AT_DCACHEBSIZE,
		AT_ICACHEBSIZE,
		AT_UCACHEBSIZE,
		AT_IGNOREPPC,
		AT_SECURE,
		AT_BASE_PLATFORM,

		AT_RANDOM,
		AT_HWCAP2,

		AT_RESERVED_27,
		AT_RESERVED_28,
		AT_RESERVED_29,
		AT_RESERVED_30,
		AT_EXECFN,
		AT_SYSINFO,
		AT_SYSINFO_EHDR,
	}
	
	static class AuxData<A extends Buffer> {
		private A vector;
		private boolean[] hasVal = new boolean[Type.values().length];
		private long[]  values = new long   [Type.values().length];;
		
		public A setVector(A vector) {
			this.vector = vector;
			for (int i = 0; i < vector.capacity(); ) {
				long key = get(vector, i++);
				long value = get(vector, i++);
				if (key >= 0 && key < Type.values().length) {
					hasVal[(int) key] = true;
					values[(int) key] = value;
				}
			}
			return vector;
		}

		private long get(A vector, int index) {
			if (vector instanceof IntBuffer) {
				return ((IntBuffer) vector).get(index);
			} else if (vector instanceof LongBuffer) {
				return ((LongBuffer) vector).get(index);
			} else if (vector instanceof ShortBuffer) {
				return ((ShortBuffer) vector).get(index);
			} else if (vector instanceof CharBuffer) {
				return ((CharBuffer) vector).get(index);
			} else if (vector instanceof ByteBuffer) {
				return ((ByteBuffer) vector).get(index);
			}
			throw new IllegalArgumentException("element type");
		}

		public boolean hasValue(Type type) {
			return hasVal[type.ordinal()];
		}
		
		public long getValue(Type type) {
			return values[type.ordinal()];
		}
	}
	
	A getAuxV();
	AuxData<A> getAuxData();
}
