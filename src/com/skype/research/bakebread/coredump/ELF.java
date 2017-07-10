/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

import com.skype.research.bakebread.config.OutConfig;
import com.skype.research.bakebread.coredump.notes.AppInfoNote;
import com.skype.research.bakebread.coredump.notes.AuxVecNote;
import com.skype.research.bakebread.coredump.notes.FileMapNote;
import com.skype.research.bakebread.coredump.notes.MathRegNote;
import com.skype.research.bakebread.coredump.notes.ReadableNote;
import com.skype.research.bakebread.coredump.notes.SigInfoNote;
import com.skype.research.bakebread.coredump.notes.StatInfoNote;
import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.bakebread.io.PackedString;
import com.skype.research.bakebread.io.ReverseEndianDataOutput;
import com.skype.research.bakebread.model.AppInfo;
import com.skype.research.bakebread.model.DmpInfo;
import com.skype.research.bakebread.model.SigInfo;
import com.skype.research.bakebread.model.ThrInfo;
import com.skype.research.bakebread.model.analysis.MemHeap;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemLoad;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Represents a writable core dump.
 */
public class ELF implements Marshaled {

	public enum Type {
		NONE, REL, EXEC, DYN, CORE;
	}

	public enum Preset {
		ANDROID_32 {
			@Override
			public void init(ELF elf) {
				final ElfHeader elfHeader = elf.elfHeader;
				elfHeader.setByteOrder(ElfHeader.ARM_ORDER);
				elfHeader.setPtrSize(PtrSize.INT);
				elfHeader.setOsAbi(ElfHeader.EABI); // ARM
				elfHeader.setMachine(ElfHeader.ARM);
				// default offsets
				// immediate placement of the notes
				elfHeader.setProgEntryCount(0);
				// immediate placement of the sections after 0 entries
				elfHeader.enableSectHeaders(0, elf);
			}
		},
		;

		public abstract void init(ELF elfHeader);
	}

	// in
	private final Program notePad;
	private final Program relProg;
	private final List<Note> notes = new ArrayList<>();
	private final MemHeap<Program> loads = new MemHeap<>();
	private final List<Section> sects = new ArrayList<>();
	private PackedString nameTable = PackedString.EMPTY;
	private final Map<String, Section> named = new HashMap<>();
	
	// own
	public final ElfHeader elfHeader = new ElfHeader();

	public ELF(Preset arch, Type type) {
		elfHeader.setType(type);
		arch.init(this);
		// WISDOM notePad is not stored in loads as it may "stick" to anything starting with zero
		// WISDOM no mapping or load in userspace starts with zero but we won't rely on is blindly 
		notePad = new Program(elfHeader.getPtrSize(), Program.Type.PT_NOTE); // zero, zero
		relProg = new Program(elfHeader.getPtrSize(), Program.Type.PT_NULL);
	}
	
	public boolean hasNotes() {
		return !notes.isEmpty();
	}

	public void addNote(Note note) {
		notes.add(note);
	}

	public void addNotes(DmpInfo<?, ?, ?> miniDump) {
		addNtPrc(miniDump.getAppInfo());
		addThreads(miniDump);
		addAuxV(miniDump.getAppInfo().getAuxInfo().getAuxV());
		addFiles(miniDump.getMemMap());
	}
	
	public <G extends Buffer, F extends Buffer> void addThreads(DmpInfo<G, F, ?> dmpInfo) {
		// TODO unit test it (verify the amount of data written)
		int[] threadIds = dmpInfo.getThreadIds();
		AppInfo appInfo = dmpInfo.getAppInfo();
		
		for (int threadId : threadIds) {
			ThrInfo<G, F> thrInfo = dmpInfo.getThread(threadId);
			SigInfo sigInfo = thrInfo.getSigInfo();
			StatInfoNote<G> statInfoNote = new StatInfoNote<G>(elfHeader.getPtrSize());
			statInfoNote.setSigInfo(sigInfo);
			statInfoNote.setAppInfo(appInfo);
			statInfoNote.setThrInfo(thrInfo);
			// statInfoNote.setTimes(); // MOREINFO look for time statistic
			statInfoNote.setRegBank(thrInfo.getMainRegs());
			statInfoNote.setFpValid(thrInfo.hasMathRegs());
			addNote(statInfoNote);
			// MOREINFO or wait, is it struct user ????
			if (thrInfo.hasMathRegs()) {
				MathRegNote<F> fpRegInfo = new MathRegNote<F>(elfHeader.getPtrSize());
				fpRegInfo.setRegBank(thrInfo.getMathRegs());
				addNote(fpRegInfo);
			}
			if (thrInfo.hasSigInfo()) {
				SigInfoNote thSigInfo = new SigInfoNote(elfHeader.getPtrSize());
				thSigInfo.setSigInfo(sigInfo);
				thSigInfo.setAppInfo(appInfo);
				addNote(thSigInfo);
			}
		}
	}

	public void addNtPrc(AppInfo appInfo) {
		AppInfoNote appInfoNote = new AppInfoNote(elfHeader.getPtrSize());
		// default, will be overwritten in setAppInfo()
		appInfoNote.setState(ProcState.TRACING);
		// TODO appInfoNote.setNice(0)
		// TODO appInfoNote.setFlags(0);
		appInfoNote.setAppInfo(appInfo);
		CharSequence appName = appInfo.getCmdLine();
		appInfoNote.setCommandLine(appName, appName);
		addNote(appInfoNote);
	}

	public <A extends Buffer> void addAuxV(A auxV) {
		AuxVecNote<A> noteAuxV = new AuxVecNote<>(elfHeader.getPtrSize());
		noteAuxV.setDesc(auxV);
		addNote(noteAuxV);
	}

	public void addFiles(Collection<? extends MapInfo> memMap) {
		FileMapNote noteFile = new FileMapNote(elfHeader.getPtrSize());
		noteFile.setTypeAndName(Note.Type.NT_FILE);
		noteFile.setDesc(memMap);
		addNote(noteFile);
	}

	public void addLoad(MemLoad memLoad, OutConfig outConfig) {
		loads.add(new Program(elfHeader.getPtrSize(), memLoad, outConfig));
	}

	public void addLoads(Iterable<MemLoad> memLoads, OutConfig outConfig) {
		for (MemLoad memLoad : memLoads) {
			addLoad(memLoad, outConfig);
		}
	}

	@Deprecated
	public long computeSectionHeaderOffset() {
		return 0; // FIXME compute section header offset (or record the position and write it back?)
	}
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		notes.clear();
		loads.clear();
		sects.clear();
		named.clear();
		elfHeader.readExternal(dataInput, fileChannel);
		readProgramHeaders(dataInput, fileChannel);
		readNotes(dataInput, fileChannel);
		// readLoads(dataInput, fileChannel); // no-op
		readSections(dataInput, fileChannel);
	}

	private void readProgramHeaders(DataInput dataInput, FileChannel fileChannel) throws IOException {
		long offset = elfHeader.getProgHeaderOff();
		if (offset != 0) {
			fileChannel.position(offset);
			for (int i = 0; i < elfHeader.getProgEntryCount(); ++i) {
				Program program = new Program(elfHeader.getPtrSize(), 0);
				program.readExternal(dataInput, fileChannel);
				if (program.getType() == Program.Type.PT_NOTE.ordinal()) {
					notePad.set(program);
				} else if (program.getType() == Program.Type.GNU_RELRO.ordinal()) {
					relProg.set(program);
				} else if (program.getType() == Program.Type.PT_LOAD.ordinal()) {
					loads.add(program);
				}
			}
		}
	}

	private void readNotes(DataInput dataInput, FileChannel fileChannel) throws IOException {
		long offset = notePad.getFileOffset();
		fileChannel.position(offset);
		long end = offset + notePad.getFileSize();
		while (fileChannel.position() < end) {
			Note note = new ReadableNote(elfHeader.getPtrSize());
			note.readExternal(dataInput, fileChannel);
			addNote(note);
		}
	}

	private void readSections(DataInput dataInput, FileChannel fileChannel) throws IOException {
		long offset = elfHeader.getSectHeaderOff();
		if (offset != 0) {
			Section strTable = null;
			fileChannel.position(offset);
			for (int i = 0; i < elfHeader.getSectEntryCount(); ++i) {
				Section section = new Section(elfHeader.getPtrSize());
				section.readExternal(dataInput, fileChannel);
				sects.add(section);
				if (i == elfHeader.getStringTableIndex()) {
					strTable = section;
				}
			}
			if (strTable != null) {
				fileChannel.position(strTable.getFileOffset());
				byte[] data = new byte[(int) strTable.getSize()];
				dataInput.readFully(data);
				nameTable = new PackedString(data);
				for (Section section : sects) {
					named.put(section.resolveName(nameTable), section);
				}
			}
		}
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		dataOutput = ReverseEndianDataOutput.ensureByteOrder(dataOutput, elfHeader.getByteOrder());
		int progCount = loads.size();
		if (hasNotes()) {
			long size = 0;
			for (Note note : notes) {
				size += note.getFileSize();
			}
			notePad.setFileSize(size);
			progCount ++;
		}
		elfHeader.setProgEntryCount(progCount);
		elfHeader.writeExternal(dataOutput, fileChannel);
		writeProgramHeaders(dataOutput, fileChannel);
		writeNotes(dataOutput, fileChannel);
		writeLoads(dataOutput, fileChannel);
		// TODO writeSections();
	}

	private void writeProgramHeaders(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		long offset = elfHeader.getProgHeaderOff();
			offset += elfHeader.getProgHeaderSize() * loads.size();
		if (hasNotes()) {
			offset += elfHeader.getProgHeaderSize();
			offset = notePad.setAndAddOffset(offset);
		}
		for (Program program : loads.values()) {
			offset = program.setAndAddOffset(offset);
		}
		if (hasNotes()) {
			notePad.writeExternal(dataOutput, fileChannel);
		}
		for (Program program : loads.values()) {
			program.writeExternal(dataOutput, fileChannel);
		}
	}

	private void writeNotes(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		for (Note note : notes) {
			note.writeExternal(dataOutput, fileChannel);
		}
	}

	private void writeLoads(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		// FIXME progress indicator
		for (Program memData : loads.values()) {
			assert memData.getFileOffset() == fileChannel.position();
			final long length = memData.getFileSize();
			if (length > 0) { // implicit shallWrite() - make memData responsibility? 
				memData.getData().writeTo(fileChannel);
			}
		}
	}
	
	public Program getRelocatedReadOnly() {
		return relProg;
	}

	public Section getSection(int index) {
		return sects.get(index);
	}
	
	public Section getSection(String name) {
		return named.get(name);
	}
	
	public int getSectionIndex(Section section) {
		return sects.indexOf(section);
	}
	
	public int getSectionIndex(String name) {
		return getSectionIndex(named.get(name));
	}
	
	public int getSectionIndex(String name, int index) {
		int j = 0, i = 0;
		while (j < sects.size()) {
			if (name.equals(getSection(j).getLiteralName())) {
				if (i++ == index) {
					return index;
				}
			}
			++j;
		}
		throw new NoSuchElementException(name + "#" + index);
	}
	
	public Section getSection(String name, int index) {
		return getSection(getSectionIndex(name, index));
	}

	public int getSectionCount() {
		return elfHeader.getSectEntryCount();
	}
}
