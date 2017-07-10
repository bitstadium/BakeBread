/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Denotes a bank of registers.
 */
public interface RegBank<B extends Buffer> {
	enum Special {
		ProcessorState
	}
	B getRegisterValues();
	long getRegisterValue(int index);
	ByteBuffer getRawRegisterValues(ByteOrder byteOrder);
	long getSpecialValue(Special kind);
}
