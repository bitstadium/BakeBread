/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump.notes;

import com.skype.research.bakebread.coredump.PtrSize;
import com.skype.research.bakebread.model.memory.MapInfo;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;

/**
 * /proc/.../maps note
 */
public final class FileMapNote extends TypedNote {
	public FileMapNote(PtrSize ptrSize) {
		super(ptrSize, Type.NT_FILE);
	}

	// MOREINFO feature: replace file names with symbolized file names (e.g. *.so -> *.so.dbg)?
	
	private Collection<? extends MapInfo> memMap;
	private int pageSize = 1; // GDB de-facto

	public void setDesc(Collection<? extends MapInfo> memMap) {
		this.memMap = Collections.unmodifiableCollection(memMap);
		setDescLen(computeDescSize());
	}

	// @see http://www.gabriel.urdhr.fr/2015/05/29/core-file/

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		super.writeExternal(dataOutput, fileChannel);
		ptrSize.writePtr(dataOutput, memMap.size());
		ptrSize.writePtr(dataOutput, pageSize);
		for (MapInfo mapInfo : memMap) {
			ptrSize.writePtr(dataOutput, mapInfo.getStartAddress());
			ptrSize.writePtr(dataOutput, mapInfo.getEndAddress());
			ptrSize.writePtr(dataOutput, mapInfo.getFileOffset());
		}
		int stLen = 0;
		for (MapInfo mapInfo : memMap) {
			String fileName = mapInfo.getName();
			dataOutput.writeBytes(fileName);
			dataOutput.writeByte(0); //c_str
			stLen += fileName.length()+1;
		}
		ptrSize.writePadding(dataOutput, stLen);
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	private int computeDescSize() {
		int wordWidth = ptrSize.pointerSize;
		int headerSize = wordWidth * 2;
		int recordSize = wordWidth * 3;
		int total = headerSize;
		for (MapInfo mapInfo : memMap) {
			total += mapInfo.getName().length() + 1 + recordSize;
		}
		return ptrSize.roundUp(total);
	}
}
