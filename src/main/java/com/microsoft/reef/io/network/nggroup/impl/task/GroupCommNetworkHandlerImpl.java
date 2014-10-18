/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.io.network.Message;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommNetworkHandler;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.tang.annotations.Name;
import com.microsoft.wake.EventHandler;

public class GroupCommNetworkHandlerImpl implements GroupCommNetworkHandler {

  private static final Logger LOG = Logger.getLogger(GroupCommNetworkHandlerImpl.class.getName());

  private final Map<Class<? extends Name<String>>, EventHandler<GroupCommunicationMessage>> commGroupHandlers = new ConcurrentHashMap<>();

  @Inject
  public GroupCommNetworkHandlerImpl () {
  }

  @Override
  public void onNext (final Message<GroupCommunicationMessage> mesg) {
    LOG.entering("GroupCommNetworkHandlerImpl", "onNext", mesg);
    final Iterator<GroupCommunicationMessage> iter = mesg.getData().iterator();
    final GroupCommunicationMessage msg = iter.hasNext() ? iter.next() : null;
    try {
      final Class<? extends Name<String>> groupName = (Class<? extends Name<String>>) Class.forName(msg.getGroupname());
      commGroupHandlers.get(groupName).onNext(msg);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("GroupName not found", e);
    }
    LOG.exiting("GroupCommNetworkHandlerImpl", "onNext", mesg);
  }

  @Override
  public void register (final Class<? extends Name<String>> groupName,
                        final EventHandler<GroupCommunicationMessage> commGroupNetworkHandler) {
    LOG.entering("GroupCommNetworkHandlerImpl", "register", new Object[] { groupName,
                                                                          commGroupNetworkHandler });
    commGroupHandlers.put(groupName, commGroupNetworkHandler);
    LOG.exiting("GroupCommNetworkHandlerImpl", "register", Arrays.toString(new Object[] { groupName,
                                                                           commGroupNetworkHandler }));
  }

}
