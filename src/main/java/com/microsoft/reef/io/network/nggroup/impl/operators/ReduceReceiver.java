/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.operators;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.parameters.DriverIdentifier;
import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.exception.ParentDeadException;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
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
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.Identifier;

public class ReduceReceiver<T> implements Reduce.Receiver<T>, EventHandler<GroupCommunicationMessage> {

  private static final Logger LOG = Logger.getLogger(ReduceReceiver.class.getName());

  private final Class<? extends Name<String>> groupName;
  private final Class<? extends Name<String>> operName;
  private final CommGroupNetworkHandler commGroupNetworkHandler;
  private final Codec<T> dataCodec;
  private final NetworkService<GroupCommunicationMessage> netService;
  private final Sender sender;
  private final ReduceFunction<T> reduceFunction;

  private final OperatorTopology topology;

  private final CommunicationGroupServiceClient commGroupClient;

  private final AtomicBoolean init = new AtomicBoolean(false);

  private final int version;

  @Inject
  public ReduceReceiver (@Parameter(CommunicationGroupName.class) final String groupName,
                         @Parameter(OperatorName.class) final String operName,
                         @Parameter(TaskConfigurationOptions.Identifier.class) final String selfId,
                         @Parameter(DataCodec.class) final Codec<T> dataCodec,
                         @Parameter(com.microsoft.reef.io.network.nggroup.impl.config.parameters.ReduceFunctionParam.class) final ReduceFunction<T> reduceFunction,
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
    this.reduceFunction = reduceFunction;
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
    return "ReduceReceiver:" + Utils.simpleName(groupName) + ":" + Utils.simpleName(operName) + ":" + version;
  }

  @Override
  public void onNext (final GroupCommunicationMessage msg) {
    topology.handle(msg);
  }

  @Override
  public T reduce () throws InterruptedException, NetworkException {
    LOG.entering("ReduceReceiver", "reduce", this);
    LOG.fine("I am " + this);

    if (init.compareAndSet(false, true)) {
      commGroupClient.initialize();
    }
    // I am root
    LOG.fine(this + " Waiting to receive reduced value");
    // Wait for children to send
    final T redVal;
    try {
      redVal = topology.recvFromChildren(reduceFunction, dataCodec);
    } catch (final ParentDeadException e) {
      throw new RuntimeException("ParentDeadException", e);
    }
    LOG.fine(this + " Received Reduced value: " + (redVal != null ? redVal : "NULL"));
    LOG.exiting("ReduceReceiver", "reduce", Arrays.toString(new Object[] { redVal }));
    return redVal;
  }

  @Override
  public T reduce (final List<? extends Identifier> order) throws InterruptedException, NetworkException {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReduceFunction<T> getReduceFunction () {
    return reduceFunction;
  }

}
