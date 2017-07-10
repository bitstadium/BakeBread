/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.minidump.streams.StreamType;
import com.skype.research.bakebread.minidump.streams.StreamTypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * A POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms680365(v=vs.85).aspx
 */
public class DirectoryEntry implements Marshaled {
	private StreamType streamType;
	private LocationDescription ld = new LocationDescription();
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		streamType = StreamTypes.fromInt(dataInput.readInt());
		ld.readExternal(dataInput, fileChannel);
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt(streamType.ordinal());
		ld.writeExternal(dataOutput, fileChannel);
	}
	
	public StreamType getStreamType() {
		return streamType;
	}
	
	public LocationDescription getLocationDescription() {
		return ld;
	}
}
