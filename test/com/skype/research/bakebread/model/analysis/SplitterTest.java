/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.analysis;

import com.skype.research.bakebread.config.BitExactValidation;
import com.skype.research.bakebread.config.FogConfig;
import com.skype.research.bakebread.config.FogConfigMap;
import com.skype.research.bakebread.config.ManConfig;
import com.skype.research.bakebread.config.ManConfigSet;
import com.skype.research.bakebread.config.MemoryFog;
import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.config.ValConfig;
import com.skype.research.bakebread.config.ValConfigSet;
import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.model.analysis.mock.MockMapInfo;
import com.skype.research.bakebread.model.analysis.mock.MockMemData;
import com.skype.research.bakebread.model.analysis.mock.PermSet;
import com.skype.research.bakebread.model.host.FileMapper;
import com.skype.research.bakebread.model.host.HostFileFinder;
import com.skype.research.bakebread.model.host.HostFileMapper;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemData;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.bakebread.model.module.ModuleInfo;
import junit.framework.Assert;

import java.io.Closeable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Test the target memory splitter component 
 */
public class SplitterTest extends AppFileTestCase {

	AutoClose autoClose;
	ValConfig valConfig = new ValConfigSet(EnumSet.allOf(BitExactValidation.class));
	ValConfig noValConf = new ValConfigSet(EnumSet.noneOf(BitExactValidation.class));
	ManConfig manConfig = new ManConfigSet(EnumSet.noneOf(ModuleAnalysis.class));
	FogConfigMap fogConfig = new FogConfigMap();
	
	FileMapper fileMapper;

	/**
	 * Given available dump and host data, reconstruct the big memory picture
	 * according to {@link Credibility}-defined priorities.
	 * <ul>
	 * <li>Dump data</li>
	 * <li>Host data</li>
	 * <li>Fill data</li>
	 * </ul>
	 * @param mapping         process memory map. If unknown, generate with {@link ModuleInfo} from module list.
	 * @param hostPaths       paths to look up files by names
	 * @param streams         streams readily available as data
	 * @param fogConfig rules to apply to memory with unknown or unreliable contents.
	 * @param valConfig
	 * @param autoClose       {@link Closeable} registry sink
	 */
	public static List<MemLoad> split(Collection<? extends MapInfo> mapping,
	                                  Collection<? extends File> hostPaths,
	                                  Collection<? extends MemData> streams,
	                                  ManConfig manConfig,
	                                  FogConfig fogConfig,
	                                  ValConfig valConfig,
	                                  AutoClose autoClose) {
		return Splitter.split(mapping, streams,
				new HostFileFinder(hostPaths),
				new HostFileMapper(fogConfig, autoClose),
				valConfig, new FragileValidator());
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		// configure
		autoClose = new AutoClose();
		fogConfig.put(MemoryFog.FILE_END_REACHED, new byte[] {'\0', 'E', 'O', 'F' });
		fogConfig.put(MemoryFog.FILE_NOT_FOUND,   new byte[] {'\0', '4', '0', '4' });
		fogConfig.put(MemoryFog.MAPPED_WRITABLE,  new byte[] { '?'}); // some omit it
		fogConfig.put(MemoryFog.NO_FILE_MAPPED,   new byte[] { 'h', 'e', 'a', 'p' });
		fileMapper = new HostFileMapper(fogConfig, autoClose);
	}

	/**
	 // test no mapping, no streams, nothing, produces nothing
	 * @throws Exception
	 */
	public void testZero() throws Exception {
		List<?> emptyResult = split(
				Collections.<MapInfo>emptyList(),
				Collections.<File>emptyList(),
				Collections.<MemData>emptyList(),
				manConfig,
				fogConfig,
				valConfig,
				autoClose);
		Assert.assertTrue(emptyResult.isEmpty());
	}

	/**
	 * test no mapping with streams produces nothing
	 * @throws Exception
 	 */
	public void testZeroMaps() throws Exception {
		try {
			split(
					Collections.<MapInfo>emptyList(),
					Collections.<File>emptyList(),
					Arrays.asList(
							new MockMemData(1024, 2048),
							new MockMemData(1000, 2000),
							new MockMemData(4096, 6144)
					),
					manConfig,
					fogConfig,
					noValConf,
					autoClose);
			Assert.assertFalse(true);
		} catch (NoSuchElementException ignored) {
			// must have been thrown: address not mapped
		}
	}

	/**
	 * with a universal mapping, produced areas mimic streams and gaps
	 * @throws Exception
	 */
	public void testAllCatchMaps() throws Exception {
		List<MemLoad> loads = split(
				Collections.singleton(new MockMapInfo(0, Long.MAX_VALUE, PermSet.LIBRARY)),
				Collections.<File>emptyList(),
				Arrays.asList(
						new MockMemData(1024, 2048),
						new MockMemData(1000, 2000),
						new MockMemData(4096, 6144)
				),
				manConfig,
				fogConfig,
				noValConf,
				autoClose);
		AreaTestUtil.assertEquals(loads.toString(), new long[][] {
				{   0, 1000},
				{1000, 1024},
				{1024, 2048}, // 1024 beats 1000
				{2048, 4096},
				{4096, 6144},
				{6144, Long.MAX_VALUE},
		}, loads);
		// test meaningful/reliable areas
		long[][] expected = new long[][] {
				{1000, 1024},
				{1024, 2048}, // 1024 beats 1000
				{4096, 6144},
		};
		Iterator<MemLoad> inRange = loads.iterator();
		for (long[] area : expected) {
			MemLoad result;
			do result = inRange.next(); while(!result.isReliable());
			assertEquals(area[0], result.getStartAddress());
			assertEquals(area[1], result.getEndAddress());
		}
		while(!inRange.hasNext()) Assert.assertFalse(inRange.next().isReliable());
	}

	/**
	 * mapped areas must not overlap. only captured data can overlap
	 * @throws Exception
	 */
	public void testMapInfoCanNotOverlap() throws Exception {
		try {
			split(
					Arrays.asList(
							new MockMapInfo(1024, 2048, PermSet.LIBRARY, "/system/lib/libc.so"),
							new MockMapInfo(1000, 2000, PermSet.RO_DATA, "/system/lib/libc.so"),
							new MockMapInfo(4096, 6144, PermSet.RW_DATA, "/system/lib/libc.so")
					),
					Collections.<File>emptyList(),
					Collections.<MemData>emptyList(),
					manConfig,
					fogConfig,
					valConfig,
					autoClose);
			Assert.assertFalse(true);
		} catch (IllegalStateException ignored) {
			// must have been thrown: overlapping mapped areas
		}
	}

	/**
	 * without streams and files, reliables and unreliables match mapping
	 * @throws Exception
	 */
	public void testMapInfoOnly() throws Exception {
		List<MemLoad> result = split(
				Arrays.asList(
						new MockMapInfo(1024, 2048, PermSet.LIBRARY,    0, "/system/lib/libc.so"),
						new MockMapInfo(2560, 3072, PermSet.RO_DATA, 1536, "/system/lib/libc.so"),
						new MockMapInfo(4096, 6144, PermSet.RW_DATA, 3072, "/system/lib/libc.so")
				),
				Collections.<File>emptyList(),
				Collections.<MemData>emptyList(),
				manConfig,
				fogConfig,
				valConfig,
				autoClose);
		AreaTestUtil.assertEquals(result.toString(), new long[][] {
				{1024, 2048},
				{2560, 3072},
				{4096, 6144},
		}, result);
	}

	// with a universal mapping, without streams, with files, reliables and unreliables match file length
	/**
	 * without streams and files, reliables and unreliables match mapping
	 * @throws Exception
	 */
	public void testMapsAndFilesOnly() throws Exception {
		List<MemLoad> result = createMapsAndFilesMapping();
		// [1024; 2048), [2560; 3072), [4096; 6144), [8192; 9216) is wrong; must split
		long base = 8192 + BASE_SIZE - 1600;
		AreaTestUtil.assertEquals(result.toString(), new long[][]{
				{1024, 2048},
				{2560, 3072},
				{4096, 6144},
				{8192, base}, // splits here because the file ends in 2048-
				{base, 9216},
		}, result);
		validateReliability(result,
				new boolean[]{true, true, false, true, false},
				new boolean[]{true, true, false, true, false});
	}

	/**
	 * without streams, with files, reliables and unreliables match mapping and file length
	 * @throws Exception
	 */
	public void testWritableMapsAndFiles() throws Exception {
		fogConfig.remove(MemoryFog.MAPPED_WRITABLE);
		List<MemLoad> result = createMapsAndFilesMapping();
		long libc = 4096 + LIBC_SIZE - 3072;
		long base = 8192 + BASE_SIZE - 1600;
		AreaTestUtil.assertEquals(result.toString(), new long[][] {
				{1024, 2048},
				{2560, 3072},
				{4096, libc}, // splits here because the file ends in 4096-
				{libc, 6144},
				{8192, base},
				{base, 9216},
		}, result);
		validateReliability(result,
				new boolean[] { true, true, true,  false, true, false },
				new boolean[] { true, true, false, false, true, false });
	}

	public List<MemLoad> createMapsAndFilesMapping() {
		return split(
				Arrays.asList(
						new MockMapInfo(1024, 2048, PermSet.LIBRARY,    0, "/system/lib/libc.so"),
						new MockMapInfo(2560, 3072, PermSet.RO_DATA, 1536, "/system/lib/libc.so"),
						new MockMapInfo(4096, 6144, PermSet.RW_DATA, 3072, "/system/lib/libc.so"),
						new MockMapInfo(8192, 9216, PermSet.PACKAGE, 1600, "/data/app/com.skype.research/base.apk")
				),
				Arrays.asList(systemRoot, appLibRoot),
				Collections.<MemData>emptyList(),
				manConfig,
				fogConfig,
				valConfig,
				autoClose);
	}

	public void validateReliability(List<MemLoad> result, boolean[] hostData, boolean[] reliable) {
		for (int i = 0; i < reliable.length; i++) {
			MemLoad load = result.get(i);
			Assert.assertEquals(load.toString(), hostData[i], load.isHostData());
			Assert.assertEquals(load.toString(), reliable[i], load.isReliable());
		}
	}
	
	// no files found -> no host chunks
	// files found but no files resolved -> no host chunks
	// make sure writables are controlled by memory fog configuration: host vs. fill
	// one or more fully heterogeneous configuration 
	// dumped area extends across multiple host chunks  

	@Override
	public void tearDown() throws Exception {
		autoClose.close();
		fogConfig.clear();
		super.tearDown();
	}
}
