/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.MemoryFog;
import com.skype.util.cmdline.ConfigurationException;
import com.skype.util.cmdline.OpenEnumOptions;
import com.skype.util.cmdline.RecognitionException;

import java.util.EnumMap;

/**
 * Mark words to fill unavailable or unreliable memory areas.
 *
 *  -Fr, --fill-run-time       [*] areas not mapped to any file (e.g. heap)
 *  -Fn, --fill-not-found      [*] mapped to a file not found in the provided path
 *  -Fe, --fill-file-end       [*] mapped to a file but extending beyond its end
 *  -Fu, --fill-unreliable         writable data (possibly modified after reading)
 */
public class FillingOptions extends OpenEnumOptions<MemoryFog> {
	final EnumMap<MemoryFog, byte[]> sequences = new EnumMap<MemoryFog, byte[]>(MemoryFog.class);

	public FillingOptions() {
		super('F', "fill", MemoryFog.class, null);
	}

	@Override
	public MemoryFog recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'r': return MemoryFog.NO_FILE_MAPPED;
			case 'n': return MemoryFog.FILE_NOT_FOUND;
			case 'e': return MemoryFog.FILE_END_REACHED;
			case 'u': return MemoryFog.MAPPED_WRITABLE;
			case 'p': return MemoryFog.PAGE_UNREADABLE;
			default:
				return null;
		}
	}

	@Override
	public MemoryFog recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "run-time":    return MemoryFog.NO_FILE_MAPPED;
			case "not-found":   return MemoryFog.FILE_NOT_FOUND;
			case "file-end":    return MemoryFog.FILE_END_REACHED;
			case "unreliable":  return MemoryFog.MAPPED_WRITABLE;
			case "private-pg":  return MemoryFog.PAGE_UNREADABLE;
			default:
				return null;
		}
	}

	@Override
	protected void onValueSet(MemoryFog key, String value) throws ConfigurationException {
		if (value.startsWith("0x") || value.startsWith("0X")) {
			value = value.substring(2);
		}
		if (value.length() > 8) {
			throw new ConfigurationException("char[" + value.length() + "]", key, value);
		}
		try {
			byte[] sequence;
			int iWord = Integer.parseInt(value, 16);
			int leads = Integer.numberOfLeadingZeros(iWord);
			if (leads >= 24) {
				sequence = new byte[] {(byte) iWord}; 
			} else if (leads >= 16) {
				sequence = new byte[] {(byte) iWord, (byte) (iWord >> 8)};
			} else {
				sequence = new byte[] {(byte) iWord, (byte) (iWord >> 8), 
								(byte) (iWord >> 16), (byte) (iWord >> 24)};
			}
			sequences.put(key, sequence);
		} catch (NumberFormatException nfe) {
			throw new ConfigurationException(nfe.getMessage(), key, value);
		}
	}

	public byte[] getFilling(MemoryFog fogType) {
		return sequences.containsKey(fogType) ? sequences.get(fogType) : fogType.getDefaultFill();
	}
}
