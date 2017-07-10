/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model.host;

import com.skype.research.bakebread.model.memory.MapInfo;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * File finder from a path string.
 */
public class HostFileFinder implements FileFinder {
	public static final String TARGET_PATH_SEP = "/";

	public interface Rule {
		String apply(String name);
	}
	
	public interface Case {
		boolean applies(String name);
	}
	
	public static final class Special implements Rule {
		private final Case condition;
		private final Rule delegate;

		protected Special(Case condition, Rule delegate) {
			this.condition = condition;
			this.delegate = delegate;
		}

		@Override
		public String apply(String name) {
			if (condition.applies(name)) {
				return delegate.apply(name);
			}
			return name;
		}
	}

	public static class PatternCase implements Case {
		private final Pattern pattern;

		public PatternCase(Pattern pattern) {
			this.pattern = pattern;
		}

		public PatternCase(String pattern) {
			this.pattern = Pattern.compile(pattern);
		}

		@Override
		public boolean applies(String name) {
			return pattern.matcher(name).matches();
		}
	}

	public static final Case MAYBE = new Case() {
		@Override
		public boolean applies(String name) {
			return name != null;
		}
	};

	public static final Rule CLEAR = new Rule() {
		@Override
		public String apply(String name) {
			return null;
		}
	};

	static final Rule TO_HOST_SEPARATORS = new Rule() {
		@Override
		public String apply(String name) {
			if (!"/".equals(File.separator)) {
				name = name.replace(TARGET_PATH_SEP, File.separator);
			}
			return name;
		}
	};
	
	static final Rule FORBID_WHITESPACES = new Special(new PatternCase("\\s"), CLEAR);
	
	static final Rule VALIDATE_FILE_PATH = new Rule() {
		@Override
		public String apply(String name) {
			try {
				Paths.get(name);
			} catch (InvalidPathException ipe) {
				return null;
			}
			return name;
		}
	};

	static final Rule MAKE_ROOT_RELATIVE = new Rule() {
		@Override
		public String apply(String name) {
			/// needs to be a target path separator
			return name.startsWith("/") ? "." + name : name;
		}
	};

	public static class Sequence implements Rule {
		private final Collection<Rule> rules = new LinkedList<>();
		
		public void addRule(Rule rule) {
			rules.add(new Special(MAYBE, rule));
		}

		@Override
		public String apply(String name) {
			for (Rule rule : rules) {
				name = rule.apply(name);
			}
			return name;
		}
	}
	
	public static class RuleSpec {
		private boolean forbidWhitespaces = true;

		public void setForbidWhitespaces(boolean forbidWhitespaces) {
			this.forbidWhitespaces = forbidWhitespaces;
		}

		public Rule build() {
			final Sequence sequence = new Sequence();
			sequence.addRule(TO_HOST_SEPARATORS);
			if (forbidWhitespaces) {
				sequence.addRule(FORBID_WHITESPACES);
			}
			sequence.addRule(VALIDATE_FILE_PATH);
			sequence.addRule(MAKE_ROOT_RELATIVE);
			return sequence;
		}
	}
	
	public static RuleSpec rules() {
		return new RuleSpec();
	}

	private final Collection<File> hostRoots = new HashSet<>();
	private final Rule preFilter;

	public HostFileFinder(File... hostRoots) {
		this(Arrays.asList(hostRoots));
	}

	public HostFileFinder(Collection<? extends File> hostRoots) {
		this(rules(), hostRoots);
	}
	
	public HostFileFinder(RuleSpec ruleSpec, File... hostRoots) {
		this(ruleSpec, Arrays.asList(hostRoots));
	}
	
	public HostFileFinder(RuleSpec ruleSpec, Collection<? extends File> hostRoots) {
		this.preFilter = ruleSpec.build();
		this.hostRoots.addAll(hostRoots);
	}
	
	@Override
	public File find(MapInfo info) {
		String name = preFilter.apply(info.getName());
		if (name != null) {
			for (File root : hostRoots) {
				File candidate = new File(root, name);
				if (candidate.isFile()) {
					return candidate;
				}
				candidate = new File(root, candidate.getName());
				if (candidate.isFile()) {
					return candidate;
				}
			}
		}
		return null;
	}
}
