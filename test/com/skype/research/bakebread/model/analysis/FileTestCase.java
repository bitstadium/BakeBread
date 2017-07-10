/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * A test case that allocates a temporary folder in {@link #setUp()}. 
 */
public abstract class FileTestCase extends TestCase {
	public static final Set<PosixFilePermission> PERMISSIONS =
			PosixFilePermissions.fromString("rwxr--r--"); // 'x' required
												// for directory listings
	public static final FileAttribute<Set<PosixFilePermission>> ATTRIBUTE =
			PosixFilePermissions.asFileAttribute(PERMISSIONS);

	private Path tempFolderPath;
	private File tempFolderFile;

	public void setUp() throws Exception {
		super.setUp();
		tempFolderPath = Files.createTempDirectory("testSysRoot", ATTRIBUTE);
		tempFolderFile = tempFolderPath.toFile();
		mkdirs(tempFolderFile);
		setUp(tempFolderFile);
	}

	public File mkdirs(File folder, String... subFolders) throws FileNotFoundException {
		for (String subFolder : subFolders) {
			folder = new File(folder, subFolder);
		}
		return mkdirs(folder);
	}

	public File mkdirs(File folderFile) throws FileNotFoundException {
		if (!folderFile.isDirectory() && !folderFile.mkdirs()) {
			throw new FileNotFoundException(folderFile.getAbsolutePath());
		}
		return folderFile;
	}

	public File mkFile(File folder, String name, int size, byte[] pattern) throws IOException {
		return mkFile(new File(folder, name), size, pattern);	
	}
	
	public File mkFile(File file, int size, byte[] pattern) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		byte[] temp = new byte[size];
		int i = 0; while (i < size) {
			int l = Math.min(pattern.length, size - i);
			System.arraycopy(pattern, 0, temp, i, l);
			i += l;
		}
		fos.write(temp);
		fos.close();
		return file;
	}

	protected abstract void setUp(File tempFolderFile) throws IOException;

	public void tearDown() throws Exception {
		if (tempFolderFile.isDirectory()) {
			deleteRecursively(tempFolderFile);
		}
		super.tearDown();
	}

	private void deleteRecursively(File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File node : files) {
					deleteRecursively(node);
				}
			}
		}
		delete(file);
	}

	private void delete(File file) {
		if (!file.delete()) file.deleteOnExit();
	}
}
