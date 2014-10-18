/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import com.microsoft.reef.annotations.audience.DriverSide;
import com.microsoft.reef.annotations.audience.Private;
import com.microsoft.reef.driver.evaluator.FailedEvaluator;
import com.microsoft.reef.driver.parameters.DriverIdentifier;
import com.microsoft.reef.driver.task.FailedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.io.network.nggroup.api.config.OperatorSpec;
import com.microsoft.reef.io.network.nggroup.api.driver.CommunicationGroupDriver;
import com.microsoft.reef.io.network.nggroup.api.driver.Topology;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.config.BroadcastOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.ReduceOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.CommunicationGroupName;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.OperatorName;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.SerializedOperConfigs;
import com.microsoft.reef.io.network.nggroup.impl.utils.BroadcastingEventHandler;
import com.microsoft.reef.io.network.nggroup.impl.utils.CountingSemaphore;
import com.microsoft.reef.io.network.nggroup.impl.utils.SetMap;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.ConfigurationSerializer;
import com.microsoft.wake.EStage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@DriverSide
@Private
public class CommunicationGroupDriverImpl implements CommunicationGroupDriver {

  private static final Logger LOG = Logger.getLogger(CommunicationGroupDriverImpl.class.getName());

  private final Class<? extends Name<String>> groupName;
  private final ConcurrentMap<Class<? extends Name<String>>, OperatorSpec> operatorSpecs = new ConcurrentHashMap<>();
  private final ConcurrentMap<Class<? extends Name<String>>, Topology> topologies = new ConcurrentHashMap<>();
  private final Map<String, TaskState> perTaskState = new HashMap<>();
  private boolean finalised = false;
  private final ConfigurationSerializer confSerializer;
  private final EStage<GroupCommunicationMessage> senderStage;
  private final String driverId;
  private final int numberOfTasks;

  private final CountingSemaphore allTasksAdded;

  private final Object topologiesLock = new Object();
  private final Object configLock = new Object();
  private final AtomicBoolean initializing = new AtomicBoolean(true);

  private final Object yetToRunLock = new Object();
  private final Object toBeRemovedLock = new Object();

  private final SetMap<MsgKey, IndexedMsg> msgQue = new SetMap<>();

  private final int fanOut;

  public CommunicationGroupDriverImpl (final Class<? extends Name<String>> groupName,
                                       final ConfigurationSerializer confSerializer,
                                       final EStage<GroupCommunicationMessage> senderStage,
                                       final BroadcastingEventHandler<RunningTask> commGroupRunningTaskHandler,
                                       final BroadcastingEventHandler<FailedTask> commGroupFailedTaskHandler,
                                       final BroadcastingEventHandler<FailedEvaluator> commGroupFailedEvaluatorHandler,
                                       final BroadcastingEventHandler<GroupCommunicationMessage> commGroupMessageHandler,
                                       final String driverId, final int numberOfTasks, final int fanOut) {
    super();
    this.groupName = groupName;
    this.numberOfTasks = numberOfTasks;
    this.driverId = driverId;
    this.confSerializer = confSerializer;
    this.senderStage = senderStage;
    this.fanOut = fanOut;
    this.allTasksAdded = new CountingSemaphore(numberOfTasks, getQualifiedName(), topologiesLock);

    final TopologyRunningTaskHandler topologyRunningTaskHandler = new TopologyRunningTaskHandler(this);
    commGroupRunningTaskHandler.addHandler(topologyRunningTaskHandler);
    final TopologyFailedTaskHandler topologyFailedTaskHandler = new TopologyFailedTaskHandler(this);
    commGroupFailedTaskHandler.addHandler(topologyFailedTaskHandler);
    final TopologyFailedEvaluatorHandler topologyFailedEvaluatorHandler = new TopologyFailedEvaluatorHandler(this);
    commGroupFailedEvaluatorHandler.addHandler(topologyFailedEvaluatorHandler);
    final TopologyMessageHandler topologyMessageHandler = new TopologyMessageHandler(this);
    commGroupMessageHandler.addHandler(topologyMessageHandler);
  }

  @Override
  public CommunicationGroupDriver addBroadcast (final Class<? extends Name<String>> operatorName,
                                                final BroadcastOperatorSpec spec) {
    LOG.entering("CommunicationGroupDriverImpl", "addBroadcast", new Object[] { getQualifiedName(), Utils.simpleName(operatorName), spec });
    if (finalised) {
      throw new IllegalStateException("Can't add more operators to a finalised spec");
    }
    operatorSpecs.put(operatorName, spec);
    final Topology topology = new TreeTopology(senderStage, groupName, operatorName, driverId, numberOfTasks, fanOut);
    topology.setRootTask(spec.getSenderId());
    topology.setOperatorSpecification(spec);
    topologies.put(operatorName, topology);
    LOG.exiting("CommunicationGroupDriverImpl", "addBroadcast", Arrays.toString(new Object[] { getQualifiedName(), Utils.simpleName(operatorName), " added" }));
    return this;
  }

  @Override
  public CommunicationGroupDriver addReduce (final Class<? extends Name<String>> operatorName,
                                             final ReduceOperatorSpec spec) {
    LOG.entering("CommunicationGroupDriverImpl", "addReduce", new Object[] {getQualifiedName(), Utils.simpleName(operatorName), spec });
    if (finalised) {
      throw new IllegalStateException("Can't add more operators to a finalised spec");
    }
    LOG.finer(getQualifiedName() + "Adding reduce operator to tree topology: " + spec);
    operatorSpecs.put(operatorName, spec);
    final Topology topology = new TreeTopology(senderStage, groupName, operatorName, driverId, numberOfTasks, fanOut);
    topology.setRootTask(spec.getReceiverId());
    topology.setOperatorSpecification(spec);
    topologies.put(operatorName, topology);
    LOG.exiting("CommunicationGroupDriverImpl", "addReduce", Arrays.toString(new Object[] { getQualifiedName(), Utils.simpleName(operatorName), " added" }));
    return this;
  }

  @Override
  public Configuration getTaskConfiguration (final Configuration taskConf) {
    LOG.entering("CommunicationGroupDriverImpl", "getTaskConfiguration", new Object[] { getQualifiedName(), confSerializer.toString(taskConf) });
    final JavaConfigurationBuilder jcb = Tang.Factory.getTang().newConfigurationBuilder();
    final String taskId = taskId(taskConf);
    if (perTaskState.containsKey(taskId)) {
      jcb.bindNamedParameter(DriverIdentifier.class, driverId);
      jcb.bindNamedParameter(CommunicationGroupName.class, groupName.getName());
      LOG.finest(getQualifiedName() + "Task has been added. Waiting to acquire configLock");
      synchronized (configLock) {
        LOG.finest(getQualifiedName() + "Acquired configLock");
        while (cantGetConfig(taskId)) {
          LOG.finest(getQualifiedName() + "Need to wait for failure");
          try {
            configLock.wait();
          } catch (final InterruptedException e) {
            throw new RuntimeException(getQualifiedName() + "InterruptedException while waiting on configLock", e);
          }
        }
        LOG.finest(getQualifiedName() + taskId + " - Will fetch configuration now.");
        LOG.finest(getQualifiedName() + "Released configLock. Waiting to acquire topologiesLock");
      }
      synchronized (topologiesLock) {
        LOG.finest(getQualifiedName() + "Acquired topologiesLock");
        for (final Map.Entry<Class<? extends Name<String>>, OperatorSpec> operSpecEntry : operatorSpecs.entrySet()) {
          final Class<? extends Name<String>> operName = operSpecEntry.getKey();
          final Topology topology = topologies.get(operName);
          final JavaConfigurationBuilder jcbInner = Tang.Factory.getTang()
                                                                .newConfigurationBuilder(topology.getTaskConfiguration(taskId));
          jcbInner.bindNamedParameter(DriverIdentifier.class, driverId);
          jcbInner.bindNamedParameter(OperatorName.class, operName.getName());
          jcb.bindSetEntry(SerializedOperConfigs.class, confSerializer.toString(jcbInner.build()));
        }
        LOG.finest(getQualifiedName() + "Released topologiesLock");
      }
    } else {
      //throw new RuntimeException("Task " + taskId + " has not been added. Task should be added before calling getConfiguration");
      return null;
    }
    final Configuration configuration = jcb.build();
    LOG.exiting("CommunicationGroupDriverImpl", "getTaskConfiguration", Arrays.toString(new Object[] { getQualifiedName(), confSerializer.toString(configuration) }));
    return configuration;
  }

  private boolean cantGetConfig (final String taskId) {
    LOG.entering("CommunicationGroupDriverImpl", "cantGetConfig", new Object[] { getQualifiedName(), taskId });
    final TaskState taskState = perTaskState.get(taskId);
    if (!taskState.equals(TaskState.NOT_STARTED)) {
      LOG.finest(getQualifiedName() + taskId + " has started.");
      if (taskState.equals(TaskState.RUNNING)) {
        LOG.exiting("CommunicationGroupDriverImpl", "cantGetConfig", Arrays.toString(new Object[] { true, getQualifiedName(), taskId, " is running. We can't get config" }));
        return true;
      } else {
        LOG.exiting("CommunicationGroupDriverImpl", "cantGetConfig", Arrays.toString(new Object[] { false, getQualifiedName(), taskId, " has failed. We can get config" }));
        return false;
      }
    } else {
      LOG.exiting("CommunicationGroupDriverImpl", "cantGetConfig", Arrays.toString(new Object[] { false, getQualifiedName(), taskId, " has not started. We can get config" }));
      return false;
    }
  }

  @Override
  public void finalise () {
    finalised = true;
  }

  @Override
  public void addTask (final Configuration partialTaskConf) {
    LOG.entering("CommunicationGroupDriverImpl", "addTask", new Object[] { getQualifiedName(), confSerializer.toString(partialTaskConf) });
    final String taskId = taskId(partialTaskConf);
    LOG.finest(getQualifiedName() + "AddTask(" + taskId + "). Waiting to acquire toBeRemovedLock");
    synchronized (toBeRemovedLock) {
      LOG.finest(getQualifiedName() + "Acquired toBeRemovedLock");
      while (perTaskState.containsKey(taskId)) {
        LOG.finest(getQualifiedName() + "Trying to add an existing task. Will wait for removeTask");
        try {
          toBeRemovedLock.wait();
        } catch (final InterruptedException e) {
          throw new RuntimeException(getQualifiedName() + "InterruptedException while waiting on toBeRemovedLock", e);
        }
      }
      LOG.finest(getQualifiedName() + "Released toBeRemovedLock. Waiting to acquire topologiesLock");
    }
    synchronized (topologiesLock) {
      LOG.finest(getQualifiedName() + "Acquired topologiesLock");
      for (final Class<? extends Name<String>> operName : operatorSpecs.keySet()) {
        final Topology topology = topologies.get(operName);
        topology.addTask(taskId);
      }
      perTaskState.put(taskId, TaskState.NOT_STARTED);
      LOG.finest(getQualifiedName() + "Released topologiesLock");
    }
    LOG.fine(getQualifiedName() + "Added " + taskId + " to topology");
    LOG.exiting("CommunicationGroupDriverImpl", "addTask", Arrays.toString(new Object[] { getQualifiedName(), "Added task: ", taskId }));
  }

  public void removeTask (final String taskId) {
    LOG.entering("CommunicationGroupDriverImpl", "removeTask", new Object[] { getQualifiedName(), taskId });
    LOG.info(getQualifiedName() + "Removing Task " + taskId +
    		" as the evaluator has failed.");
    LOG.finest(getQualifiedName() + "Remove Task(" + taskId +
    		"): Waiting to acquire topologiesLock");
    synchronized (topologiesLock) {
      LOG.finest(getQualifiedName() + "Acquired topologiesLock");
      for (final Class<? extends Name<String>> operName : operatorSpecs.keySet()) {
        final Topology topology = topologies.get(operName);
        topology.removeTask(taskId);
      }
      perTaskState.remove(taskId);
      LOG.finest(getQualifiedName() + "Released topologiesLock. Waiting to acquire toBeRemovedLock");
    }
    synchronized (toBeRemovedLock) {
      LOG.finest(getQualifiedName() + "Acquired toBeRemovedLock");
      LOG.finest(getQualifiedName() + "Removed Task " + taskId + " Notifying waiting threads");
      toBeRemovedLock.notifyAll();
      LOG.finest(getQualifiedName() + "Released toBeRemovedLock");
    }
    LOG.fine(getQualifiedName() + "Removed " + taskId + " to topology");
    LOG.exiting("CommunicationGroupDriverImpl", "removeTask", Arrays.toString(new Object[] { getQualifiedName(), "Removed task: ", taskId }));
  }

  public void runTask (final String id) {
    LOG.entering("CommunicationGroupDriverImpl", "runTask", new Object[] { getQualifiedName(), id });
    LOG.finest(getQualifiedName() + "Task-" + id + " running. Waiting to acquire topologiesLock");
    LOG.fine(getQualifiedName() + "Got running Task: " + id);

    boolean nonMember = false;
    synchronized (topologiesLock) {
      if (perTaskState.containsKey(id)) {
        LOG.finest(getQualifiedName() + "Acquired topologiesLock");
        for (final Class<? extends Name<String>> operName : operatorSpecs.keySet()) {
          final Topology topology = topologies.get(operName);
          topology.onRunningTask(id);
        }
        allTasksAdded.decrement();
        perTaskState.put(id, TaskState.RUNNING);
        LOG.finest(getQualifiedName() + "Released topologiesLock. Waiting to acquire yetToRunLock");
      }
      else {
        nonMember = true;
      }
    }
    synchronized (yetToRunLock) {
      LOG.finest(getQualifiedName() + "Acquired yetToRunLock");
      yetToRunLock.notifyAll();
      LOG.finest(getQualifiedName() + "Released yetToRunLock");
    }
    if(nonMember) {
      LOG.exiting("CommunicationGroupDriverImpl", "runTask", getQualifiedName() + id + " does not belong to this communication group. Ignoring");
    }
    else {
      LOG.fine(getQualifiedName() + "Status of task " + id + " changed to RUNNING");
      LOG.exiting("CommunicationGroupDriverImpl", "runTask", Arrays.toString(new Object[] { getQualifiedName(), "Set running complete on task ", id }));
    }
  }

  public void failTask (final String id) {
    LOG.entering("CommunicationGroupDriverImpl", "failTask", new Object[] { getQualifiedName(), id });
    LOG.finest(getQualifiedName() + "Task-" + id + " failed. Waiting to acquire yetToRunLock");
    LOG.fine(getQualifiedName() + "Got failed Task: " + id);
    synchronized (yetToRunLock) {
      LOG.finest(getQualifiedName() + "Acquired yetToRunLock");
      while (cantFailTask(id)) {
        LOG.finest(getQualifiedName() + "Need to wait for it run");
        try {
          yetToRunLock.wait();
        } catch (final InterruptedException e) {
          throw new RuntimeException(getQualifiedName() + "InterruptedException while waiting on yetToRunLock", e);
        }
      }
      LOG.finest(getQualifiedName() + id + " - Can safely set failure.");
      LOG.finest(getQualifiedName() + "Released yetToRunLock. Waiting to acquire topologiesLock");
    }
    synchronized (topologiesLock) {
      LOG.finest(getQualifiedName() + "Acquired topologiesLock");
      for (final Class<? extends Name<String>> operName : operatorSpecs.keySet()) {
        final Topology topology = topologies.get(operName);
        topology.onFailedTask(id);
      }
      allTasksAdded.increment();
      perTaskState.put(id, TaskState.FAILED);
      LOG.finest(getQualifiedName() + "Removing msgs associated with dead task " + id + " from msgQue.");
      final Set<MsgKey> keys = msgQue.keySet();
      final List<MsgKey> keysToBeRemoved = new ArrayList<>();
      for (final MsgKey msgKey : keys) {
        if (msgKey.getSrc().equals(id)) {
          keysToBeRemoved.add(msgKey);
        }
      }
      LOG.finest(getQualifiedName() + keysToBeRemoved + " keys that will be removed");
      for (final MsgKey key : keysToBeRemoved) {
        msgQue.remove(key);
      }
      LOG.finest(getQualifiedName() + "Released topologiesLock. Waiting to acquire configLock");
    }
    synchronized (configLock) {
      LOG.finest(getQualifiedName() + "Acquired configLock");
      configLock.notifyAll();
      LOG.finest(getQualifiedName() + "Released configLock");
    }
    LOG.fine(getQualifiedName() + "Status of task " + id + " changed to FAILED");
    LOG.exiting("CommunicationGroupDriverImpl", "failTask", Arrays.toString(new Object[] { getQualifiedName(), "Set failed complete on task ", id }));
  }

  private boolean cantFailTask (final String taskId) {
    LOG.entering("CommunicationGroupDriverImpl", "cantFailTask", new Object[] { getQualifiedName(), taskId });
    final TaskState taskState = perTaskState.get(taskId);
    if (!taskState.equals(TaskState.NOT_STARTED)) {
      LOG.finest(getQualifiedName() + taskId + " has started.");
      if (!taskState.equals(TaskState.RUNNING)) {
        LOG.exiting("CommunicationGroupDriverImpl", "cantFailTask", Arrays.toString(new Object[] { true, getQualifiedName(), taskId, " is not running yet. Can't set failure" }));
        return true;
      } else {
        LOG.exiting("CommunicationGroupDriverImpl", "cantFailTask", Arrays.toString(new Object[] { false, getQualifiedName(), taskId, " is running. Can set failure" }));
        return false;
      }
    } else {
      LOG.exiting("CommunicationGroupDriverImpl", "cantFailTask", Arrays.toString(new Object[] { true, getQualifiedName(), taskId, " has not started. We can't fail a task that hasn't started" }));
      return true;
    }
  }

  public void queNProcessMsg (final GroupCommunicationMessage msg) {
    LOG.entering("CommunicationGroupDriverImpl", "queNProcessMsg", new Object[] { getQualifiedName(), msg });
    final IndexedMsg indMsg = new IndexedMsg(msg);
    final Class<? extends Name<String>> operName = indMsg.getOperName();
    final MsgKey key = new MsgKey(msg);
    if (msgQue.contains(key, indMsg)) {
      throw new RuntimeException(getQualifiedName() + "MsgQue already contains " + msg.getType() + " msg for " + key + " in "
                          + Utils.simpleName(operName));
    }
    LOG.finest(getQualifiedName() + "Adding msg to que");
    msgQue.add(key, indMsg);
    if (msgQue.count(key) == topologies.size()) {
      LOG.finest(getQualifiedName() + "MsgQue for " + key + " contains " + msg.getType() + " msgs from: "
               + msgQue.get(key));
      for (final IndexedMsg innerIndMsg : msgQue.remove(key)) {
        topologies.get(innerIndMsg.getOperName()).onReceiptOfMessage(innerIndMsg.getMsg());
      }
      LOG.finest(getQualifiedName() + "All msgs processed and removed");
    }
    LOG.exiting("CommunicationGroupDriverImpl", "queNProcessMsg", Arrays.toString(new Object[] { getQualifiedName(), "Que & Process done for: ", msg }));
  }

  private boolean isMsgVersionOk (final GroupCommunicationMessage msg) {
    LOG.entering("CommunicationGroupDriverImpl", "isMsgVersionOk", new Object[] { getQualifiedName(), msg });
    if (msg.hasVersion()) {
      final String srcId = msg.getSrcid();
      final int rcvSrcVersion = msg.getSrcVersion();
      final int expSrcVersion = topologies.get(Utils.getClass(msg.getOperatorname())).getNodeVersion(srcId);

      final boolean srcVersionChk = chkVersion(rcvSrcVersion, expSrcVersion, "Src Version Check: ");
      LOG.exiting("CommunicationGroupDriverImpl", "isMsgVersionOk", Arrays.toString(new Object[] { srcVersionChk, getQualifiedName(), msg }));
      return srcVersionChk;
    } else {
      throw new RuntimeException(getQualifiedName() + "can only deal with versioned msgs");
    }
  }

  private boolean chkVersion (final int rcvVersion, final int version, final String msg) {
    if (rcvVersion < version) {
      LOG.warning(getQualifiedName() + msg + "received a ver-" + rcvVersion + " msg while expecting ver-" + version);
      return false;
    }
    if (rcvVersion > version) {
      LOG.warning(getQualifiedName() + msg + "received a HIGHER ver-" + rcvVersion + " msg while expecting ver-"
                  + version + ". Something fishy!!!");
      return false;
    }
    return true;
  }

  public void processMsg (final GroupCommunicationMessage msg) {
    LOG.entering("CommunicationGroupDriverImpl", "processMsg", new Object[] { getQualifiedName(), msg });
    LOG.finest(getQualifiedName() + "ProcessMsg: " + msg + ". Waiting to acquire topologiesLock");
    synchronized (topologiesLock) {
      LOG.finest(getQualifiedName() + "Acquired topologiesLock");
      if (!isMsgVersionOk(msg)) {
        LOG.finer(getQualifiedName() + "Discarding msg. Released topologiesLock");
        return;
      }
      if (initializing.get() || msg.getType().equals(Type.UpdateTopology)) {
        LOG.fine(getQualifiedName() + msg.getSimpleOperName() + ": Waiting for all required(" + allTasksAdded.getInitialCount() +
                 ") nodes to run");
        allTasksAdded.await();
        LOG.fine(getQualifiedName() + msg.getSimpleOperName() + ": All required(" + allTasksAdded.getInitialCount() +
                 ") nodes are running");
        initializing.compareAndSet(true, false);
      }
      queNProcessMsg(msg);
      LOG.finest(getQualifiedName() + "Released topologiesLock");
    }
    LOG.exiting("CommunicationGroupDriverImpl", "processMsg", Arrays.toString(new Object[] { getQualifiedName(), "ProcessMsg done for: ", msg }));
  }

  private String taskId (final Configuration partialTaskConf) {
    try {
      final Injector injector = Tang.Factory.getTang().newInjector(partialTaskConf);
      return injector.getNamedInstance(TaskConfigurationOptions.Identifier.class);
    } catch (final InjectionException e) {
      throw new RuntimeException(getQualifiedName() + "Injection exception while extracting taskId from partialTaskConf", e);
    }
  }

  private String getQualifiedName () {
    return Utils.simpleName(groupName) + " - ";
  }
}
