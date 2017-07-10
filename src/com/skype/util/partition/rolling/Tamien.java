/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.rolling;

/**
 * The Tamien window hash. R&D by Skype Research.
 * 
 * A window hash based off a 64-bit Bloom filter.
 * See: https://en.wikipedia.org/wiki/Bloom_filter
 * Good for matching slightly deviating machine code;
 * the default 2:1 bit count combination "considers" 
 * compiler instruction reordering a lesser change than
 * a single word edit, in turn a lesser change than 
 * a range shift (insertion or deletion).
 * 
 * Further domain awareness may be applied by sending the
 * "most significant" bit-part of the instruction word
 * separately (before or after) from the entire one.
 * 
 * Additional "position bleach" may be achieved by erasing
 * the argument part of static procedure call instructions,
 * such as BL (branch-link) on ARM - to evaluate only local
 * method code changes rather than the module composition
 * in general.
 * 
 * Tamien is a Native American tribe, a location in San Jose
 * and a Caltrain station, as are Blossom Hill and Capitol:
 * http://onlineexhibits.historysanjose.org/dairyhill/people/tamien.html
 * http://www.caltrain.com/stations/tamienstation.html
 * http://www.caltrain.com/stations/capitolstation.html
 * http://www.caltrain.com/stations/blossomhillstation.html
 * 
 * The position-independent part is called "Blossom" to honor 
 * the heritage of the original Bloom filter inventor. "Capitol",
 * the position-dependent part, is just the next station 
 * on the way to Tamien.
 */
public class Tamien {
	private final int plainBits; // how many bits a single value adds
	private final int whirlBits; // how many bits a single value adds
	private final int[] history;
	private int historian, myth;

	private long blossom, capitol;

	public Tamien(int plainBits, int whirlBits, int window) {
		this.plainBits = plainBits;
		this.whirlBits = whirlBits;
		this.history = new int[window];
		myth = -history.length;
	}

	private static int knuth(int value, int modulo) {
		// https://www.cs.hmc.edu/~geoff/classes/hmc.cs070.200101/homework10/hashfuncs.html
		return value * (value + 3) % modulo;
	}

	private long getFamily(int value, int bits, int modulo) {
		final int enough = value + bits;
		long family = 0;
		while (value < enough) {
			family |= 1L << knuth(value++, modulo);
		}
		return family;
	}

	private static int fold(long value) {
		return (int) (value ^ (value >>> 32));
	}

	public void put(int value) {
		// window apply
		if (history.length > 0) {
			if (myth >= 0) {
				remove(history[historian], history.length);
			} else {
				// this complex nested if() is here only to avoid rollover 
				myth++;
			}
		}
		capitol ^= getFamily(value, plainBits, 64);
		blossom ^= getFamily(~value, whirlBits, 32);
		blossom = blossom << 1;
		// window put
		if (history.length > 0) {
			history[historian++] = value;
			historian %= history.length;
		}
	}
	
	public void remove(int value, int age) {
		long family;
		capitol ^= getFamily(value, plainBits, 64);
		if (age < 64) {
			long word = getFamily(~value, whirlBits, 32);
			family = word << age;
			blossom ^= family;
		}
	}

	public void put(long value) {
		put(fold(value));
	}

	public void put(Object obj) {
		put(obj.hashCode());
	}
	
	public void remove(long value, int age) {
		remove(fold(value), age);
	}

	public void remove(Object obj, int age) {
		remove(obj.hashCode(), age);
	}
	
	public void clear() {
		myth = -history.length;
		blossom = 0;
		capitol = 0;
	}
	
	public boolean isWarmingUp() {
		return history.length != 0 && myth < 0;
	} 
	
	public long getTamien() {
		return blossom ^ capitol;
	}
}
