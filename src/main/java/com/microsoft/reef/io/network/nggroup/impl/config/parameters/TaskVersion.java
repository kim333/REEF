/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

@NamedParameter(doc = "The version that this task is assigned")
public final class TaskVersion implements Name<Integer> {
  private TaskVersion () {
  }
}
