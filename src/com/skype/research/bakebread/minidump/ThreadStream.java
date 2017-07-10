/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.minidump.flags.CtxFlag;
import com.skype.research.bakebread.model.RegBank;
import com.skype.research.bakebread.model.SigInfo;
import com.skype.research.bakebread.model.ThrInfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

/**
 * A POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms680517(v=vs.85).aspx
 */
public class ThreadStream implements Marshaled, ThreadContextual, ThrInfo<IntBuffer, LongBuffer> {
	private int threadId;
	private int suspendCount;
	private int priorityClass;
	private int priority;
	private long threadEnvironmentBlock;
	private final MemoryStream stack = new MemoryStream();
	private final LocationDescription contextLocation = new LocationDescription();
	
	// readable in the second pass
	private final ThreadContext threadContext = new ThreadContext();
	// assignable in the third pass
	private SignalInfo sigInfo = SignalInfo.EMPTY;
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		threadId = dataInput.readInt();
		suspendCount = dataInput.readInt();
		priorityClass = dataInput.readInt();
		priority = dataInput.readInt();
		threadEnvironmentBlock = dataInput.readLong();
		stack.readExternal(dataInput, fileChannel);
		contextLocation.readExternal(dataInput, fileChannel);
		// read thread context and return
		contextLocation.readMarshaled(threadContext, dataInput, fileChannel);
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		// FIXME the thread context is written elsewhere and its position stored
		dataOutput.writeInt(threadId);
		dataOutput.writeInt(suspendCount);
		dataOutput.writeInt(priorityClass);
		dataOutput.writeInt(priority);
		dataOutput.writeLong(threadEnvironmentBlock);
		stack.writeExternal(dataOutput, fileChannel);
		contextLocation.writeExternal(dataOutput, fileChannel);
	}
	
	public int getThreadId() {
		return threadId;
	}

	@Override
	public boolean hasMainRegs() {
		return getThreadContext().hasCtxFlag(CtxFlag.INT);
	}

	@Override
	public boolean hasMathRegs() {
		return getThreadContext().hasCtxFlag(CtxFlag.VFP);
	}

	@Override
	public boolean hasSigInfo() {
		return sigInfo != SignalInfo.EMPTY;
	}

	@Override
	public RegBank<IntBuffer> getMainRegs() {
		return threadContext.getMainRegs();
	}

	@Override
	public RegBank<LongBuffer> getMathRegs() {
		return threadContext.getMathRegs();
	}

	@Override
	public SigInfo getSigInfo() {
		return sigInfo;
	}

	public int getSuspendCount() {
		return suspendCount;
	}
	
	public int getPriorityClass() {
		return priorityClass;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public long getThreadEnvironmentBlock() {
		return threadEnvironmentBlock;
	}
	
	public MemoryStream getStack() {
		return stack;
	}
	
	@Override
	public LocationDescription getContextLocation() {
		return contextLocation;
	}
	
	@Override
	public ThreadContext getThreadContext() {
		return threadContext;
	}
	
	public void setSignalInfo(SignalStream signalStream) {
		sigInfo = signalStream.getSignalInfo();
	}

	@Override
	public String toString() {
		return "Tid " + threadId + " stack " + stack.toString();
	}
}
