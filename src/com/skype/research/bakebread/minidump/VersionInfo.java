/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.minidump;

import com.skype.research.bakebread.io.Marshaled;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * POJO for https://msdn.microsoft.com/en-us/library/windows/desktop/ms646997(v=vs.85).aspx
 */
public class VersionInfo implements Marshaled {
	
	public enum Field {
		 dwSignature,
		 dwStructVersion,
		 dwFileVersionMS,
		 dwFileVersionLS,
		 dwProductVersionMS,
		 dwProductVersionLS,
		 dwFileFlagsMask,
		 dwFileFlags,
		 dwFileOS,
		 dwFileType,
		 dwFileSubtype,
		 dwFileDateMS,
		 dwFileDateLS,
	}
	
	private final int[] fields = new int[Field.values().length]; 
	
	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		for (int i = 0; i < fields.length; i++) {
			fields[i] = dataInput.readInt();
		}
	}
	
	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		for (int field : fields) {
			dataOutput.writeInt(field);
		}
	}
	
	public int getField(Field field) {
		return fields[field.ordinal()];
	}
}
