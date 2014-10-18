/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.operators;

import com.microsoft.reef.io.network.exception.ParentDeadException;
import com.microsoft.tang.annotations.Name;

public interface GroupCommOperator {

  Class<? extends Name<String>> getOperName();

  Class<? extends Name<String>> getGroupName();

  void initialize() throws ParentDeadException;

  int getVersion();
}
