/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.io.network.nggroup.api.task.CommGroupNetworkHandler;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.tang.annotations.Name;
import com.microsoft.wake.EventHandler;

public class CommGroupNetworkHandlerImpl implements
    CommGroupNetworkHandler {

  private static final Logger LOG = Logger.getLogger(CommGroupNetworkHandlerImpl.class.getName());

  private final Map<Class<? extends Name<String>>, EventHandler<GroupCommunicationMessage>> operHandlers = new ConcurrentHashMap<>();
  private final Map<Class<? extends Name<String>>, BlockingQueue<GroupCommunicationMessage>> topologyNotifications = new ConcurrentHashMap<>();

  @Inject
  public CommGroupNetworkHandlerImpl () {
  }

  @Override
  public void register (final Class<? extends Name<String>> operName,
                        final EventHandler<GroupCommunicationMessage> operHandler) {
    LOG.entering("CommGroupNetworkHandlerImpl", "register", new Object[] { Utils.simpleName(operName), operHandler });
    operHandlers.put(operName, operHandler);
    LOG.exiting("CommGroupNetworkHandlerImpl", "register", Arrays.toString(new Object[] { Utils.simpleName(operName), operHandler }));
  }

  @Override
  public void addTopologyElement (final Class<? extends Name<String>> operName) {
    LOG.entering("CommGroupNetworkHandlerImpl", "addTopologyElement", Utils.simpleName(operName));
    LOG.finest("Creating LBQ for " + operName);
    topologyNotifications.put(operName, new LinkedBlockingQueue<GroupCommunicationMessage>());
    LOG.exiting("CommGroupNetworkHandlerImpl", "addTopologyElement", Utils.simpleName(operName));
  }

  @Override
  public void onNext (final GroupCommunicationMessage msg) {
    LOG.entering("CommGroupNetworkHandlerImpl", "onNext", msg);
    final Class<? extends Name<String>> operName = Utils.getClass(msg.getOperatorname());
    if (msg.getType() == Type.TopologyUpdated || msg.getType() == Type.TopologyChanges) {
      topologyNotifications.get(operName).add(msg);
    } else {
      operHandlers.get(operName).onNext(msg);
    }
    LOG.exiting("CommGroupNetworkHandlerImpl", "onNext", msg);
  }

  @Override
  public byte[] waitForTopologyChanges (final Class<? extends Name<String>> operName) {
    LOG.entering("CommGroupNetworkHandlerImpl", "waitForTopologyChanges", Utils.simpleName(operName));
    try {
      final byte[] retVal = Utils.getData(topologyNotifications.get(operName).take());
      LOG.exiting("CommGroupNetworkHandlerImpl", "waitForTopologyChanges", retVal);
      return retVal;
    } catch (final InterruptedException e) {
      throw new RuntimeException("InterruptedException while waiting for topology update of "
                                 + operName.getSimpleName(), e);
    }
  }

  @Override
  public GroupCommunicationMessage waitForTopologyUpdate (final Class<? extends Name<String>> operName) {
    LOG.entering("CommGroupNetworkHandlerImpl", "waitForTopologyUpdate", Utils.simpleName(operName));
    try {
      final GroupCommunicationMessage retVal = topologyNotifications.get(operName).take();
      LOG.exiting("CommGroupNetworkHandlerImpl", "waitForTopologyUpdate", retVal);
      return retVal;
    } catch (final InterruptedException e) {
      throw new RuntimeException("InterruptedException while waiting for topology update of "
                                 + operName.getSimpleName(), e);
    }
  }

}
