/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.io.PackedString;
import com.skype.research.bakebread.model.analysis.Areas;
import com.skype.research.bakebread.model.analysis.ResolvedMemData;
import com.skype.research.bakebread.model.memory.MemArea;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.nio.FileMemory;
import com.skype.research.bakebread.nio.Memory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Represents an ELF file section.
 */
public class Section implements Marshaled, MemData {
	
	public enum Type {
		SHT_NULL,
		SHT_PROGBITS,
		SHT_SYMTAB,
		SHT_STRTAB,
		SHT_RELA,
		SHT_HASH,
		SHT_DYNAMIC,
		SHT_NOTE,
		SHT_NOBITS,
		SHT_REL,
		SHT_SHLIB,
		SHT_DYNSYM,
		SHT_NUM,
		;
	}
	
	public enum Flag {
		SHF_WRITE,
		SHF_ALLOC, // "SHF_READ"
		SHF_EXECINSTR,
		; // SHF_MASKPROC 0xf0000000

		final int bit = 1 << ordinal();
	}
	
	private final PtrSize ptrSize;

	// default (uninitialized) section is the obligatory 0th section 
	public Section(PtrSize ptrSize) {
		this.ptrSize = ptrSize;
	}
	
	private int name;
	private int type;
	private int flags;
	private long address;   // userspace memory address
	private long fileOffset;    // file fileOffset
	private long size;
	private int link;       // section header table index link. irrelevant to core dumps.
	private int info;       // supplementary info. irrelevant to core dumps.
	private long alignment;
	private long entrySize;

	private transient Memory data;
	private transient String literalName;

	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		name = dataInput.readInt();
		type = dataInput.readInt();
		flags = dataInput.readInt();
		address = ptrSize.readPtr(dataInput);
		fileOffset = ptrSize.readPtr(dataInput);
		size = ptrSize.readPtr(dataInput);
		link = dataInput.readInt();
		info = dataInput.readInt();
		alignment = ptrSize.readPtr(dataInput);
		entrySize = ptrSize.readPtr(dataInput);
		
		data = new FileMemory(fileChannel, fileOffset, size);
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		// assert data == new FileMemory(fileChannel, fileOffset, size);
		
		dataOutput.writeInt(name);
		dataOutput.writeInt(type);
		dataOutput.writeInt(flags);
		ptrSize.writePtr(dataOutput, address);
		ptrSize.writePtr(dataOutput, fileOffset);
		ptrSize.writePtr(dataOutput, size);
		dataOutput.writeInt(link);
		dataOutput.writeInt(info);
		ptrSize.writePtr(dataOutput, alignment);
		ptrSize.writePtr(dataOutput, entrySize);
	}

	public PtrSize getPtrSize() {
		return ptrSize;
	}

	public int getName() {
		return name;
	}

	public void setName(int name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public long getAddress() {
		return address;
	}

	public void setAddress(long address) {
		this.address = address;
	}

	public long getFileOffset() {
		return fileOffset;
	}

	public void setFileOffset(long fileOffset) {
		this.fileOffset = fileOffset;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getLink() {
		return link;
	}

	public void setLink(int link) {
		this.link = link;
	}

	public int getInfo() {
		return info;
	}

	public void setInfo(int info) {
		this.info = info;
	}

	public long getAlignment() {
		return alignment;
	}

	public void setAlignment(long alignment) {
		this.alignment = alignment;
	}

	public long getEntrySize() {
		return entrySize;
	}

	public void setEntrySize(long entrySize) {
		this.entrySize = entrySize;
	}

	public String resolveName(PackedString nameTable) {
		return literalName = nameTable.subSequence(name).toString();
	}

	public String getLiteralName() {
		return literalName;
	}
	
	@Override
	public long getStartAddress() {
		return address;
	}

	@Override
	public long getEndAddress() {
		return address == 0 ? 0 : address + size;
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
		return new ResolvedMemData(common, data.transform(this, common));
	}
	
	public MemData asFileData() {
		return new ResolvedMemData(fileOffset, fileOffset + size, data);
	}

	@Override
	public String toString() {
		return literalName + ":" + PrettyPrint.hexRangeSlim(this)
				+ "@" + PrettyPrint.hexWordSlim(fileOffset)
				+ "+" + PrettyPrint.hexWordSlim(size);
	}
}
