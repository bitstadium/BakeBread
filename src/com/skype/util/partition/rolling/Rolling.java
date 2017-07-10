/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.rolling;

import java.nio.LongBuffer;

/**
 * Properties of a rolling hash type.
 */
public interface Rolling {
	Rolling getHashAlgorithm();

	int getWarmUpWindowBytes();
	int getWarmUpWindowSteps();
	int getWindowSizeInBytes();
	int getWindowSizeInSteps();
	int getSingleStepInBytes();
	
	class Utils {
		public static long hashIndexToWindowStart(Rolling hashType, int index) {
			return (index - hashType.getWarmUpWindowSteps()) * hashType.getSingleStepInBytes();
		}
		
		public static long hashIndexToAfterWindow(Rolling hashType, int index) {
			return hashIndexToWindowStart(hashType, index) + hashType.getWindowSizeInBytes();
		}
		
		public static int windowStartToHashIndex(Rolling hashType, long address) {
			return (int) ((address / hashType.getSingleStepInBytes()) + hashType.getWarmUpWindowSteps());
		}
		
		public static int afterWindowToHashIndex(Rolling hashType, long address) {
			return windowStartToHashIndex(hashType, address - hashType.getWindowSizeInBytes());
		}

		public static void checkInvariants(RollingHash rollingHash) {
			Rolling algorithm = rollingHash.getHashAlgorithm();
			LongBuffer hashes = rollingHash.computed();
			final int warmUpSteps = rollingHash.getWarmUpWindowSteps();
			if (warmUpSteps != hashes.position()) {
				throw new IllegalStateException("Hash buffer must be positioned after the warm-up window");
			}
			if (0 != hashIndexToWindowStart(algorithm, warmUpSteps)) {
				throw new IllegalStateException("First reliable window starting offset must be 0");
			}
			if (algorithm.getWindowSizeInBytes() != hashIndexToAfterWindow(algorithm, warmUpSteps)) {
				throw new IllegalStateException("First reliable window ending offset must be window size");
			}
		}

		public static long byteLength(RollingHash modified) {
			return hashIndexToAfterWindow(modified.getHashAlgorithm(), modified.computed().capacity());
			  // - Rolling.Utils.hashIndexToWindowStart(modified.getHashAlgorithm(), modified.computed().position());
		}

		public static void checkHashAlgorithm(Rolling original, Rolling modified) {
			if (!modified.getHashAlgorithm().equals(original.getHashAlgorithm())) {
				throw new IllegalArgumentException("Hash algorithm must match");
			}
		}
	}
}
