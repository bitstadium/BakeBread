/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.streams;

/**
 * Original/Microsoft stream types:
 * https://msdn.microsoft.com/en-us/library/windows/desktop/ms680394(v, //vs.85).aspx
 */
public enum Microsoft implements StreamType {
	UnusedStream, // 0,
	ReservedStream0, // 1,
	ReservedStream1, // 2,
	ThreadListStream, // 3,
	ModuleListStream, // 4,
	MemoryListStream, // 5,
	ExceptionStream, // 6,
	SystemInfoStream, // 7,
	ThreadExListStream, // 8,
	Memory64ListStream, // 9,
	CommentStreamA, // 10,
	CommentStreamW, // 11,
	HandleDataStream, // 12,
	FunctionTableStream, // 13,
	UnloadedModuleListStream, // 14,
	MiscInfoStream, // 15,
	MemoryInfoListStream, // 16,
	ThreadInfoListStream, // 17,
	HandleOperationListStream, // 18,
	; // LastReservedStream         , // 0xffff
	
	@Override
	public int type() {
		return 0;
	}
}
