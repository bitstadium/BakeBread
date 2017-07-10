/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.model;

import java.nio.Buffer;

/**
 * Thread representation. Exposes signal info, stack and registers.
 */
public interface ThrInfo<G extends Buffer, F extends Buffer> {
	int getThreadId();
	boolean hasMainRegs();
	RegBank<G> getMainRegs();
	boolean hasMathRegs();
	RegBank<F> getMathRegs();
	boolean hasSigInfo();
	SigInfo getSigInfo(); // MOREINFO DmpInfo#getSigInfo(ThrInfo)?
}
