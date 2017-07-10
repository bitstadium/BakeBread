/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.io.Marshaled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * aka ExceptionStream
 */
public class SignalStream implements Marshaled, ThreadContextual {
	
	private int threadId;
	// skip 4 bytes
	private final SignalInfo signalInfo = new SignalInfo();
	private final LocationDescription contextLocation = new LocationDescription();
	private final ThreadContext threadContext = new ThreadContext();
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		threadId = dataInput.readInt();
		dataInput.skipBytes(4);
		signalInfo.readExternal(dataInput, fileChannel);
		contextLocation.readExternal(dataInput, fileChannel);
		// read the thread context.
		contextLocation.readMarshaled(threadContext, dataInput, fileChannel);
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		// FIXME the thread context is written elsewhere and its position stored
		dataOutput.writeInt(threadId);
		dataOutput.writeInt(0);
		signalInfo.writeExternal(dataOutput, fileChannel);
		contextLocation.writeExternal(dataOutput, fileChannel);
	}
	
	@Override
	public String toString() {
		return "*** tid " + + threadId + " signal " + signalInfo;
	}
	
	@Override
	public LocationDescription getContextLocation() {
		return contextLocation;
	}
	
	public int getThreadId() {
		return threadId;
	}
	
	public SignalInfo getSignalInfo() {
		return signalInfo;
	}
	
	@Override
	public ThreadContext getThreadContext() {
		return threadContext;
	}
}
