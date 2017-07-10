/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Registers closeables.
 */
public class AutoClose implements Closeable {
	private final Collection<Closeable> closeables = new LinkedList<>();
	private final PrintStream printStream;

	public AutoClose(PrintStream printStream) {
		this.printStream = printStream;
	}

	public AutoClose() {
		this.printStream = System.err;
	}

	public synchronized <T extends Closeable> T register(T closeable) {
		closeables.add(closeable);
		return closeable;
	}

	@Override
	public synchronized void close() {
		for (Closeable closeable : closeables) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace(printStream);
			}
		}
		closeables.clear();
	}
}
