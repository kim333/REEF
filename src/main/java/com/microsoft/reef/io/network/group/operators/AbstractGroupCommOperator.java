/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.operators;

import com.microsoft.tang.annotations.Name;

public abstract class AbstractGroupCommOperator implements GroupCommOperator {

  @Override
  public Class<? extends Name<String>> getOperName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Class<? extends Name<String>> getGroupName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void initialize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getVersion() {
    throw new UnsupportedOperationException();
  }
}
