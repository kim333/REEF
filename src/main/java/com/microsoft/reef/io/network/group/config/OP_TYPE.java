/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.config;

/**
 * The different operator types
 */
public enum OP_TYPE {
		SCATTER, 
		GATHER,
		REDUCE,
		BROADCAST,
		REDUCE_SCATTER, 
		ALL_GATHER, 
		ALL_REDUCE 
}
