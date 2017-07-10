/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.flags;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Flag sets used around.
 */
public interface Flag {
	int getBit();
	
	class Util {	
		public static Collection<Flag> parse(int value, Flag[] flags) {
			final Collection<Flag> retVal = new ArrayList<Flag>();
			for (Flag flag : flags) {
				if ((value & flag.getBit()) == flag.getBit()) {
					retVal.add(flag);
				}
			}
			return retVal;
		}
	}
}
