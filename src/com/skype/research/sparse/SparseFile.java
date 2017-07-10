/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse;

import com.skype.research.bakebread.io.Marshaled;
import com.skype.research.sparse.scanner.Scanner;
import com.skype.research.sparse.trimmer.NeedTrimmingException;
import com.skype.research.sparse.trimmer.TrimChannel;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;

/**
 * Sparse header structure.
 */
public class SparseFile implements Marshaled {
	public static final long MAGIC = 0xed26ff3a;
	
	private short major, minor;
	private short fileHeaderSize;
	private short chunkHeaderSize;
	private int  blockSize;
	private long blockCount;
	private long chunkCount;
	private long checksum;
	private BitSet overlap, tempSet;

	// scanning
	private Scanner scanner;

	// logging
	private PrintStream printWriter;

	public void setPrintStream(PrintStream printStream) {
		this.printWriter = printStream;
	}

	// TODO move to customization
	public void setOverlap(BitSet overlap) {
		this.overlap = overlap;
	}
	
	public void setScanner(Scanner scanner) {
		this.scanner = scanner;
	}

	@Override
	public void readExternal(DataInput dataInput, FileChannel fileChannel) throws IOException {
		major = dataInput.readShort(); // 2
		minor = dataInput.readShort(); // 2
		fileHeaderSize = dataInput.readShort(); // 2
		chunkHeaderSize = dataInput.readShort();// 2
		blockSize = dataInput.readInt(); // 4
		blockCount = dataInput.readInt();// 4
		chunkCount = dataInput.readInt();// 4
		checksum = dataInput.readInt();  // 4
		dataInput.skipBytes(fileHeaderSize - 4 * 4 - 2 * 4 - 4);
		tempSet = overlap != null ? new BitSet((int) blockCount) : null;
	}

	public void dumpHeader() {
		if (printWriter != null) {
			printWriter.println("Target size (b): " + blockSize * blockCount);
			printWriter.println("File header (b): " + fileHeaderSize);
			printWriter.println("Frag header (b): " + chunkHeaderSize);
			printWriter.println("Chunks to write: " + chunkCount);
			printWriter.println();
		}
	}

	@Override
	public void writeExternal(DataOutput dataOutput, FileChannel fileChannel) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Transfer data from the 
	 * @param input metadata input, with proper endianness
	 * @param in bulk data source
	 * @param out bulk data sink
	 */
	public void transfer(DataInput input, FileChannel in, TrimChannel out) throws IOException, NeedTrimmingException {
		long chunksProcessed = 0;
		long spaceProcessed = 0;
		final long outSize = blockCount * blockSize;
		while (chunksProcessed < chunkCount && spaceProcessed < outSize) {
			ChunkType chunkType = ChunkType.fromMagic(input.readShort());
			Chunk chunk = chunkType.createChunk(scanner);
			chunk.setHeaderSize(chunkHeaderSize);
			chunk.setBlockSize(blockSize);
			chunk.readExternal(input, in);
			if (overlap != null && !chunkType.isSkip()) {
				int chunkBlock = (int) (out.position() / blockSize);
				tempSet.clear();
				tempSet.set(chunkBlock, chunkBlock + (int) chunk.getBlockCount());
				if (overlap.intersects(tempSet)) {
					throw new IOException("Chunk " + chunk + " overlaps with unpacked area");
				} else {
					overlap.or(tempSet);
				}
			}
			if (printWriter != null) {
				printWriter.println(String.format("%6d/%6d %s ::: %s", chunksProcessed, chunkCount, chunkType, chunk));
				printWriter.flush();
			}
			spaceProcessed += chunk.transfer(input, in, out);
			chunksProcessed++;
		}
		if (chunksProcessed != chunkCount) {
			throw new EOFException("Chunks left: " + (chunkCount - chunksProcessed));
		}
		if (spaceProcessed < outSize) {
			throw new EOFException("Space left: " + (outSize - spaceProcessed) + " bytes");
		}
		expandToPosition(out);
		out.force(true);
	}

	private void expandToPosition(TrimChannel out) throws IOException {
		printWriter.println(String.format("Logical output size: %10d (b)", out.position()));
		printWriter.println(String.format("Written output size: %10d (b)", out.size()));
		if (out.position() > out.size()) {
			out.write(ByteBuffer.allocate(1));
			out.position(out.position() - 1);
		}
		out.truncate(out.position());
	}
}
