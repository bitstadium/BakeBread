/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump.streams;

import java.util.HashMap;
import java.util.Map;

/**
 * Maintains a registry of stream types.
 */
public class StreamTypes {
	static final Map<Short, StreamType[]> exhaustiveMapping = new HashMap<Short, StreamType[]>() {
		private void register(StreamType[] values) {
			put((short) values[0].type(), values);
		}
		
		{
			register(Microsoft.values());
			register(Google.values());
		}
	};
	
	public static StreamType fromInt(int id) {
		return exhaustiveMapping.get((short) (id >> 16))[id & 0xffff];
	}
}
