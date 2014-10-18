/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.operators;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.parameters.DriverIdentifier;
import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.exception.ParentDeadException;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.nggroup.api.task.CommGroupNetworkHandler;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupServiceClient;
import com.microsoft.reef.io.network.nggroup.api.task.OperatorTopology;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.CommunicationGroupName;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.DataCodec;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.OperatorName;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.TaskVersion;
import com.microsoft.reef.io.network.nggroup.impl.task.OperatorTopologyImpl;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.wake.EventHandler;

public class BroadcastReceiver<T> implements Broadcast.Receiver<T>, EventHandler<GroupCommunicationMessage> {

  private static final Logger LOG = Logger.getLogger(BroadcastReceiver.class.getName());

  private final Class<? extends Name<String>> groupName;
  private final Class<? extends Name<String>> operName;
  private final CommGroupNetworkHandler commGroupNetworkHandler;
  private final Codec<T> dataCodec;
  private final NetworkService<GroupCommunicationMessage> netService;
  private final Sender sender;

  private final OperatorTopology topology;

  private final AtomicBoolean init = new AtomicBoolean(false);

  private final CommunicationGroupServiceClient commGroupClient;

  private final int version;

  @Inject
  public BroadcastReceiver (@Parameter(CommunicationGroupName.class) final String groupName,
                            @Parameter(OperatorName.class) final String operName,
                            @Parameter(TaskConfigurationOptions.Identifier.class) final String selfId,
                            @Parameter(DataCodec.class) final Codec<T> dataCodec,
                            @Parameter(DriverIdentifier.class) final String driverId,
                            @Parameter(TaskVersion.class) final int version,
                            final CommGroupNetworkHandler commGroupNetworkHandler,
                            final NetworkService<GroupCommunicationMessage> netService,
                            final CommunicationGroupServiceClient commGroupClient) {
    super();
    this.version = version;
    LOG.finest(operName + " has CommGroupHandler-" + commGroupNetworkHandler.toString());
    this.groupName = Utils.getClass(groupName);
    this.operName = Utils.getClass(operName);
    this.dataCodec = dataCodec;
    this.commGroupNetworkHandler = commGroupNetworkHandler;
    this.netService = netService;
    this.sender = new Sender(this.netService);
    this.topology = new OperatorTopologyImpl(this.groupName, this.operName, selfId, driverId, sender, version);
    this.commGroupNetworkHandler.register(this.operName, this);
    this.commGroupClient = commGroupClient;
  }

  @Override
  public int getVersion () {
    return version;
  }

  @Override
  public void initialize () throws ParentDeadException {
    topology.initialize();
  }

  @Override
  public Class<? extends Name<String>> getOperName () {
    return operName;
  }

  @Override
  public Class<? extends Name<String>> getGroupName () {
    return groupName;
  }

  @Override
  public String toString () {
    return "BroadcastReceiver:" + Utils.simpleName(groupName) + ":" + Utils.simpleName(operName) + ":" + version;
  }

  @Override
  public void onNext (final GroupCommunicationMessage msg) {
    topology.handle(msg);
  }

  @Override
  public T receive () throws NetworkException, InterruptedException {
    LOG.entering("BroadcastReceiver", "receive", this);
    LOG.fine("I am " + this);

    if (init.compareAndSet(false, true)) {
      LOG.fine(this + " Communication group initializing");
      commGroupClient.initialize();
      LOG.fine(this + " Communication group initialized");
    }
    // I am an intermediate node or leaf.

    final T retVal;
    // Wait for parent to send
    LOG.fine(this + " Waiting to receive broadcast");
    byte[] data;
    try {
      data = topology.recvFromParent();
      // TODO: Should receive the identity element instead of null
      if (data == null) {
        LOG.fine(this + " Received null. Perhaps one of my ancestors is dead.");
        retVal = null;
      } else {
        LOG.finest("Using " + dataCodec.getClass().getSimpleName() + " as codec");
        retVal = dataCodec.decode(data);
        LOG.finest("Decoded msg successfully");
        LOG.fine(this + " Received: " + retVal);
        LOG.finest(this + " Sending to children.");
      }

      topology.sendToChildren(data, Type.Broadcast);
    } catch (final ParentDeadException e) {
      throw new RuntimeException("ParentDeadException", e);
    }
    LOG.exiting("BroadcastReceiver", "receive", Arrays.toString(new Object[] {retVal, this }));
    return retVal;
  }

}
