/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model;

import com.skype.research.bakebread.coredump.AuxInfo;
import com.skype.research.bakebread.coredump.ProcState;
import com.skype.research.bakebread.model.banks.ProcFlag;

import java.nio.Buffer;

/**
 * "Umbrella" application info such as PID/UID/GID
 */
public interface AppInfo<A extends Buffer> {

	String getProcStat(String psKey);
	// this should be a more generic data bank interface
	boolean isKnown(ProcFlag pf);
	boolean getFlag(ProcFlag pf);
	AuxInfo<A> getAuxInfo();
	CharSequence getCmdLine();

	static class Utils {

		public static int getIntStats(AppInfo appInfo, String key) {
			return getIntStats(appInfo, key, 0);
		}

		public static int getIntStats(AppInfo appInfo, String key, int defaultValue) {
			String val = appInfo.getProcStat(key);
			return val == null ? defaultValue : Integer.parseInt(val.split("\\s+")[0]);
		}

		public static long getLongHexStats(AppInfo appInfo, String sigPnd) {
			return getLongHexStats(appInfo, sigPnd, 0);
		}

		public static long getLongHexStats(AppInfo appInfo, String sigPnd, long defaultValue) {
			String val = appInfo.getProcStat(sigPnd);
			return val == null ? defaultValue : Long.parseLong(val.split("\\s+")[0], 16);
		}

		public static ProcState getProcState(AppInfo appInfo, String key, ProcState defaultValue) {
			String state = appInfo.getProcStat(key);
			if (state != null && state.length() > 0) {
				char s = state.charAt(0);
				ProcState procState = ProcState.valueOf(s);
				if (procState != null) {
					return procState;
				}
			}
			return defaultValue;
		}

		// TODO add interval types here
		// TODO move ProcState parser here
	}
}
