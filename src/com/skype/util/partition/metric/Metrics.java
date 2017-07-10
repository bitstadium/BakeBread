/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.partition.metric;

/**
 * Various distance metrics.
 */
public enum Metrics implements Metric {
	BitCountMetric {
		@Override
		public int distance(long l, long r) {
			return Long.bitCount(l ^ r);
		}
	},

	ByteRadialMetric {
		@Override
		public int distance(long l, long r) {
			int t = 0;
			while ((l | r) != 0) {
				int c = (int) ((l & 0xff) - (r & 0xff));
				t += c * c;
				l >>>=8;
				r >>>=8;
			}
			return (int) Math.sqrt(t);
		}
	},
	
	ByteTotalMetric {
		@Override
		public int distance(long l, long r) {
			int t = 0;
			while ((l | r) != 0) {
				t += Math.abs((l & 0xff) - (r & 0xff));
				l >>>=8;
				r >>>=8;
			}
			return t;
		}
	},

	ByteXorBallMetric {
		@Override
		public int distance(long l, long r) {
			long x = l ^ r;
			x |= x >> 32;
			x |= x >> 16;
			x |= x >> 8;
			return (int) (0xff & x);
		}
	},

	NibbleRadialMetric {
		@Override
		public int distance(long l, long r) {
			int t = 0;
			while ((l | r) != 0) {
				t += Math.abs(((l & 0xf) - (r & 0xf)));
				l >>>=4;
				r >>>=4;
			}
			return t;
		}
	},

	ShortRadialMetric {
		@Override
		public int distance(long l, long r) {
			long t = 0;
			for (int i = 0; i < 4; ++i) {
				long c = ((l & 0xffff) - (r & 0xffff));
				t += c * c;
				l >>>=16;
				r >>>=16;
			}
			return (int) Math.sqrt(t);
		}
	}
}
