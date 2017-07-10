/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis.mock;

import com.skype.research.bakebread.model.analysis.ResolvedMemArea;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemPerm;

/**
 * Dummy memory mapping.
 */
public class MockMapInfo extends ResolvedMemArea implements MapInfo {

	private final MemPerm memPerm;
	private final long fileOffset;
	private final String fileName;

	public MockMapInfo(long startAddress, long endAddress, MemPerm memPerm) {
		this(startAddress, endAddress, memPerm, null);
	}
	
	public MockMapInfo(long startAddress, long endAddress, MemPerm memPerm, String fileName) {
		this(startAddress, endAddress, memPerm, 0, fileName);
	}
	
	public MockMapInfo(long startAddress, long endAddress, MemPerm memPerm, long fileOffset, String fileName) {
		super(startAddress, endAddress);
		this.memPerm = memPerm;
		this.fileOffset = fileOffset;
		this.fileName = fileName;
	}

	@Override
	public long getFileOffset() {
		return fileOffset;
	}

	@Override
	public short[] getPartition() {
		// we may want to isolate the system partition by :: rather than by the path prefix
		return new short[2];
	}

	@Override
	public int getFd() {
		return fileName == null || fileName.isEmpty() ? 0 : fileName.hashCode();
	}

	@Override
	public String getName() {
		return fileName;
	}

	@Override
	public boolean isReadable() {
		return memPerm.isReadable();
	}

	@Override
	public boolean isWritable() {
		return memPerm.isWritable();
	}

	@Override
	public boolean isRunnable() {
		return memPerm.isRunnable();
	}

	@Override
	public boolean isShared() {
		return memPerm.isShared();
	}
}
