/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.io.FromToUtils;
import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.minidump.flags.CtxFlag;
import com.skype.research.bakebread.minidump.flags.Flag;
import com.skype.research.bakebread.minidump.flags.PswFlag;
import com.skype.research.bakebread.model.RegBank;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

/**
 * Thread context as specified by 
 */
public class ThreadContext implements Marshaled {
	static final boolean SHOW_FP_REGS = false;
	
	static final String[] regIds = new String[16];
	static {
		regIds[10] = "sl";
		regIds[11] = "fp";
		regIds[12] = "ip";
		regIds[13] = "sp";
		regIds[14] = "lr";
		regIds[15] = "pc";
	}
	private final int[] registers = new int[16];
	
	private int ctxFlags;
	
	private int psw;
	private long fpscr;
	private final long[] fpRegs = new long[32];
	private final int[] fpExtra = new int[8];

	private final RegBank<IntBuffer> mainRegBank = new RegBank<IntBuffer>() {
		final IntBuffer buffer = IntBuffer.wrap(registers);
		
		@Override
		public IntBuffer getRegisterValues() {
			return buffer;
		}

		@Override
		public long getRegisterValue(int index) {
			return registers[index];
		}

		@Override
		public ByteBuffer getRawRegisterValues(ByteOrder desired) {
			return FromToUtils.asByteBuffer(buffer, desired);
		}

		@Override
		public long getSpecialValue(Special kind) {
			return psw;
		}
	};
	
	private final RegBank<LongBuffer> mathRegBank
			= new RegBank<LongBuffer>() {
		final LongBuffer buffer = LongBuffer.wrap(fpRegs);

		@Override
		public LongBuffer getRegisterValues() {
			return buffer;
		}

		@Override
		public long getRegisterValue(int index) {
			return fpRegs[index];
		}

		@Override
		public ByteBuffer getRawRegisterValues(ByteOrder desired) {
			return FromToUtils.asByteBuffer(buffer, desired);
		}

		@Override
		public long getSpecialValue(Special kind) {
			return fpscr;
		}
	};
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		ctxFlags = dataInput.readInt();
		for (int i = 0; i < registers.length; i++) {
			registers[i] = dataInput.readInt();
		}
		psw = dataInput.readInt();
		fpscr = dataInput.readLong();
		for (int i = 0; i < fpRegs.length; i++) {
			fpRegs[i] = dataInput.readLong();
		}
		for (int i = 0; i < fpExtra.length; i++) {
			fpExtra[i] = dataInput.readInt();
		}
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt(ctxFlags);
		for (int reg : registers) {
			dataOutput.writeInt(reg);
		}
		dataOutput.writeInt(psw);
		dataOutput.writeLong(fpscr);
		for (long fpReg : fpRegs) {
			dataOutput.writeLong(fpReg);
		}
		for (int extra : fpExtra) {
			dataOutput.writeInt(extra);
		}
	}
	
	public boolean hasCtxFlag(CtxFlag flag) {
		return 0 != (flag.getBit() & ctxFlags);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("flags=")
				.append(PrettyPrint.hexWord(ctxFlags))
				.append(' ')
				.append(Flag.Util.parse(ctxFlags, CtxFlag.values()))
				.append('\n');
		if (hasCtxFlag(CtxFlag.INT)) {
			for (int i = 0; i < registers.length; ++i) {
				sb.append(regIds[i] == null ? "r" + i : regIds[i])
						.append('=')
						.append(PrettyPrint.hexWord(registers[i]));
				sb.append((i % 4 == 3) ? "\n" : " ");
			}
			sb.append("cpsr=")
					.append(PrettyPrint.hexWord(psw))
					.append(' ')
					.append(Flag.Util.parse(psw, PswFlag.values()));
		}
		//noinspection PointlessBooleanExpression
		if (SHOW_FP_REGS && hasCtxFlag(CtxFlag.VFP)) {
			sb.append("\n");
			sb.append("fpscr=")
					.append(PrettyPrint.hexDWord(fpscr));
			for (int i = 0; i < fpRegs.length; i++) {
				sb.append((i % 4 == 0) ? "\n" : "  ");
				sb.append('f').append(Integer.toString(i, 32));
				final long dWord = fpRegs[i];
				sb.append('=').append(PrettyPrint.hexDWord(dWord));
			}
			for (int i = 0; i < fpExtra.length; i++) {
				sb.append((i % 8 == 0) ? "\nx: " : " ");
				sb.append(PrettyPrint.hexWord(fpExtra[i]));
			}
		}
		return sb.toString();
	}
	
	public RegBank<IntBuffer> getMainRegs() {
		return mainRegBank;
	}

	public RegBank<LongBuffer> getMathRegs() {
		return mathRegBank;
	}
}
