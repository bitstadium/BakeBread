/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread;

import com.skype.research.bakebread.config.BitExactValidation;
import com.skype.research.bakebread.config.Configuration;
import com.skype.research.bakebread.config.Conversion;
import com.skype.research.bakebread.config.DefaultConfiguration;
import com.skype.research.bakebread.config.DisplaySection;
import com.skype.research.bakebread.config.ModuleAnalysis;
import com.skype.research.bakebread.config.cmdline.CmdLineConfiguration;
import com.skype.research.bakebread.coredump.ELF;
import com.skype.research.bakebread.io.AutoClose;
import com.skype.research.bakebread.minidump.DirectoryEntry;
import com.skype.research.bakebread.minidump.Header;
import com.skype.research.bakebread.minidump.MemoryStream;
import com.skype.research.bakebread.minidump.MiniDumpFromFile;
import com.skype.research.bakebread.minidump.ThreadContextual;
import com.skype.research.bakebread.minidump.ThreadStream;
import com.skype.research.bakebread.minidump.streams.Microsoft;
import com.skype.research.bakebread.minidump.streams.StreamType;
import com.skype.research.bakebread.model.analysis.FlexibleValidator;
import com.skype.research.bakebread.model.analysis.FragileValidator;
import com.skype.research.bakebread.model.analysis.Splitter;
import com.skype.research.bakebread.model.analysis.Validator;
import com.skype.research.bakebread.model.arch.ElfAnalyzer;
import com.skype.research.bakebread.model.host.HostFileFinder;
import com.skype.research.bakebread.model.host.HostFileMapper;
import com.skype.research.bakebread.model.memory.MapInfo;
import com.skype.research.bakebread.model.memory.MemLoad;
import com.skype.research.bakebread.nio.BufferAdapter;
import com.skype.research.exediff.bleach.ArmBleach;
import com.skype.research.exediff.bleach.DataBleach;
import com.skype.research.exediff.bleach.WeakThumbBleach;
import com.skype.util.partition.metric.Metrics;
import com.skype.util.partition.rolling.HashRollers;
import sun.misc.IOUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws IOException {
	    // we are the primary entry point. but we may want to introduce more symmetric routing.
	    if (args.length > 0 && "-0".equals(args[0])) {
		    com.skype.research.sparse.Main.main(Arrays.copyOfRange(args, 1, args.length));
		    return;
	    }
	    if (args.length > 0 && "-1".equals(args[0])) {
		    com.skype.research.exediff.Main.main(Arrays.copyOfRange(args, 1, args.length));
		    return;
	    }
		final PrintStream printStream = System.out;
	    showHelp(printStream, "BANNER.txt");
	    if (args.length == 0) {
		    showHelp(printStream, "README.txt");
		    System.exit(1);
	    } else {
		    try {
			    Configuration configuration = new CmdLineConfiguration(args);
			    if (configuration.hasNothingToDo()) {
				    final File dumpFile = configuration.getDumpFile();
				    if (dumpFile == null) {
					    showHelp(printStream, "README.txt");
					    printStream.println();
					    printStream.println("Error: no file specified to process!");
					    System.exit(1);
				    } else {
					    showSimpleSplitExample(printStream, dumpFile);
					    configuration = new DefaultConfiguration(dumpFile);
				    }
			    }
			    runTask(configuration, printStream);
		    } catch (Exception parseException) {
			    parseException.printStackTrace(printStream);
			    System.exit(3);
		    }
	    }
    }

	private static void showSimpleSplitExample(PrintStream printStream, File dumpFile) {
		printStream.println("Using default configuration. Run "
				+ "\n\n java -jar bakebread.jar -CS <out-folder> " + dumpFile.getName()
				+ "\n\n to actually split the dump.\n");
	}

	private static void showHelp(PrintStream printStream, String name) throws IOException {
		printStream.write(IOUtils.readFully(Main.class.getResourceAsStream(name), Short.MAX_VALUE, false));
	}

	private static void runTask(Configuration configuration, PrintStream printStream) throws IOException {
		final AutoClose autoClose = new AutoClose();
		final File dumpFile = configuration.getDumpFile();
		final String fileName = dumpFile.getName();
		MiniDumpFromFile miniDump = autoClose.register(new MiniDumpFromFile(dumpFile));
		miniDump.open();
		if (configuration.isDisplaySectionEnabled(DisplaySection.STATS)) {
			Header header = miniDump.getHeader();
			printStream.println("stream_count = " + header.getStreamCount());
			printStream.println("directory_at = " + header.getStreamDirectoryRva().getVirtualAddress());
			printStream.println("gmt_datetime = " + header.getDateTime().toGMTString());
			printStream.println("crash_extras = " + Long.toBinaryString(header.getFlags()));
			printStream.println("total_length = " + dumpFile.length());
			printStream.println();
		}
		File targetFolder = null;
		if (configuration.shouldConvertTo(Conversion.SPLIT_DIR)) {
			targetFolder = configuration.getConversionTarget(Conversion.SPLIT_DIR);
			if (!targetFolder.exists()) {
				//noinspection ResultOfMethodCallIgnored
				targetFolder.mkdirs();
			}
			if (!targetFolder.isDirectory()) {
				final String path = targetFolder.getAbsolutePath();
				printStream.println("Target folder " + path + " cannot be created.");
				printStream.println();
				throw new FileNotFoundException(path);
			}
		}
		for (DirectoryEntry directoryEntry : miniDump.getDirectory()) {
			final StreamType streamType = directoryEntry.getStreamType();
			if (configuration.shouldConvertTo(Conversion.SPLIT_DIR)) {
				directoryEntry.getLocationDescription().copyOut(
						dumpFile,
						new File(targetFolder, fileName + "." + streamType)
				);
			}
			if (configuration.isDisplaySectionEnabled(DisplaySection.ROOTS)) {
				if (miniDump.getTopLevelStreamTypes().contains(streamType)) {
					printStream.println("root stream: " + streamType
							+ "[" + directoryEntry.getLocationDescription().getDataSize() + "]");
				}
			}
		}
		printStream.println();
		if (configuration.shouldConvertTo(Conversion.SPLIT_DIR)) {
			for (MemoryStream memoryStream : miniDump.getOtherStreams()) {
				final String hexAddress = String.format("0x%08x", memoryStream.getStartAddress());
				memoryStream.getLocationDescription().copyOut(
						dumpFile,
						new File(targetFolder, fileName + "." + Microsoft.MemoryListStream +
								"." + hexAddress)
				);
			}
			for (ThreadStream threadStream : miniDump.getThreadStreams()) {
				final MemoryStream memoryStream = threadStream.getStack();
				final String hexAddress = String.format("0x%08x", memoryStream.getStartAddress());
				memoryStream.getLocationDescription().copyOut(
						dumpFile,
						new File(targetFolder, fileName + "." + Microsoft.ThreadListStream +
								"." + threadStream.getThreadId() + "." + hexAddress)
				);
			}
		}
		if (configuration.isDisplaySectionEnabled(DisplaySection.CRASHED)) {
			printStream.println(miniDump.getSignalStream());
			printStream.println();
			if (!configuration.isDisplaySectionEnabled(DisplaySection.THREADS)) {
				// FIXME always the first one ATM, but should look up by index
				printThread(printStream, miniDump.getThreadStreams().iterator().next());
			}
		}

		if (configuration.isDisplaySectionEnabled(DisplaySection.THREADS)) {
			for (ThreadContextual threadStream : miniDump.getThreadStreams()) {
				printThread(printStream, threadStream);
			}
		}

		if (configuration.isDisplaySectionEnabled(DisplaySection.MAPPING)) {
			for (MapInfo memoryMapping : miniDump.getMemMap()) {
				printStream.println(memoryMapping);
			}
		}

		// LinuxDSODebug
		// ProcAuxVStream
		// CPUInfo - google, SystemInfo - MSFT

		// core dump
		if (configuration.shouldConvertTo(Conversion.CORE_FILE)) {
			// TODO: add RelRo to module analysis or validation
			
			File coreFile = configuration.getConversionTarget(Conversion.CORE_FILE);

			printStream.println();
			printStream.println("Writing corefile: " + coreFile.getCanonicalPath());

			final ELF elf = new ELF(ELF.Preset.ANDROID_32, ELF.Type.CORE);
			final ElfAnalyzer elfAn = new ElfAnalyzer(configuration, autoClose); // incoming

			elf.addNotes(miniDump);
			HostFileFinder finder = new HostFileFinder(configuration.getModulePaths());
			HostFileMapper mapper = new HostFileMapper(configuration, autoClose);
			if (configuration.isModuleAnalysisEnabled(ModuleAnalysis.ELF)) {
				mapper.addAnalyzer(elfAn);
			}
			final Validator validator;
			if (configuration.isValidationTypeEnabled(BitExactValidation.STRICT_CHECKS)) {
				validator = new FragileValidator();
			} else {
				final boolean relax = configuration.isValidationTypeEnabled(BitExactValidation.LOOSEN_CHECKS);
				final FlexibleValidator flexValidator = new FlexibleValidator(relax);
				flexValidator.setRoller(HashRollers.TAMIEN_HALFWORD);
				flexValidator.setMetric(Metrics.ShortRadialMetric);
				flexValidator.setGreedyHeal(true);
				flexValidator.setThresholds(configuration);
				if (configuration.isModuleAnalysisEnabled(ModuleAnalysis.ARM)) {
					flexValidator.addBleach(new ArmBleach());
					flexValidator.addBleach(new WeakThumbBleach());
				}
				flexValidator.addBleach(new DataBleach<>(BufferAdapter.Stateless.INT_BAD));
				flexValidator.setSummarize(configuration.isDisplaySectionEnabled(DisplaySection.HAMMING));
				flexValidator.setShowCosts(configuration.isDisplaySectionEnabled(DisplaySection.DEBUG));
				flexValidator.setDiffplay(printStream);
				flexValidator.setDiffFile(companionFile(coreFile, "diff"));
				validator = flexValidator;
			}
			List<MemLoad> memLoads = Splitter.split(miniDump.getMemMap(), miniDump.getMemDmp(),
					finder, mapper, configuration, validator);
			if (configuration.isDisplaySectionEnabled(DisplaySection.DEBUG)) {
				for (MemLoad memLoad : memLoads) {
					printStream.println(memLoad);
				}
			}
			elf.addLoads(memLoads, configuration);
			FileOutputStream fos = new FileOutputStream(coreFile);
			DataOutputStream dos = autoClose.register(new DataOutputStream(fos));
			elf.writeExternal(dos, fos.getChannel());
			
			if (configuration.isModuleAnalysisEnabled(ModuleAnalysis.ELF)) {
				File gdbScript = companionFile(coreFile, "gdb");
				printStream.println("Courtesy script: " + gdbScript.getCanonicalPath());
				try (FileWriter fileWriter = new FileWriter(gdbScript)) {
					PrintWriter printWriter = new PrintWriter(fileWriter, true);
					// printWriter.println("set pagination off"); // not so helpful if backtrace infinitely loops
					// printWriter.println("set print elements 0"); // similar warning
					for (Map.Entry<File, Long> hostModel : elfAn.getTextOffsets().entrySet()) {
						printWriter.println("add-symbol-file " + prettyFile(hostModel.getKey()) 
								+ " 0x" + Long.toHexString(hostModel.getValue())); // MOREINFO -s other sections?
					}
					int[] threadIds = miniDump.getThreadIds();
					Arrays.sort(threadIds); // we copied out
					int crashedPos = Arrays.binarySearch(threadIds, miniDump.getCrashedThreadId()) + 1;
					printWriter.println("thread " + crashedPos);
					printWriter.println("bt");
					printWriter.flush();
					printStream.println("Success! To analyze the dump, run");
					printStream.println();
					printStream.println(
							"gdb -c " + coreFile.getCanonicalPath() 
							+ " -x " + gdbScript.getCanonicalPath());
					printStream.println();
				}
			}
		}
		autoClose.close();
	}

	private static File companionFile(File coreFile, String extension) {
		// MOREINFO check existence?
		return new File(coreFile.getParent(), coreFile.getName() + "." + extension);
	}

	private static String prettyFile(File file) {
		// todo relativize here too
		String path = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			path = path.replace("\\", "\\\\");
		}
		return path;
	}

	private static void printThread(PrintStream printStream, ThreadContextual threadStream) {
		printStream.println(threadStream);
		printStream.println(threadStream.getThreadContext());
		printStream.println();
	}
}
