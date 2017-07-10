/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.model.SigInfo;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * MDException aka MINIDUMP_EXCEPTION
 */
public class SignalInfo implements Marshaled, SigInfo {
	
	public static final SignalInfo EMPTY = new SignalInfo();

	enum Signal {
		__SIG_ZERO,
		SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGTRAP,
		SIGABRT, SIGBUS, SIGFPE, SIGKILL, SIGUSR1,
		SIGSEGV, SIGUSR2, SIGPIPE, SIGALRM, SIGTERM,
		SIGSTKFLT, SIGCHLD, SIGCONT, SIGSTOP, SIGTSTP,
		SIGTTIN, SIGTTOU, SIGURG, SIGXCPU, SIGXFSZ,
		SIGVTALRM, SIGPROF, SIGWINCH, SIGIO, SIGPWR,
		SIGSYS;
		
		static String fromInt(int sigNo) {
			return sigNo + "/" + 
					(sigNo <= values().length 
							? values()[sigNo].name() 
							: String.format("SIG%02d", sigNo));
		}
	}
	
	private static final int PARAM_COUNT = 15;
	
	private int sigNo;
	private int flags;
	private long causeAddress;
	private long faultAddress;
	private int paramCount;
	// skip 4 bytes
	private final long[] parameters = new long[PARAM_COUNT];
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		sigNo = dataInput.readInt();
		flags  = dataInput.readInt();
		causeAddress = dataInput.readLong();
		faultAddress = dataInput.readLong();
		paramCount = dataInput.readInt();
		dataInput.skipBytes(4);
		for (int i = 0; i < parameters.length; ++i) {
			parameters[i] = dataInput.readLong();
		}
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt(sigNo);
		dataOutput.writeInt(flags);
		dataOutput.writeLong(causeAddress);
		dataOutput.writeLong(faultAddress);
		dataOutput.writeInt(paramCount);
		dataOutput.writeInt(0);
		for (long parameter : parameters) {
			dataOutput.writeLong(parameter);
		}
	}
	
	@Override
	public int getSigNo() {
		return sigNo;
	}

	public int getFlags() {
		return flags;
	}
	
	public long getCauseAddress() {
		return causeAddress;
	}
	
	public long getFaultAddress() {
		return faultAddress;
	}
	
	public int getParameterCount() {
		return paramCount;
	}
	
	public long getParameter(int index) {
		return parameters[index];
	}
	
	@Override
	public long getSigAddr() {
		return faultAddress;
	}

	@Override
	public long getSigData() {
		// this is not reliable anyway
		return causeAddress;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(Signal.fromInt(sigNo));
		if (flags != 0) {
			sb.append(" flags=").append(PrettyPrint.hexWordSlim(flags));
		}
		sb.append(" at ").append(PrettyPrint.hexWord(faultAddress));
		if (causeAddress != 0) {
			sb.append(" cause=").append(PrettyPrint.hexWord(causeAddress));
		}
		sb.append(' ').append(Arrays.toString(Arrays.copyOf(parameters, paramCount)));
		return sb.toString();
	}
}
