/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.model.analysis.mock.MockMapInfo;
import com.skype.research.bakebread.model.analysis.mock.PermSet;
import junit.framework.Assert;

import java.io.File;

/**
 * Tests a file locator.
 */
public class FileFinderTest extends AppFileTestCase {

	public void testFullPath() throws Exception {
		File libc = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "/system/lib/libc.so"));
		Assert.assertEquals(LIBC_SIZE, libc.length());
	}
	
	public void testFlatRule() throws Exception {
		// absolute file paths are optional
		File libc = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "system/lib/libc.so"));
		Assert.assertEquals(LIBC_SIZE, libc.length());
	}
	
	public void testPartPath() throws Exception {
		File libc = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "/lib/libc.so"));
		Assert.assertEquals(null, libc);
	}
	
	public void testPtTrPath() throws Exception {
		File libc = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "lib/libc.so"));
		Assert.assertEquals(null, libc);
	}

	public void testLoneFile() throws Exception {
		File base = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "base.apk"));
		Assert.assertEquals(BASE_SIZE, base.length());
	}

	public void testGoneFile() throws Exception {
		File base = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "gone.apk"));
		Assert.assertEquals(null, base);
	}

	public void testLeafName() throws Exception {
		// even when there is a /system prefix, the "leaf" file name is checked
		File base = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "/system/data/data/what/base.apk"));
		Assert.assertEquals(BASE_SIZE, base.length());
	}
	
	public void testTailFile() throws Exception {
		File base = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "/data/data/what/base.apk"));
		Assert.assertEquals(BASE_SIZE, base.length());
	}

	public void testUnixJail() throws Exception {
		File jail = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "/bin/sh"));
		Assert.assertEquals(null, jail);
	}

	public void testWindJail() throws Exception {
		File jail = fileFinder.find(new MockMapInfo(0, 1024, PermSet.LIBRARY, 0, "/Windows/win.ini"));
		Assert.assertEquals(null, jail);
	}
}
