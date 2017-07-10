/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

/**
 * Recognizes an Enum value from short and long option form.
 */
public interface EnumRouter<E extends Enum<E>> {
	E recognizeAbbr(char abbrForm) throws RecognitionException;
	E recognizeLong(String longForm) throws RecognitionException;
}
