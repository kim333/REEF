/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.parameters.DriverIdentifier;
import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.GroupCommOperator;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.reef.io.network.nggroup.api.task.CommGroupNetworkHandler;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupServiceClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommNetworkHandler;
import com.microsoft.reef.io.network.nggroup.impl.GroupChangesCodec;
import com.microsoft.reef.io.network.nggroup.impl.GroupChangesImpl;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.CommunicationGroupName;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.OperatorName;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.SerializedOperConfigs;
import com.microsoft.reef.io.network.nggroup.impl.operators.Sender;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.ConfigurationSerializer;
import com.microsoft.wake.EStage;
import com.microsoft.wake.impl.ThreadPoolStage;

public class CommunicationGroupClientImpl implements CommunicationGroupServiceClient {
  private static final Logger LOG = Logger.getLogger(CommunicationGroupClientImpl.class.getName());

  private final GroupCommNetworkHandler groupCommNetworkHandler;
  private final Class<? extends Name<String>> groupName;
  private final Map<Class<? extends Name<String>>, GroupCommOperator> operators;
  private final Sender sender;

  private final String taskId;

  private final String driverId;

  private final CommGroupNetworkHandler commGroupNetworkHandler;

  private final AtomicBoolean init = new AtomicBoolean(false);

  @Inject
  public CommunicationGroupClientImpl (@Parameter(CommunicationGroupName.class) final String groupName,
                                       @Parameter(TaskConfigurationOptions.Identifier.class) final String taskId,
                                       @Parameter(DriverIdentifier.class) final String driverId,
                                       final GroupCommNetworkHandler groupCommNetworkHandler,
                                       @Parameter(SerializedOperConfigs.class) final Set<String> operatorConfigs,
                                       final ConfigurationSerializer configSerializer,
                                       final NetworkService<GroupCommunicationMessage> netService) {
    this.taskId = taskId;
    this.driverId = driverId;
    LOG.finest(groupName + " has GroupCommHandler-" + groupCommNetworkHandler.toString());
    this.groupName = Utils.getClass(groupName);
    this.groupCommNetworkHandler = groupCommNetworkHandler;
    this.sender = new Sender(netService);
    this.operators = new TreeMap<>(new Comparator<Class<? extends Name<String>>>() {

      @Override
      public int compare (final Class<? extends Name<String>> o1, final Class<? extends Name<String>> o2) {
        final String s1 = o1.getSimpleName();
        final String s2 = o2.getSimpleName();
        return s1.compareTo(s2);
      }
    });
    try {
      this.commGroupNetworkHandler = Tang.Factory.getTang().newInjector().getInstance(CommGroupNetworkHandler.class);
      this.groupCommNetworkHandler.register(this.groupName, commGroupNetworkHandler);

      for (final String operatorConfigStr : operatorConfigs) {

        final Configuration operatorConfig = configSerializer.fromString(operatorConfigStr);
        final Injector injector = Tang.Factory.getTang().newInjector(operatorConfig);

        injector.bindVolatileParameter(TaskConfigurationOptions.Identifier.class, taskId);
        injector.bindVolatileParameter(CommunicationGroupName.class, groupName);
        injector.bindVolatileInstance(CommGroupNetworkHandler.class, commGroupNetworkHandler);
        injector.bindVolatileInstance(NetworkService.class, netService);
        injector.bindVolatileInstance(CommunicationGroupServiceClient.class, this);

        final GroupCommOperator operator = injector.getInstance(GroupCommOperator.class);
        final String operName = injector.getNamedInstance(OperatorName.class);
        this.operators.put(Utils.getClass(operName), operator);
        LOG.finest(operName + " has CommGroupHandler-" + commGroupNetworkHandler.toString());
      }
    } catch (BindException | IOException e) {
      throw new RuntimeException("Unable to deserialize operator config", e);
    } catch (final InjectionException e) {
      throw new RuntimeException("Unable to deserialize operator config", e);
    }
  }

  @Override
  public Broadcast.Sender getBroadcastSender (final Class<? extends Name<String>> operatorName) {
    LOG.entering("CommunicationGroupClientImpl", "getBroadcastSender", new Object[] { getQualifiedName(),
                                                                                     Utils.simpleName(operatorName) });
    final GroupCommOperator op = operators.get(operatorName);
    if (!(op instanceof Broadcast.Sender)) {
      throw new RuntimeException("Configured operator is not a broadcast sender");
    }
    commGroupNetworkHandler.addTopologyElement(operatorName);
    LOG.exiting("CommunicationGroupClientImpl", "getBroadcastSender", getQualifiedName() + op);
    return (Broadcast.Sender) op;
  }

  @Override
  public Reduce.Receiver getReduceReceiver (final Class<? extends Name<String>> operatorName) {
    LOG.entering("CommunicationGroupClientImpl", "getReduceReceiver", new Object[] { getQualifiedName(),
                                                                                    Utils.simpleName(operatorName) });
    final GroupCommOperator op = operators.get(operatorName);
    if (!(op instanceof Reduce.Receiver)) {
      throw new RuntimeException("Configured operator is not a reduce receiver");
    }
    commGroupNetworkHandler.addTopologyElement(operatorName);
    LOG.exiting("CommunicationGroupClientImpl", "getReduceReceiver", getQualifiedName() + op);
    return (Reduce.Receiver) op;
  }

  @Override
  public Broadcast.Receiver getBroadcastReceiver (final Class<? extends Name<String>> operatorName) {
    LOG.entering("CommunicationGroupClientImpl", "getBroadcastReceiver", new Object[] { getQualifiedName(),
                                                                                       Utils.simpleName(operatorName) });
    final GroupCommOperator op = operators.get(operatorName);
    if (!(op instanceof Broadcast.Receiver)) {
      throw new RuntimeException("Configured operator is not a broadcast receiver");
    }
    commGroupNetworkHandler.addTopologyElement(operatorName);
    LOG.exiting("CommunicationGroupClientImpl", "getBroadcastReceiver", getQualifiedName() + op);
    return (Broadcast.Receiver) op;
  }

  @Override
  public Reduce.Sender getReduceSender (final Class<? extends Name<String>> operatorName) {
    LOG.entering("CommunicationGroupClientImpl", "getReduceSender", new Object[] { getQualifiedName(),
                                                                                  Utils.simpleName(operatorName) });
    final GroupCommOperator op = operators.get(operatorName);
    if (!(op instanceof Reduce.Sender)) {
      throw new RuntimeException("Configured operator is not a reduce sender");
    }
    commGroupNetworkHandler.addTopologyElement(operatorName);
    LOG.exiting("CommunicationGroupClientImpl", "getReduceSender", getQualifiedName() + op);
    return (Reduce.Sender) op;
  }

  @Override
  public void initialize () {
    LOG.entering("CommunicationGroupClientImpl", "initialize", getQualifiedName());
    if (init.compareAndSet(false, true)) {
      LOG.finest("CommGroup-" + groupName + " is initializing");
      final CountDownLatch initLatch = new CountDownLatch(operators.size());

      final InitHandler initHandler = new InitHandler(initLatch);
      final EStage<GroupCommOperator> initStage = new ThreadPoolStage<>(initHandler, operators.size());
      for (final GroupCommOperator op : operators.values()) {
        initStage.onNext(op);
      }

      try {
        initLatch.await();
      } catch (final InterruptedException e) {
        throw new RuntimeException("InterruptedException while waiting for initialization", e);
      }

      if (initHandler.getException() != null) {
        throw new RuntimeException(getQualifiedName() + "Parent dead. Current behavior is for the child to die too.");
      }
    }
    LOG.exiting("CommunicationGroupClientImpl", "initialize", getQualifiedName());
  }

  @Override
  public GroupChanges getTopologyChanges () {
    LOG.entering("CommunicationGroupClientImpl", "getTopologyChanges", getQualifiedName());
    for (final GroupCommOperator op : operators.values()) {
      final Class<? extends Name<String>> operName = op.getOperName();
      LOG.finest("Sending TopologyChanges msg to driver");
      try {
        sender.send(Utils.bldVersionedGCM(groupName, operName, Type.TopologyChanges, taskId, op.getVersion(), driverId,
                                          0, Utils.EmptyByteArr));
      } catch (final NetworkException e) {
        throw new RuntimeException("NetworkException while sending GetTopologyChanges", e);
      }
    }
    final Codec<GroupChanges> changesCodec = new GroupChangesCodec();
    final Map<Class<? extends Name<String>>, GroupChanges> perOpChanges = new HashMap<>();
    for (final GroupCommOperator op : operators.values()) {
      final Class<? extends Name<String>> operName = op.getOperName();
      final byte[] changes = commGroupNetworkHandler.waitForTopologyChanges(operName);
      perOpChanges.put(operName, changesCodec.decode(changes));
    }
    final GroupChanges retVal = mergeGroupChanges(perOpChanges);
    LOG.exiting("CommunicationGroupClientImpl", "getTopologyChanges", getQualifiedName() + retVal);
    return retVal;
  }

  /**
   * @param perOpChanges
   * @return
   */
  private GroupChanges mergeGroupChanges (final Map<Class<? extends Name<String>>, GroupChanges> perOpChanges) {
    LOG.entering("CommunicationGroupClientImpl", "mergeGroupChanges", new Object[] { getQualifiedName(), perOpChanges });
    boolean doChangesExist = false;
    for (final GroupChanges change : perOpChanges.values()) {
      if (change.exist()) {
        doChangesExist = true;
        break;
      }
    }
    final GroupChanges changes = new GroupChangesImpl(doChangesExist);
    LOG.exiting("CommunicationGroupClientImpl", "mergeGroupChanges", getQualifiedName() + changes);
    return changes;
  }

  @Override
  public void updateTopology () {
    LOG.entering("CommunicationGroupClientImpl", "updateTopology", getQualifiedName());
    for (final GroupCommOperator op : operators.values()) {
      final Class<? extends Name<String>> operName = op.getOperName();
      try {
        sender.send(Utils.bldVersionedGCM(groupName, operName, Type.UpdateTopology, taskId, op.getVersion(), driverId,
                                          0, Utils.EmptyByteArr));
      } catch (final NetworkException e) {
        throw new RuntimeException("NetworkException while sending UpdateTopology", e);
      }
    }
    for (final GroupCommOperator op : operators.values()) {
      final Class<? extends Name<String>> operName = op.getOperName();
      GroupCommunicationMessage msg;
      do {
         msg = commGroupNetworkHandler.waitForTopologyUpdate(operName);
      }while(!isMsgVersionOk(msg));
    }
    LOG.exiting("CommunicationGroupClientImpl", "updateTopology", getQualifiedName());
  }

  private boolean isMsgVersionOk (final GroupCommunicationMessage msg) {
    LOG.entering("CommunicationGroupClientImpl", "isMsgVersionOk", new Object[] { getQualifiedName(), msg });
    if (msg.hasVersion()) {
      final int msgVersion = msg.getVersion();
      final GroupCommOperator operator = operators.get(Utils.getClass(msg.getOperatorname()));
      final int nodeVersion = operator.getVersion();
      final boolean retVal;
      if (msgVersion < nodeVersion) {
        LOG.warning(getQualifiedName() + "Received a ver-" + msgVersion + " msg while expecting ver-" + nodeVersion
                    + ". Discarding msg");
        retVal = false;
      } else {
        retVal = true;
      }
      LOG.exiting("CommunicationGroupClientImpl", "isMsgVersionOk", Arrays.toString(new Object[] { retVal, getQualifiedName(), msg }));
      return retVal;
    } else {
      throw new RuntimeException(getQualifiedName() + "can only deal with versioned msgs");
    }
  }

  private String getQualifiedName () {
    return Utils.simpleName(groupName) + " ";
  }

  @Override
  public Class<? extends Name<String>> getName () {
    return groupName;
  }

}
