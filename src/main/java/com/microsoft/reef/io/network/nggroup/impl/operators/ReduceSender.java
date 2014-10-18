/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.operators;

import java.util.ArrayList;
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
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.wake.EventHandler;

public class ReduceSender<T> implements Reduce.Sender<T>, EventHandler<GroupCommunicationMessage> {

  private static final Logger LOG = Logger.getLogger(ReduceSender.class.getName());

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
  public ReduceSender (@Parameter(CommunicationGroupName.class) final String groupName,
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
    return Utils.simpleName(groupName) + ":" + Utils.simpleName(operName) + ":" + version;
  }

  @Override
  public void onNext (final GroupCommunicationMessage msg) {
    topology.handle(msg);
  }

  @Override
  public void send (final T myData) throws NetworkException, InterruptedException {
    LOG.entering("ReduceSender", "send", new Object[] { this, myData });
    LOG.fine("I am " + this);

    if (init.compareAndSet(false, true)) {
      commGroupClient.initialize();
    }
    // I am an intermediate node or leaf.
    LOG.finest("Waiting for children");
    // Wait for children to send
    try {
      final T reducedValueOfChildren = topology.recvFromChildren(reduceFunction, dataCodec);
      final List<T> vals = new ArrayList<>(2);
      vals.add(myData);
      if (reducedValueOfChildren != null) {
        vals.add(reducedValueOfChildren);
      }
      final T reducedValue = reduceFunction.apply(vals);
      LOG.fine(this + " Sending local " + reducedValue + " to parent");
      topology.sendToParent(dataCodec.encode(reducedValue), Type.Reduce);
    } catch (final ParentDeadException e) {
      throw new RuntimeException("ParentDeadException", e);
    }
    LOG.exiting("ReduceSender", "send", Arrays.toString(new Object[] { this, myData }));
  }

  @Override
  public ReduceFunction<T> getReduceFunction () {
    return reduceFunction;
  }

}
