/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.rolling;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Compute a rolling hash from a byte buffer, a byte array or a byte array range.
 */
public interface HashRoller extends Rolling {
	RollingHash index(ByteBuffer source);
	RollingHash index(byte[] source, int beginIndex, int endIndex, ByteOrder byteOrder);
	RollingHash index(byte[] source, ByteOrder byteOrder);
}
