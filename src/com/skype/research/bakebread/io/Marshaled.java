/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Suppresses ClassNotFoundException
 */
public interface Marshaled {
	/**
	 * Read the entity, advancing the type-aware input and the file channel by its size.
	 * @param dataInput type-aware data source with correct byte order already enforced.
	 * @param fileChannel file channel to read extra data. Seeking and mapping are both
	 *                    allowed but the eventual position MUST be after the root data
	 *                    chunk placement, so that lists of uniform top-level data were
	 *                    retrieved seamlessly.
	 * @throws IOException if anything goes wrong.
	 */
	void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException;

	/**
	 * Write the entity, advancing the type-aware output and the file channel by its size.
	 * @param dataOutput  type-aware data sink with correct byte order already enforced.
	 * @param fileChannel file channel to re-read or amend previously written data. Seek
	 *                    or map it as much as you need but make sure the eventual "head"
	 *                    position is at the end of the file, so that data were written
	 *                    in sequential order without repositioning.
	 * @throws IOException if anything goes wrong.
	 */
	void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException;
}
