/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.cmdline;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Cannot parse any further
 */
public class RecognitionException extends ParseException {
	final Leftover leftover;
	
	public RecognitionException(String s, Iterator<String> iterator) {
		this(s, new Leftover(iterator));
	}

	private RecognitionException(String s, Leftover leftover) {
		super(s, leftover.getPos());
		this.leftover = leftover;
	}
	
	public String getMessage() {
		return super.getMessage() + leftover.args;
	}
	
	private static class Leftover {
		final List<String> args;

		private Leftover(Iterator<String> iterator) {
			List<String> list = new ArrayList<>();
			while (iterator.hasNext()) {
				list.add(iterator.next());
			}
			args = list;
		}

		public int getPos() {
			return - args.size();
		}
	}
}
