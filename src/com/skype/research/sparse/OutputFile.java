/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.bakebread.io.ReverseEndianDataInput;
import com.skype.research.sparse.scanner.Scanner;
import com.skype.research.sparse.trimmer.NeedTrimmingException;
import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;

/**
 * Represents an output of a sparse conversion.
 */
public class OutputFile {

	private PrintStream printStream;
	private Collection<File> ins;
	private File out;

	final TrimChannel trimmer = new TrimChannel();
	final Scanner scanner;

	public OutputFile(File[] ins, File out, Scanner scanner) {
		this(Arrays.asList(ins), out, scanner);
	}
	
	public OutputFile(Collection<File> ins, File out, Scanner scanner) {
		this.ins = ins;
		this.out = out;
		this.scanner = scanner;
		if (scanner != null) {
			scanner.setTrimmer(trimmer);
		}
	}
	
	public OutputFile setPrintStream(PrintStream printStream) {
		this.printStream = printStream;
		return this;
	}
	
	public void transfer() throws IOException {
		try (FileOutputStream fileOutputStream = new FileOutputStream(out)) {
			trimmer.setChannel(fileOutputStream.getChannel());
			do try {
				tryTransfer(trimmer);
				return;
			} catch (NeedTrimmingException e) {
				printStream.printf("Encountered a cutting point at %10d (b) - cutting the head off...%n",
						e.getCuttingPoint());
			}
			while (true);
		}
	}
	
	private void tryTransfer(TrimChannel outTrimmer) throws IOException, NeedTrimmingException {
		BitSet overlap = ins.size() > 1 ? new BitSet() : null;
		// let actual i/o start
		for (File in : ins) {
			try (FileInputStream fileInputStream = new FileInputStream(in)) {
				dumpTransfer(out, in);
				DataInput input = new ReverseEndianDataInput(new DataInputStream(fileInputStream));
				SparseFile sparseFile = new SparseFile();
				sparseFile.setOverlap(overlap);
				sparseFile.setScanner(scanner);
				sparseFile.setPrintStream(printStream);
				//noinspection StatementWithEmptyBody
				while (input.readInt() != SparseFile.MAGIC) {}
				sparseFile.readExternal(input, fileInputStream.getChannel());
				sparseFile.dumpHeader();
				outTrimmer.position(0L);
				sparseFile.transfer(input, fileInputStream.getChannel(), outTrimmer);
			}
		}
	}

	public void dumpTransfer(File out, File in) {
		printStream.println();
		printStream.println("Reading from: " + in.getAbsolutePath());
		printStream.println("Writing to:   " + out.getAbsolutePath());
		printStream.println();
		printStream.println("Source size (b): " + in.length());
	}
}
