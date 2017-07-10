/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.config.OutConfig;
import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.ResolvedMemData;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.bakebread.model.memory.MemPerm;
import com.skype.research.bakebread.nio.FileMemory;
import com.skype.research.bakebread.nio.Memory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;

/**
 * Represents an ELF file program record.
 * http://www.sco.com/developers/gabi/latest/ch5.pheader.html
 */
public class Program implements Marshaled, MemData {

	enum Type {
		PT_NULL,
		PT_LOAD,    // offset, VA=mem, PA=0, FS=0, MS=size, RX, align 1
					// offset, VA=mem, PA=0, FS=size, MS=size, RW, align 1
		
		PT_DYNAMIC,
		PT_INTERP,
		PT_NOTE,    // offset, VA=0, PA=0, FS=size, MS=0, R, align 1
		PT_SHLIB,
		PT_PHDR,
		PT_TLS,

		GNU_EH_FRAME(0x6474e550), // dt.P
		GNU_STACK   (0x6474e551), // dt.Q
		GNU_RELRO   (0x6474e552), // dt.R
		
		EXIDX       (0x70000001),
		;

		public final int value;

		Type() {
			value = ordinal();
		}
		
		Type(int value) {
			this.value = value;
		}
	}
	
	enum Flag {
		Execute,
		Write,
		Read,
		;
		
		public final int value = 1 << ordinal();
		
		public boolean isSet(int flags) {
			return (flags & value) != 0;
		}
		public int set(int flags, boolean enable) {
			return enable ? flags | value : flags & ~value;
		}
	}
	
	// TODO Q1: "copy load section contents?"
	
	private final PtrSize ptrSize;
	
	private int type;
	private int flags;
	private long fileOffset;
	private long virtAddr;  // user memory address
	private long physAddr;  // normally 0
	private long fileSize;
	private long virtSize;
	private long alignment;

	private Memory data;
	
	public Program(PtrSize ptrSize, Type type) {
		this(ptrSize, type.ordinal());
	}
	
	public Program(PtrSize ptrSize, int type) {
		this.ptrSize = ptrSize;
		this.type = type;
	}

	public Program(Program original) {
		this(original.ptrSize, original.type);
		set(original);
	}

	public void set(Program original) {
		flags = original.flags;
		physAddr = original.physAddr;
		virtAddr = original.virtAddr;
		fileOffset = original.fileOffset;
		fileSize = original.fileSize;
		virtSize = original.virtSize;
		data = original.data;
	}

	// package private to make sure that bounds cannot expand, only contract
	Program(Program original, MemArea adjustTo) {
		this(original);
		adjustTo(adjustTo);
	}

	// package private to make sure that bounds cannot expand, only contract
	void adjustTo(MemArea adjustTo) {
		long sizeDelta = Areas.length(adjustTo) - Areas.length(this);
		long addrDelta = adjustTo.getStartAddress() - this.getStartAddress();
		this.physAddr = adjustBy(this.physAddr, addrDelta);
		this.virtAddr = adjustBy(this.virtAddr, addrDelta);
		this.fileOffset = adjustBy(this.fileOffset,   addrDelta);
		this.fileSize = adjustBy(this.fileSize, sizeDelta);
		this.virtSize = adjustBy(this.virtSize, sizeDelta);
		this.data = data == null ? null : data.transform(this, adjustTo);
	}
	
	public Program(PtrSize ptrSize, MemLoad memLoad, OutConfig outConfig) {
		this(ptrSize, Type.PT_LOAD);
		MemPerm memPerm = memLoad.getMapInfo();
		flags = Flag.Read.set(flags, memPerm.isReadable());
		flags = Flag.Write.set(flags, memPerm.isWritable());
		flags = Flag.Execute.set(flags, memPerm.isRunnable());
		// WISDOM offset is determined at payload writing time
		virtAddr = memLoad.getStartAddress();
		physAddr = 0; // no real mode, sorry
		virtSize = Areas.length(memLoad);
		fileSize = outConfig.shallWrite(memLoad) ? virtSize : 0;
		alignment = 1; // no alignment, place as is
		data = memLoad.getData();
	}

	@Override
	public long getStartAddress() {
		return virtAddr;
	}

	@Override
	public long getEndAddress() {
		return virtAddr + virtSize;
	}

	@Override
	public Memory getData() {
		return data;
	}

	@Override
	public MemData trimTo(MemArea memArea) {
		MemArea common = Areas.trim(this, memArea);
		if (common == this) {
			return this;
		}

		return new Program(this, common);
	}

	private static long adjustBy(long original, long addrDelta) {
		return original == 0 ? original + addrDelta : 0;
	}

	public MemData asFileData() {
		return new ResolvedMemData(fileOffset, fileOffset + fileSize, data);
	}
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		type = dataInput.readInt();
		// alternative, pack-friendly placement of flags
		if (ptrSize == PtrSize.LONG) {
			flags = dataInput.readInt();
		}
		fileOffset = ptrSize.readPtr(dataInput);
		virtAddr = ptrSize.readPtr(dataInput);
		physAddr = ptrSize.readPtr(dataInput);
		fileSize = ptrSize.readPtr(dataInput);
		virtSize = ptrSize.readPtr(dataInput);
		// original placement of flags
		if (ptrSize == PtrSize.INT) {
			flags = dataInput.readInt();
		}
		alignment = ptrSize.readPtr(dataInput);

		data = new FileMemory(fileChannel, fileOffset, fileSize);
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.writeInt(type);
		// alternative, pack-friendly placement of flags
		if (ptrSize == PtrSize.LONG) {
			dataOutput.writeInt(flags);
		}
		ptrSize.writePtr(dataOutput, fileOffset);
		ptrSize.writePtr(dataOutput, virtAddr);
		ptrSize.writePtr(dataOutput, physAddr);
		ptrSize.writePtr(dataOutput, fileSize);
		ptrSize.writePtr(dataOutput, virtSize);
		// original placement of flags
		if (ptrSize == PtrSize.INT) {
			dataOutput.writeInt(flags);
		}
		ptrSize.writePtr(dataOutput, alignment);
	}
	
	public void asSection(Collection<Section> toSection) {
		switch (Type.values()[type]) {
			case PT_NOTE:
			case PT_LOAD:
				Section section = new Section(ptrSize);
				// replicate flags
				// reference an existing file or embed existing data:

				// TODO Q2: "replicate program data as sections?"
				// "SHT_NOTE section header maps to the PT_NOTE program header."
				// -- add multiple per-thread notes
				// "SHT_NOBITS sections reference parts of other existing files;"
				// "SHT_PROGBITS section are present in the core file;"
				// Key to Flags:
				// W (write), A (alloc), X (execute), M (merge), S (strings), l (large)
				// I (info), L (link order), G (group), T (TLS), E (exclude), x (unknown)
				// O (extra OS processing required) o (OS specific), p (processor specific
				toSection.add(section);
				break;
		}
	}

	public long getFileSize() {
		return fileSize;
	}

	public long getVirtSize() {
		return virtSize;
	}

	public int getType() {
		return type;
	}

	long setAndAddOffset(long offset) {
		this.fileOffset = offset;
		return offset + fileSize;
	}

	public long getFileOffset() {
		return fileOffset;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
}
