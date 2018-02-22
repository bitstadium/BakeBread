/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.PrettyPrint;
import com.skype.research.bakebread.model.memory.MapInfo;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Collection;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A single memory mapping in the userspace process.
 * Examples:
 * <pre>
 *    937ae000-938ab000 rw-p 00000000 00:00 0          [stack:27507]
 *    938ab000-938c2000 r--s 001f5000 b3:1c 1671389    /data/app/com.example.app-1/base.apk
 *    938c2000-9392f000 rw-p 00000000 00:04 230062     /dev/ashmem/dalvik-large object space allocation (deleted)
 *    9392f000-93930000 ---p 00000000 00:00 0 
 * </pre>
 */
public class MemoryMapping implements MapInfo {
	private static final Pattern LINE_PATTERN = Pattern.compile(
					"^" +
					"(\\p{XDigit}{8})" + // start address
					"\\-" +
					"(\\p{XDigit}{8})" + // end address
					"\\s+" +
					"([r\\-])([w\\-])([x\\-])([sp])" + // flags x4
					"\\s+" +
					"(\\p{XDigit}{8,12})" + // file offset
					"\\s+" +
					"(\\p{XDigit}{2,4}):(\\p{XDigit}{2,4})" + // partition x2 
					"\\s+" +
					"(\\d+)" +  // FD
					"\\s*" +    // optional space
					"(.*?)" + // path or mapping name
					"[\\s\\x0a\\x0d]*$",
			Pattern.UNICODE_CHARACTER_CLASS
	);
	public static final Pattern SO_NAME = Pattern.compile("lib.*\\.so");

	enum Defined {
		MEMORY, FLAGS, OFFSET, INODE, MODULE
	}
	
	private final String line;
	private final long startAddress, endAddress;
	private final boolean readable, writable, runnable, shared;
	private final long offset;
	private final short[] partition = new short[2]; // unsigned byte[]
	private final long fd;
	private final String name;
	private final EnumSet<Defined> defined;
	
	public MemoryMapping(Matcher matcher) {
		int g = 0;
		line = matcher.group(g++);
		startAddress = Long.parseLong(matcher.group(g++), 16);
		endAddress = Long.parseLong(matcher.group(g++), 16);
		readable = matcher.group(g++).charAt(0) != '-';
		writable = matcher.group(g++).charAt(0) != '-';
		runnable = matcher.group(g++).charAt(0) != '-';
		shared = matcher.group(g++).charAt(0) == 's';
		offset = Long.parseLong(matcher.group(g++), 16);
		partition[0] = Short.parseShort(matcher.group(g++), 16);
		partition[1] = Short.parseShort(matcher.group(g++), 16);
		fd = Long.parseLong(matcher.group(g++));
		name = matcher.group(g); 
		assert g == matcher.groupCount();
		defined = EnumSet.allOf(Defined.class);
	}
	
	public MemoryMapping(ModuleStream moduleStream) {
		startAddress = 0xffffffffL & moduleStream.getImageBase();
		endAddress = startAddress + moduleStream.getImageSize();
		readable = writable = runnable = true; shared = false;
		offset = 0; // entire image. we don't account for "holes" but that only affects data sections.
		partition[0] = partition[1] = 0xff;
		fd = 0xffff;
		name = moduleStream.getModuleName();
		line = PrettyPrint.hexRangeSlim(this)
				+ ' ' + PrettyPrint.printFlags(this)
				+ ' ' + PrettyPrint.hexWordSlim(offset)
				+ ' ' + String.format("%02x:%02x", partition[0], partition[1])
				+ ' ' + PrettyPrint.padTo(Long.toString(fd), 10)
				+ ' ' + name;
		defined = EnumSet.of(Defined.MEMORY, Defined.FLAGS, Defined.MODULE);
	}

	public MemoryMapping(String line) {
		startAddress = 0;
		endAddress = 0;
		readable = writable = runnable = true; shared = false;
		offset = 0; // entire image. we don't account for "holes" but that only affects data sections.
		partition[0] = partition[1] = 0;
		fd = 0;
		Matcher soName = SO_NAME.matcher(line);
		name = soName.matches() ? soName.group() : "";
		this.line = line;
		defined = soName.matches() ? EnumSet.of(Defined.MODULE) : EnumSet.noneOf(Defined.class);
	}

	public static void matchLine(String line, Collection<MemoryMapping> mappings) {
		Matcher matcher = LINE_PATTERN.matcher(line);
		if (matcher.matches()) {
			// if it does not, either it's a new architecture or something is seriously broken
			mappings.add(new MemoryMapping(matcher));
		} else {
			mappings.add(new MemoryMapping(line));
		}
	}
	
	public static void parse(Reader reader, Collection<MemoryMapping> outMappings) throws IOException {
		LineNumberReader lnr = new LineNumberReader(reader);
		String line;
		while ((line = lnr.readLine()) != null) {
			matchLine(line, outMappings);
		}
	}
	
	@Override
	public String toString() {
		return line;
	}
	
	private void ensure(Defined chapter) {
		if (! defined.contains(chapter)) {
			throw new NoSuchElementException(chapter.name());
		}
	}

	@Override
	public long getStartAddress() {
		ensure(Defined.MEMORY);
		return startAddress;
	}
	
	@Override
	public long getEndAddress() {
		ensure(Defined.MEMORY);
		return endAddress;
	}
	
	@Override
	public boolean isReadable() {
		ensure(Defined.FLAGS);
		return readable;
	}
	
	@Override
	public boolean isWritable() {
		ensure(Defined.FLAGS);
		return writable;
	}
	
	@Override
	public boolean isRunnable() {
		ensure(Defined.FLAGS);
		return runnable;
	}
	
	@Override
	public boolean isShared() {
		ensure(Defined.FLAGS);
		return shared;
	}
	
	@Override
	public long getFileOffset() {
		ensure(Defined.OFFSET);
		return offset;
	}
	
	@Override
	public short[] getPartition() {
		ensure(Defined.INODE);
		return partition;
	}
	
	@Override
	public long getFd() {
		ensure(Defined.INODE);
		return fd;
	}
	
	@Override
	public String getName() {
		ensure(Defined.MODULE);
		return name;
	}
}
