/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.streams;

/**
 * BreakPad stream types:
 * http://google-breakpad.googlecode.com/svn/branches/chrome_23/src/google_breakpad/common/minidump_format.h
 */
public enum Google implements StreamType {
	BpBegin, /* Breakpad extension types.  0x4767 , // "Gg" */
	BpInfoStream, // 0x47670001,  /* MDRawBreakpadInfo  */
	BpAssertionInfo, // 0x47670002,  /* MDRawAssertionInfo */
	/* These are additional minidump stream values which are specific to
	 * the linux breakpad implementation. */
	ProcCpuInfo, // 0x47670003,  /* /proc/cpuinfo      */
	ProcStatus, // 0x47670004,  /* /proc/$x/status    */
	EtcLsbRelease, // 0x47670005,  /* /etc/lsb-release   */
	ProcCmdLine, // 0x47670006,  /* /proc/$x/cmdline   */
	ProcEnviron, // 0x47670007,  /* /proc/$x/environ   */
	ProcAuxV, // 0x47670008,  /* /proc/$x/auxv      */
	ProcMaps, // 0x47670009,  /* /proc/$x/maps      */
	LinuxDSODebug, // 0x4767000A   /* MDRawDebug         */
	;
	
	@Override
	public int type() {
		return 0x4767; // Gg
	}
}
