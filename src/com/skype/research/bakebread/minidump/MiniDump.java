/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.minidump.streams.StreamType;

import java.util.Collection;
import java.util.List;

/**
 * Representation of a MiniDump file.
 */
public interface MiniDump {
	Header getHeader();
	DirectoryEntry getDirectoryEntry(StreamType rootStreamType);
	List<DirectoryEntry> getDirectory();
	Collection<MemoryStream> getMemoryStreams();
	Collection<ThreadStream> getThreadStreams();
	@Deprecated
	Collection<ModuleStream> getModuleStreams();
	Collection<MemoryStream> getOtherStreams();
	Collection<MemoryStream> getStackStreams();
	Collection<MemoryMapping> getMappings();
	SignalStream getSignalStream();
	Collection<StreamType> getTopLevelStreamTypes();
}
