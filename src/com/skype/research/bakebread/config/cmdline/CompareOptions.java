/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config.cmdline;

import com.skype.research.bakebread.config.BitExactValidation;
import com.skype.util.cmdline.EnumListOptions;
import com.skype.util.cmdline.RecognitionException;

/**
 * Bit-exact comparison command line options.
 */
public class CompareOptions extends EnumListOptions<BitExactValidation> {
	public CompareOptions() {
		super('V', "validate", BitExactValidation.class);
	}

	@Override
	public BitExactValidation recognizeAbbr(char abbrForm) throws RecognitionException {
		switch (abbrForm) {
			case 'd':
				return BitExactValidation.DUMP_INTERNAL;
			case 'h':
				return BitExactValidation.HOST_AND_DUMP;
			case 'l':
				return BitExactValidation.LOOSEN_CHECKS;
			case 's':
				return BitExactValidation.STRICT_CHECKS;
			default:
				return null;
		}
	}

	@Override
	public BitExactValidation recognizeLong(String longForm) throws RecognitionException {
		switch (longForm) {
			case "dump":
				return BitExactValidation.DUMP_INTERNAL;
			case "host":
				return BitExactValidation.HOST_AND_DUMP;
			case "relax":
				return BitExactValidation.LOOSEN_CHECKS;
			case "strict":
				return BitExactValidation.STRICT_CHECKS;
			default:
				return null;
		}
	}
}
