/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.coredump;

/**
 * Process states (running, traced, zombie etc.)
 */
public enum ProcState {

	// fs/proc/array.c
	// static const char * const task_state_array[] = ...
	
	RUNNING('R'),
	SLEEPING('S'),
	DISK_SLEEP('D'),
	STOPPED('T'),
	TRACING('t'),
	ZOMBIE('Z'),
	X_DEAD('X'),
	x_DEAD('x'),
	WAKEKILL('K'),
	WAKING('W'),
	;
	
	public final byte sCode;
	public final char sName;
	public final boolean zombie;

	ProcState(char sName) {
		this.sCode = (byte) (1 << (ordinal() - 1));
		this.sName = sName;
		this.zombie = Character.toUpperCase(sName) == 'Z';
	}

	public static ProcState valueOf(char sName) {
		for (ProcState ps : values()) {
			if (sName == ps.sName) {
				return ps;
			}
		}
		return null;
	}
}
