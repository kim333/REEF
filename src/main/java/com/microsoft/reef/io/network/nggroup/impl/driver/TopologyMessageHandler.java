/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.logging.Logger;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.wake.EventHandler;

public class TopologyMessageHandler implements EventHandler<GroupCommunicationMessage> {

  private static final Logger LOG = Logger.getLogger(TopologyMessageHandler.class.getName());


  private final CommunicationGroupDriverImpl communicationGroupDriverImpl;

  public TopologyMessageHandler (final CommunicationGroupDriverImpl communicationGroupDriverImpl) {
    this.communicationGroupDriverImpl = communicationGroupDriverImpl;
  }

  @Override
  public void onNext (final GroupCommunicationMessage msg) {
    LOG.entering("TopologyMessageHandler", "onNext", msg);
    communicationGroupDriverImpl.processMsg(msg);
    LOG.exiting("TopologyMessageHandler", "onNext", msg);
  }

}
