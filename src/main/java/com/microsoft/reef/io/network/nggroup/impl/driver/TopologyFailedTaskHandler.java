/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.logging.Logger;

import com.microsoft.reef.driver.task.FailedTask;
import com.microsoft.wake.EventHandler;

public class TopologyFailedTaskHandler implements EventHandler<FailedTask> {

  private static final Logger LOG = Logger.getLogger(TopologyFailedTaskHandler.class.getName());


  private final CommunicationGroupDriverImpl communicationGroupDriverImpl;

  public TopologyFailedTaskHandler (final CommunicationGroupDriverImpl communicationGroupDriverImpl) {
    this.communicationGroupDriverImpl = communicationGroupDriverImpl;
  }

  @Override
  public void onNext (final FailedTask failedTask) {
    final String failedTaskId = failedTask.getId();
    LOG.entering("TopologyFailedTaskHandler", "onNext", failedTaskId);
    communicationGroupDriverImpl.failTask(failedTaskId);
    LOG.exiting("TopologyFailedTaskHandler", "onNext", failedTaskId);
  }

}
