/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.util.quality;

/**
 * Aggregates objects of the respective type.
 */
public interface Considerable<T> {
	T consider(T consideration);
}
