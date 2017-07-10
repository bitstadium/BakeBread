/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model;

import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemData;

import java.nio.Buffer;
import java.util.Collection;

/**
 * Ideal, representation-agnostic model of the dumped process.
 * Won't name the class "Process" because it would be a third one then (after java.lang.* and android.os.*).
 */
public interface DmpInfo<G extends Buffer, F extends Buffer, A extends Buffer> {
	AppInfo<A> getAppInfo();
	int[] getThreadIds();
	ThrInfo<G, F> getThread(int threadId);
	int getCrashedThreadId();
	Collection<? extends MapInfo> getMemMap();
	Collection<? extends MemData> getMemDmp();
}
