/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.sparse.scanner.Scanner;

/**
 * Three field types.
 */
public enum ChunkType {
	CHUNK_TYPE_DATA((short) 0xCAC1) {
		@Override
		public Chunk createChunk(Scanner scanner) {
			return new DataChunk(scanner);
		}
	},
	CHUNK_TYPE_ZERO((short) 0xCAC2) {
		@Override
		public Chunk createChunk(Scanner scanner) {
			return new ZeroChunk();
		}
	},
	CHUNK_TYPE_SKIP((short) 0xCAC3) {
		@Override
		public Chunk createChunk(Scanner scanner) {
			return new SkipChunk();
		}
	},
	;

	public final short magic;

	ChunkType(short magic) {
		this.magic = magic;
	}

	public static ChunkType fromMagic(short headerMagic) {
		for (ChunkType chunkType : values()) {
			if (chunkType.magic == headerMagic) {
				return chunkType;
			}
		}
		throw new IllegalArgumentException(String.format("Unrecognized header: %08x", headerMagic));
	}

	public boolean isSkip() {
		return this == CHUNK_TYPE_SKIP;
	}

	public abstract Chunk createChunk(Scanner scanner);
}
