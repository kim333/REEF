/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.task;

import com.microsoft.reef.annotations.audience.Private;
import com.microsoft.tang.annotations.DefaultImplementation;

@Private
@DefaultImplementation(value = com.microsoft.reef.io.network.nggroup.impl.task.CommunicationGroupClientImpl.class)
public interface CommunicationGroupServiceClient extends CommunicationGroupClient {
  /**
   * Should not be used by user code
   * Used for initialization of the
   * communication group
   */
  void initialize();
}
