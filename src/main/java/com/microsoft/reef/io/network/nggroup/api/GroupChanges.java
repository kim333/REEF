/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api;

import com.microsoft.reef.annotations.audience.TaskSide;

/**
 * Represents the changes in Topology that happened in a communication group
 * from the last time the user asked for topology changes
 */
@TaskSide
public interface GroupChanges {

  boolean exist ();
}
