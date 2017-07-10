/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.coredump.AuxInfo;
import com.skype.research.bakebread.io.Lifecycle;
import com.skype.research.bakebread.io.PackedString;
import com.skype.research.bakebread.io.ReverseEndianDataInput;
import com.skype.research.bakebread.minidump.streams.Google;
import com.skype.research.bakebread.minidump.streams.Microsoft;
import com.skype.research.bakebread.minidump.streams.StreamType;
import com.skype.research.bakebread.model.AppInfo;
import com.skype.research.bakebread.model.DmpInfo;
import com.skype.research.bakebread.model.ThrInfo;
import com.skype.research.bakebread.model.banks.ProcFlag;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemData;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An implementation of MiniDump backed by a File.
 */
public class MiniDumpFromFile implements MiniDump, Lifecycle, DmpInfo<IntBuffer, LongBuffer, IntBuffer>, AppInfo<IntBuffer>, AuxInfo<IntBuffer> {
	
	// specification
	private final File file;
	
	// access implementation (streams/channels)
	private RandomAccessFile connection; // todo push down, extract seek
	private DataInput dataInput; // reversed bytes data input (logical)
	private FileChannel channel;
	
	// payload; candidate for pull
	private final Header header = new Header();
	private final List<DirectoryEntry> directory = new ArrayList<DirectoryEntry>();
	private final List<DirectoryEntry> roDirectory = Collections.unmodifiableList(directory);
	private final Map<StreamType, DirectoryEntry> deMap = new LinkedHashMap<StreamType, DirectoryEntry>();
	private final Set<MemoryStream> memoryStreams = new HashSet<MemoryStream>();
	private final List<ThreadStream> threadStreams = new ArrayList<ThreadStream>();
	private final List<ModuleStream> moduleStreams = new ArrayList<ModuleStream>();
	private final List<MemoryMapping> mappings = new ArrayList<MemoryMapping>();
	private final SignalStream signalStream = new SignalStream();
	private final Set<StreamType> standalone = new LinkedHashSet<StreamType>();
	private final Map<Integer, ThreadStream> threadsById = new LinkedHashMap<Integer, ThreadStream>();
	
	// derivatives
	private final Collection<MemoryStream> stackStreams = new HashSet<MemoryStream>(memoryStreams);
	private final Collection<MemoryStream> otherStreams = new HashSet<MemoryStream>(memoryStreams);
	
	// read-only representation
	private final Collection<MemoryStream> roMemoryStreams = Collections.unmodifiableSet(memoryStreams);
	private final Collection<ThreadStream> roThreadStreams = Collections.unmodifiableList(threadStreams);
	private final Collection<ModuleStream> roModuleStreams = Collections.unmodifiableList(moduleStreams);
	private final Collection<MemoryMapping> roMappings = Collections.unmodifiableList(mappings);
	private final Collection<StreamType> roStandalone = Collections.unmodifiableCollection(standalone);
	// read-only derivatives
	private final Collection<MemoryStream> roStackStreams = Collections.unmodifiableCollection(stackStreams);
	private final Collection<MemoryStream> roOtherStreams = Collections.unmodifiableCollection(otherStreams);
	
	// data banks. refactor when you are done.
	private IntBuffer auxV;
	private final AuxData<IntBuffer> auxData = new AuxData<>();
	private final Map<String, String> stats = new LinkedHashMap<>();

	public MiniDumpFromFile(File file) {
		this.file = file;
	}
	
	@Override
	public void open() throws IOException {
		connection = new RandomAccessFile(file, "r");
		dataInput = new ReverseEndianDataInput(connection);
		channel = connection.getChannel();
		// data initialization
		header.readExternal(dataInput, channel);
		directory.clear();
		header.getStreamDirectoryRva().navigate(channel);
		for (int i = 0; i < header.getStreamCount(); ++i) {
			final DirectoryEntry streamDef = new DirectoryEntry();
			streamDef.readExternal(dataInput, channel);
			directory.add(streamDef);
			deMap.put(streamDef.getStreamType(), streamDef);
		}
		standalone.addAll(deMap.keySet());
		
		// signal
		readSignalStream();
		readThreadStreams();
		readMemoryStreams();
		for (MemoryStream memoryStream : memoryStreams) {
			memoryStream.setChannel(connection.getChannel());
		}
		analyzeThreads();
		
		readModuleStreams();
		if (!readMemoryMapping()) {
			for (ModuleStream moduleStream : moduleStreams) {
				// FIXME compare memory and file offsets (ReadElf!) and split mappings
				mappings.add(new MemoryMapping(moduleStream));
			}
		}
		readProcessStats();
		analyzeAuxV();
	}

	private boolean readProcessStats() throws IOException {
		DirectoryEntry entry = getDirectoryEntry(Google.ProcStatus);
		if (entry != null) {
			final Reader reader = entry.getLocationDescription().asAsciiReader(dataInput, channel);
			final LineNumberReader lnr = new LineNumberReader(reader);
			String line;
			while ((line = lnr.readLine()) != null) {
				String[] split = line.split(":");
				if (split.length > 1) {
					stats.put(split[0].trim().toLowerCase(), split[1].trim());
				}
			}
			assertConsumed(entry);
			return true;
		} else {
			return false;
		}
	}

	private void analyzeAuxV() throws IOException {
		DirectoryEntry auxVStream = getDirectoryEntry(Google.ProcAuxV);
		if (auxVStream != null) {
			LocationDescription ld = auxVStream.getLocationDescription();
			auxV = ld.mapOriginalChannel(connection.getChannel())
					.order(targetByteOrder()).asIntBuffer();
			auxData.setVector(auxV);
		}
	}

	private void readSignalStream() throws IOException {
		DirectoryEntry signalEntry = getDirectoryEntry(Microsoft.ExceptionStream);
		if (signalEntry == null) {
			throw new MalformedMiniDumpException("No signal stream in input");
		}
		navigateTo(signalEntry);
		signalStream.readExternal(dataInput, channel);
		assertConsumed(signalEntry);
	}
	
	private void analyzeThreads() {
		otherStreams.addAll(memoryStreams);
		for (ThreadStream threadStream : threadStreams) {
			final MemoryStream stack = threadStream.getStack();
			stackStreams.add(stack);
			otherStreams.remove(stack);
			threadsById.put(threadStream.getThreadId(), threadStream);
		}
		ThreadStream crashed = threadsById.get(signalStream.getThreadId());
		crashed.setSignalInfo(signalStream);
		threadStreams.remove(crashed);
		threadStreams.add(0, crashed);
	}
	
	private void navigateTo(DirectoryEntry entry) throws IOException {
		navigateTo(entry.getLocationDescription());
	}
	
	private void navigateTo(LocationDescription ld) throws IOException {
		ld.getRVA().navigate(channel);
	}
	
	private void assertConsumed(DirectoryEntry entry) throws IOException {
		entry.getLocationDescription().assertConsumed(channel);
	}

	private void readMemoryStreams() throws IOException {
		standalone.remove(Microsoft.MemoryListStream);
		DirectoryEntry entry = deMap.get(Microsoft.MemoryListStream);
		navigateTo(entry);
		int streamCount = dataInput.readInt();
		for (int i = 0; i < streamCount; ++i) {
			final MemoryStream stream = new MemoryStream();
			stream.readExternal(dataInput, channel);
			memoryStreams.add(stream);
		}
		assertConsumed(entry);
	}

	private void readThreadStreams() throws IOException {
		standalone.remove(Microsoft.ThreadListStream);
		DirectoryEntry entry = deMap.get(Microsoft.ThreadListStream);
		navigateTo(entry);
		int streamCount = dataInput.readInt();
		for (int i = 0; i < streamCount; ++i) {
			final ThreadStream stream = new ThreadStream();
			stream.readExternal(dataInput, channel);
			threadStreams.add(stream);
		}
		assertConsumed(entry);
	}

	private void readModuleStreams() throws IOException {
		standalone.remove(Microsoft.ModuleListStream);
		DirectoryEntry entry = deMap.get(Microsoft.ModuleListStream);
		navigateTo(entry);
		int streamCount = dataInput.readInt();
		for (int i = 0; i < streamCount; ++i) {
			final ModuleStream stream = new ModuleStream();
			stream.readExternal(dataInput, channel);
			moduleStreams.add(stream);
		}
		assertConsumed(entry);
		for (ModuleStream moduleStream : moduleStreams) {
			moduleStream.readName(dataInput, channel);
		}
	}
	
	private boolean readMemoryMapping() throws IOException {
		DirectoryEntry entry = getDirectoryEntry(Google.ProcMaps);
		if (entry != null) {
			final Reader reader = entry.getLocationDescription().asAsciiReader(dataInput, channel);
			MemoryMapping.parse(reader, mappings);
			assertConsumed(entry);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void close() throws IOException {
		connection.close();
	}
	
	@Override
	public Header getHeader() {
		return header;
	}
	
	@Override
	public DirectoryEntry getDirectoryEntry(StreamType rootStreamType) {
		return deMap.get(rootStreamType);
	}
	
	@Override
	public List<DirectoryEntry> getDirectory() {
		return roDirectory;
	}
	
	@Override
	public Collection<MemoryStream> getMemoryStreams() {
		return roMemoryStreams;
	}
	
	@Override
	public Collection<ThreadStream> getThreadStreams() {
		return roThreadStreams;
	}
	
	@Override
	@Deprecated
	public Collection<ModuleStream> getModuleStreams() {
		return roModuleStreams;
	}
	
	@Override
	public Collection<MemoryStream> getOtherStreams() {
		return roOtherStreams;
	}
	
	@Override
	public Collection<MemoryStream> getStackStreams() {
		return roStackStreams;
	}
	
	@Override
	public Collection<MemoryMapping> getMappings() {
		return roMappings;
	}
	
	@Override
	public SignalStream getSignalStream() {
		return signalStream;
	}

	@Override
	public Collection<StreamType> getTopLevelStreamTypes() {
		return roStandalone;
	}

	@Override
	public AppInfo<IntBuffer> getAppInfo() {
		return this;
	}

	@Override
	public AuxInfo<IntBuffer> getAuxInfo() {
		return this;
	}

	@Override
	public int[] getThreadIds() {
		Set<Integer> keySet = threadsById.keySet();
		int[] thrIds = new int[keySet.size()];
		int i = 0; for (Integer tid : keySet) {
			thrIds[i++] = tid;
		}
		return thrIds;
	}

	@Override
	public ThrInfo<IntBuffer, LongBuffer> getThread(int threadId) {
		return threadsById.get(threadId);
	}

	@Override
	public int getCrashedThreadId() {
		return signalStream.getThreadId();
	}

	@Override
	public Collection<? extends MapInfo> getMemMap() {
		return roMappings;
	}

	@Override
	public Collection<? extends MemData> getMemDmp() {
		return roMemoryStreams;
	}
	
	public String getProcStat(String psKey) {
		return stats.get(psKey.toLowerCase());
	}

	// the below methods are overly posix-specific...
	@Override
	public boolean isKnown(ProcFlag pf) {
		return pf == ProcFlag.JustForked;
	}

	@Override
	public boolean getFlag(ProcFlag pf) {
		return false;
	}
	
	@Override
	public IntBuffer getAuxV() {
		return auxV;
	}

	@Override
	public AuxData<IntBuffer> getAuxData() {
		return auxData;
	}

	@Override
	public CharSequence getCmdLine() {
		try {
			LocationDescription ld = getDirectoryEntry(Google.ProcCmdLine).getLocationDescription();
			// ByteOrder desiredOrder = targetByteOrder();
			ByteBuffer bb = ld.mapOriginalChannel(connection.getChannel());
			byte[] packed = new byte[bb.capacity()];
			bb.get(packed);
			return new PackedString(packed);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public ByteOrder targetByteOrder() {
		return ReverseEndianDataInput.byteOrder(dataInput);
	}
}
