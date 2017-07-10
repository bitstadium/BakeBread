/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.bakebread.config;

/**
 *  -Ds, --display=stats       Show file statistic (size, stream count, etc.)
 *  -Dr, --display=roots       Root stream list (standalone top-level streams)
 *  -Dc, --display=crashed     Signal information and crashed thread context
 *  -Dt, --display=threads     All thread contexts (status and registers)
 *  -Dm, --display=mapping     Memory mapping information in /proc/PID/maps format
 *  -Dh, --display=hamming     Display approximate matching quality
 *  -Dv, --display=verbose     Display all displayable sections
 */
public enum DisplaySection {
	STATS, ROOTS, CRASHED, THREADS, MAPPING, HAMMING, VERBOSE, DEBUG
}
