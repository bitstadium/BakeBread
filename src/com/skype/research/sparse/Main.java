/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.sparse.config.SparseConfig;
import com.skype.research.sparse.config.cmdline.CmdLineSparseConfig;
import com.skype.research.sparse.scanner.Scanner;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.RecognitionException;
import sun.misc.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Entry point of the sparse image expander.
 */
public class Main {

	public static void main(String[] args) throws IOException {
		final PrintStream printStream = System.out;
		showHelp(printStream, "BANNER.txt");
		try {
			SparseConfig sparseConfig = new CmdLineSparseConfig(args);
			if (sparseConfig.hasAnythingToDo()) {
				File out = sparseConfig.getOut();
				if (out.exists() && !out.delete()) {
					throw new IOException("Can't overwrite " + out.getAbsolutePath());
				}
				// customization
				final Scanner scanner = sparseConfig.getScannerType().createScanner();
				scanner.setScannerCfg(sparseConfig.getScannerConf());
				final OutputFile outputFile = new OutputFile(sparseConfig.getIns(), out, scanner);
				outputFile.setPrintStream(printStream);
				outputFile.transfer();
				scanner.report(printStream);
				return;
			}
		} catch (ConfigurationException|RecognitionException e) {
			e.printStackTrace(printStream);
		}
		showHelp(printStream, "README.txt");
	}

	private static void showHelp(PrintStream printStream, String name) throws IOException {
		printStream.write(IOUtils.readFully(Main.class.getResourceAsStream(name), Short.MAX_VALUE, false));
	}
}
