/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * A resource (such as file, database connection etc.) that can be recurrently opened and closed.
 */
public interface Lifecycle extends Closeable {
	void open() throws IOException;
}
