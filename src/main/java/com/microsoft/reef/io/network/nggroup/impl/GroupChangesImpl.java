/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import com.microsoft.reef.io.network.nggroup.api.GroupChanges;

@Immutable
@ThreadSafe
public class GroupChangesImpl implements GroupChanges {

  private final boolean changes;

  public GroupChangesImpl(final boolean changes) {
    this.changes = changes;
  }

  @Override
  public boolean exist() {
    return changes;
  }

  @Override
  public String toString () {
    return "Changes: " + changes;
  }
}
