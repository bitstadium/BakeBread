/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.EnumSet;

/**
 * Test of the command line parser functionality.
 */
public class CmdLineTest extends TestCase {

	public void testRawValueAbbr() throws Exception {
		final DimensionSummary summary = new DimensionSummary();
		final String[] args = "-D 24x40".split("\\s+");
		ArgParser.parseCommandLine(args, summary);
		Assert.assertEquals(args[1], summary.getSummaryString());
	}
	
	public void testEnumListAbbr() throws Exception {
		final DimensionChoice choice = new DimensionChoice();
		final String[] args = "-Dxy".split("\\s+");
		ArgParser.parseCommandLine(args, choice);
		Assert.assertEquals(EnumSet.of(Dimension.ASIDE, Dimension.AHEAD), EnumSet.copyOf(choice.getOptions()));
	}
	
	public void testValueListAbbr() throws Exception {
		final DimensionValues values = new DimensionValues();
		final String[] args = "-Dz 0.25 -Dy 72".split("\\s+");
		ArgParser.parseCommandLine(args, values);
		Assert.assertEquals(0.00f, values.getVector()[0], 0.0001f);
		Assert.assertEquals(72.0f, values.getVector()[1], 0.0001f);
		Assert.assertEquals(0.25f, values.getVector()[2], 0.0001f);
	}
	
	public void testRawValueLong() throws Exception {
		final DimensionSummary summary = new DimensionSummary();
		final String[] args = "--dimension=24x40".split("\\s+");
		ArgParser.parseCommandLine(args, summary);
		Assert.assertEquals(args[0].split("=")[1], summary.getSummaryString());
	}
	
	public void testEnumListLong() throws Exception {
		final DimensionChoice choice = new DimensionChoice();
		final String[] args = "--dimension=aside,ahead".split("\\s+");
		ArgParser.parseCommandLine(args, choice);
		Assert.assertEquals(EnumSet.of(Dimension.ASIDE, Dimension.AHEAD), EnumSet.copyOf(choice.getOptions()));
	}
	
	public void testValueListLong() throws Exception {
		final DimensionValues values = new DimensionValues();
		final String[] args = "--dimension-above=0.25 --dimension-ahead=72".split("\\s+");
		ArgParser.parseCommandLine(args, values);
		Assert.assertEquals(0.00f, values.getVector()[0], 0.0001f);
		Assert.assertEquals(72.0f, values.getVector()[1], 0.0001f);
		Assert.assertEquals(0.25f, values.getVector()[2], 0.0001f);
	}
	
	// TODO add error cases (RecognitionException, ConfigurationException)
}
