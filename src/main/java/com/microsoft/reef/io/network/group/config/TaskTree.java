/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.config;

import java.util.List;

import com.microsoft.wake.ComparableIdentifier;

public interface TaskTree {

  public static enum Status {
    UNSCHEDULED, SCHEDULED, COMPLETED, ANY;
    // ANY is to be used for search only. Its not an actual state
  }

  void add(ComparableIdentifier id);

  ComparableIdentifier parent(ComparableIdentifier id);

  ComparableIdentifier left(ComparableIdentifier id);

  ComparableIdentifier right(ComparableIdentifier id);

  List<ComparableIdentifier> neighbors(ComparableIdentifier id);

  List<ComparableIdentifier> children(ComparableIdentifier id);

  int childrenSupported(ComparableIdentifier taskId);

  void remove(ComparableIdentifier failedTaskId);

  List<ComparableIdentifier> scheduledChildren(ComparableIdentifier taskId);

  List<ComparableIdentifier> scheduledNeighbors(ComparableIdentifier taskId);

  void setStatus(ComparableIdentifier taskId, Status status);

  Status getStatus(ComparableIdentifier taskId);
}
