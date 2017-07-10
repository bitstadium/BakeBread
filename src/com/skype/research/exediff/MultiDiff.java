/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.exediff;

import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.exediff.frame.Einsteinian;
import com.skype.research.exediff.match.SeamBase;
import com.skype.research.exediff.match.SeamDiff;
import com.skype.research.exediff.present.DamageMeter;
import com.skype.research.exediff.present.HammingStat;
import com.skype.research.exediff.present.Hexualizer;
import com.skype.research.exediff.present.Quality;
import com.skype.util.partition.rolling.RollingHash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A stateful differencing facade (stores original data and hash).
 */
public class MultiDiff extends AbstractDiffFacade {
	
	private final AutoClose autoClose;
	String originalName;
	ByteBuffer bbOriginal;
	RollingHash rhOriginal;
	SeamBase base;

	public MultiDiff(AutoClose autoClose) {
		this.autoClose = autoClose;
	}

	public void setOriginal(File original) throws IOException {
		originalName = original.getCanonicalPath();
		bbOriginal = mapFile(original);
		rhOriginal = roller.index(bbOriginal);
		base = new SeamBase(rhOriginal, metric);
	}

	public void compare(File modified, PrintWriter writer) throws IOException {
		String modifiedName = modified.getCanonicalPath();
		ByteBuffer bbModified = mapFile(modified);
		RollingHash rhModified = roller.index(bbModified);
		SeamDiff diff = base.approximate(rhModified);
		if (greedyHeal) {
			diff.healGaps(diff.newBlankCost(), bbOriginal, bbModified);
		}
		final Einsteinian frame = new Einsteinian(
				bbOriginal, 0,
				bbModified, 0
		);
		HammingStat stat = new HammingStat(diff, frame);
		Quality quality = new DamageMeter(thresholds).assess(diff, stat);
		writer.printf(__KEY_VALUE_, QUALITY_INFO, quality);
		writer.println(INTRODUCTION);
		writer.println("--- " + originalName);
		writer.println("+++ " + modifiedName);
		if (quality.isGood()) {
			// write data
			Hexualizer hexualizer = new Hexualizer(writer, frame);
			// I don't want to suppress displaced display by now
			hexualizer.displayLineByteChanges(diff, stat, true);
		}
		writer.println();
		writer.flush();
	}

	private MappedByteBuffer mapFile(File file) throws IOException {
		FileInputStream stream = autoClose.register(new FileInputStream(file));
		return stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
	}
}
