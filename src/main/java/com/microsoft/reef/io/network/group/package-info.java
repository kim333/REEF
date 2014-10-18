/**
 * Copyright (C) 2014 Microsoft Corporation
 */
/**
 * Provides MPI style Group Communication operators for collective communication
 * between tasks. These should be primarily used for any form of
 * task to task messaging along with the point to point communication
 * provided by {@link com.microsoft.reef.io.network.impl.NetworkService}
 *
 * The interfaces for the operators are in com.microsoft.reef.io.network.group.operators
 * The fluent way to describe these operators is available com.microsoft.reef.io.network.group.config
 * The implementation of these operators are available in com.microsoft.reef.io.network.group.impl
 * Currently only a basic implementation is available
 */
package com.microsoft.reef.io.network.group;
