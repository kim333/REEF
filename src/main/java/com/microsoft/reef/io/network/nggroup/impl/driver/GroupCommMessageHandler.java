/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.utils.BroadcastingEventHandler;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.tang.annotations.Name;
import com.microsoft.wake.EventHandler;

/**
 * The network handler for the group communcation service on the driver side
 */
public class GroupCommMessageHandler implements EventHandler<GroupCommunicationMessage> {

  private static final Logger LOG = Logger.getLogger(GroupCommMessageHandler.class.getName());

  private final Map<Class<? extends Name<String>>, BroadcastingEventHandler<GroupCommunicationMessage>>
    commGroupMessageHandlers = new HashMap<>();

  public void addHandler (final Class<? extends Name<String>> groupName,
                          final BroadcastingEventHandler<GroupCommunicationMessage> handler) {
    LOG.entering("GroupCommMessageHandler", "addHandler", new Object[] { Utils.simpleName(groupName), handler });
    commGroupMessageHandlers.put(groupName, handler);
    LOG.exiting("GroupCommMessageHandler", "addHandler", Utils.simpleName(groupName));
  }

  @Override
  public void onNext (final GroupCommunicationMessage msg) {
    LOG.entering("GroupCommMessageHandler", "onNext", msg);
    final Class<? extends Name<String>> groupName = Utils.getClass(msg.getGroupname());
    commGroupMessageHandlers.get(groupName).onNext(msg);
    LOG.exiting("GroupCommMessageHandler", "onNext", msg);
  }
}
