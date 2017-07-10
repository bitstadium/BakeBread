/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.io.ReverseEndianDataInput;
import com.skype.research.bakebread.io.ReverseEndianDataOutput;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * Executable and Linkable Format header. Specs and tutorials:
 * http://www.skyfree.org/linux/references/ELF_Format.pdf
 * http://www.gabriel.urdhr.fr/2015/05/29/core-file/
 * http://uhlo.blogspot.fr/2012/05/brief-look-into-core-dumps.html
 * http://www.docjar.com/html/api/Linker/ELF/Section.java.html
 * http://man7.org/linux/man-pages/man5/core.5.html aka `man core`
 * https://docs.oracle.com/cd/E19455-01/806-0633/6j9vn6q30/index.html
 * 
 * Authoritative structure definitions:
 * $NDKROOT/platforms/android-$API/arch-arm/usr/include/asm/elf.h
 * $NDKROOT/platforms/android-$API/arch-arm/usr/include/linux/elf.h
 * $NDKROOT/platforms/android-$API/arch-arm/usr/include/elf.h
 * 
 * See also (though I found those less useful):
 * https://sourceware.org/git/gitweb.cgi?p=binutils-gdb.git;a=tree
 */
public class ElfHeader implements Marshaled {

	public static final int VERSION = 1;
	public static final char ARM = 0x28;
	public static final char EABI = 'a';

	public static final ByteOrder ARM_ORDER = ByteOrder.LITTLE_ENDIAN;

	static final byte[] MAGIC = { 0x7f, 'E', 'L', 'F'};

	enum Magic {
		$, E, L, F,
		PtrSize,
		Endian,
		Version,
		OsAbi,
		;
		final int i = ordinal();
		
		public int get(byte[] magic) {
			return magic[i] & 0xff;
		}
		
		public <T> void set(byte[] magic, T[] values, T search, int defIndex) {
			for (int j = 0; j < values.length; j++) {
				if (Objects.equals(values[j], search)) {
					defIndex = (byte) j;
					break;
				}
			}
			set(magic, defIndex);
		}
		
		public void set(byte[] magic, int value) {
			magic[i] = (byte) value;
		}
	}
	
	static final ByteOrder[] ENDIAN = { null, ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN };

	//	unsigned char e_ident[EI_NIDENT];       ;  01 01 01 61 = EI_CLASS EI_DATA EI_VERSION EI_OSABI
	private final byte[] ident = new byte[SizeOf.ELF_IDENT];
	private transient ByteOrder byteOrder;
	private transient PtrSize ptrSize;

	// the below code uses char for uint16
	private ELF.Type type;       // ET_CORE       4
	private char machine;    // ARM =         0x28
	private int version;   // EV_CURRENT    1
	private long entry;       // 0   // ptr
	private long progHeaderOff;       // program header // ptr
	private long sectHeaderOff;       // section header // ptr
	private int flags;        // BitSet<EF_machine_flag>
	// private char fileHeaderSize;     // header size
	// private char progHeaderSize;  // program header size
	private char progEntryCount;      // program entry count
	// private char sectHeaderSize;  // section header size
	private char sectEntryCount;      // section header count
	private char strTableIndex;   // string table index, reliably SHN_UNDEF=0 by default

	public ElfHeader() {
		System.arraycopy(MAGIC, 0, ident, 0, MAGIC.length);
		setVersion(VERSION);
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		Magic.Endian.set(ident, ENDIAN, byteOrder, 0 /* none */);
	}

	public PtrSize getPtrSize() {
		return ptrSize;
	}
	
	public void setPtrSize(PtrSize ptrSize) {
		this.ptrSize = ptrSize;
		Magic.PtrSize.set(ident, PtrSize.values(), ptrSize, 0 /* none */);
	}
	
	public int getVersion() {
		return Magic.Version.get(ident);
	}

	public void setVersion(int version) {
		Magic.Version.set(ident, version);
		this.version = version; 
	}
	
	public char getOsAbi() {
		return (char) Magic.OsAbi.get(ident);
	}
	
	public void setOsAbi(char value) {
		Magic.OsAbi.set(ident, value);
	}

	public ELF.Type getType() {
		return type;
	}

	public void setType(ELF.Type type) {
		this.type = type;
	}

	public char getMachine() {
		return machine;
	}

	public void setMachine(char machine) {
		this.machine = machine;
	}
	
	public int getFileHeaderSize() {
		return ptrSize.fileHeaderSize;
	}

	public int getProgHeaderSize() {
		return ptrSize.progHeaderSize;
	}

	public int getSectHeaderSize() {
		return ptrSize.sectHeaderSize;
	}

	public long getProgHeaderOff() {
		return progHeaderOff;
	}

	public long getSectHeaderOff() {
		return sectHeaderOff;
	}
	
	public long getStringTableOff() {
		return sectHeaderOff + getSectHeaderSize() * strTableIndex;
	}

	// true on add, false on clear
	public void setProgEntryCount(int count) {
		boolean enable = fitInChar(count);
		progHeaderOff = enable ? getFileHeaderSize() : 0;
		progEntryCount = (char) count;
	}
	
	// true on add, false on clear. MOREINFO compute total size
	public void enableSectHeaders(int count, ELF container) {
		boolean enable = fitInChar(count);
		sectHeaderOff = enable ? container.computeSectionHeaderOffset() : 0;
		sectEntryCount = (char) count;
	}
	
	public int getProgEntryCount() {
		return progEntryCount;
	}

	public int getSectEntryCount() {
		return sectEntryCount;
	}

	public int getStringTableIndex() {
		return strTableIndex;
	}
	
	public boolean fitInChar(int count) {
		if (count < 0 || count > Character.MAX_VALUE) throw new IllegalArgumentException();
		return count != 0;
	}

	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		dataInput.readFully(ident);
		if (ident[0] != MAGIC[0] || ident[1] != MAGIC[1] || ident[2] != MAGIC[2] || ident[3] != MAGIC[3]) {
			throw new MalformedElfMagicException("signature");
		}
		// initialize transients
		ptrSize     = PtrSize.values()[Magic.PtrSize.get(ident)];
		byteOrder   = ENDIAN[Magic.Endian.get(ident)];
		// we may want to assert 'A' for ARM/Linux but in fact we are ABI independent
		dataInput   = ReverseEndianDataInput.ensureByteOrder(dataInput, byteOrder);
		type = ELF.Type.values()[dataInput.readChar()]; // 4 for core dump
		machine = dataInput.readChar();
		version = dataInput.readInt();
		if (version != getVersion()) {
			throw new MalformedElfMagicException("version");
		}
		entry = ptrSize.readPtr(dataInput);
		progHeaderOff = ptrSize.readPtr(dataInput);
		sectHeaderOff = ptrSize.readPtr(dataInput);
		flags = dataInput.readInt();
		if (dataInput.readChar() != ptrSize.fileHeaderSize) {
			throw new MalformedElfMagicException("file header size");
		}
		if (dataInput.readChar() != ptrSize.progHeaderSize) {
			throw new MalformedElfMagicException("program header size");
		}
		progEntryCount = dataInput.readChar();
		if (dataInput.readChar() != ptrSize.sectHeaderSize) {
			throw new MalformedElfMagicException("section header size");
		}
		sectEntryCount = dataInput.readChar();
		strTableIndex  = dataInput.readChar();
		// we have read fileHeaderSize bytes since magic, inclusive
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput.write(ident);
		dataOutput = ReverseEndianDataOutput.ensureByteOrder(dataOutput, byteOrder);
		dataOutput.writeChar(type.ordinal()); // 4 for core dump
		dataOutput.writeChar(machine);
		dataOutput.writeInt(version);
		ptrSize.writePtr(dataOutput, entry);
		ptrSize.writePtr(dataOutput, progHeaderOff);
		ptrSize.writePtr(dataOutput, sectHeaderOff);
		dataOutput.writeInt(flags);
		dataOutput.writeChar(ptrSize.fileHeaderSize);
		dataOutput.writeChar(ptrSize.progHeaderSize);
		dataOutput.writeChar(progEntryCount);
		dataOutput.writeChar(ptrSize.sectHeaderSize);
		dataOutput.writeChar(sectEntryCount);
		dataOutput.writeChar(strTableIndex );
	}
}
