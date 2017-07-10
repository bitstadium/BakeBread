/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.sparse.scanner;

/**
 * File system type to recognize.
 */
public enum ScannerType {
	EXT4 {
		@Override
		public Scanner createScanner() {
			return new Ext4Scanner();
		}
	},
	;
	
	public abstract Scanner createScanner();
}
