/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.quality;

/**
 * When an aggregate metric is evaluated, components may be reported individually.
 */
public interface CloseLook<K, V> {
	void onPartial(K memSeam, V partial);
}
