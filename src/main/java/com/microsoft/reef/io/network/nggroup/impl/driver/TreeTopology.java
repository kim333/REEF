/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import com.microsoft.reef.io.network.group.operators.GroupCommOperator;
import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.reef.io.network.nggroup.api.config.OperatorSpec;
import com.microsoft.reef.io.network.nggroup.api.driver.TaskNode;
import com.microsoft.reef.io.network.nggroup.api.driver.Topology;
import com.microsoft.reef.io.network.nggroup.impl.GroupChangesCodec;
import com.microsoft.reef.io.network.nggroup.impl.GroupChangesImpl;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.config.BroadcastOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.ReduceOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.DataCodec;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.ReduceFunctionParam;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.TaskVersion;
import com.microsoft.reef.io.network.nggroup.impl.operators.BroadcastReceiver;
import com.microsoft.reef.io.network.nggroup.impl.operators.BroadcastSender;
import com.microsoft.reef.io.network.nggroup.impl.operators.ReduceReceiver;
import com.microsoft.reef.io.network.nggroup.impl.operators.ReduceSender;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.formats.AvroConfigurationSerializer;
import com.microsoft.tang.formats.ConfigurationSerializer;
import com.microsoft.wake.EStage;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.impl.SingleThreadStage;

/**
 * Implements a tree topology with the specified Fan Out
 */
public class TreeTopology implements Topology {

  private static final Logger LOG = Logger.getLogger(TreeTopology.class.getName());

  private final EStage<GroupCommunicationMessage> senderStage;
  private final Class<? extends Name<String>> groupName;
  private final Class<? extends Name<String>> operName;
  private final String driverId;
  private String rootId;
  private OperatorSpec operatorSpec;

  private TaskNode root;
  private TaskNode logicalRoot;
  private TaskNode prev;
  private final int fanOut;

  private final ConcurrentMap<String, TaskNode> nodes = new ConcurrentSkipListMap<>();
  private final ConfigurationSerializer confSer = new AvroConfigurationSerializer();



  public TreeTopology (final EStage<GroupCommunicationMessage> senderStage,
                       final Class<? extends Name<String>> groupName, final Class<? extends Name<String>> operatorName,
                       final String driverId, final int numberOfTasks, final int fanOut) {
    this.senderStage = senderStage;
    this.groupName = groupName;
    this.operName = operatorName;
    this.driverId = driverId;
    this.fanOut = fanOut;
    LOG.config(getQualifiedName() + "Tree Topology running with a fan-out of " + fanOut);
  }

  @Override
  public void setRootTask (final String rootId) {
    LOG.entering("TreeTopology", "setRootTask", new Object[] { getQualifiedName(), rootId });
    this.rootId = rootId;
    LOG.exiting("TreeTopology", "setRootTask", getQualifiedName() + rootId);
  }

  @Override
  public String getRootId () {
    LOG.entering("TreeTopology", "getRootId", getQualifiedName());
    LOG.exiting("TreeTopology", "getRootId", getQualifiedName() + rootId);
    return rootId;
  }

  @Override
  public void setOperatorSpecification (final OperatorSpec spec) {
    LOG.entering("TreeTopology", "setOperSpec", new Object[] { getQualifiedName(), spec });
    this.operatorSpec = spec;
    LOG.exiting("TreeTopology", "setOperSpec", getQualifiedName() + spec);
  }

  @Override
  public Configuration getTaskConfiguration (final String taskId) {
    LOG.entering("TreeTopology", "getTaskConfig", new Object[] { getQualifiedName(), taskId });
    final TaskNode taskNode = nodes.get(taskId);
    if (taskNode == null) {
      throw new RuntimeException(getQualifiedName() + taskId + " does not exist");
    }

    final int version = getNodeVersion(taskId);
    final JavaConfigurationBuilder jcb = Tang.Factory.getTang().newConfigurationBuilder();
    jcb.bindNamedParameter(DataCodec.class, operatorSpec.getDataCodecClass());
    jcb.bindNamedParameter(TaskVersion.class, Integer.toString(version));
    if (operatorSpec instanceof BroadcastOperatorSpec) {
      final BroadcastOperatorSpec broadcastOperatorSpec = (BroadcastOperatorSpec) operatorSpec;
      if (taskId.equals(broadcastOperatorSpec.getSenderId())) {
        jcb.bindImplementation(GroupCommOperator.class, BroadcastSender.class);
      } else {
        jcb.bindImplementation(GroupCommOperator.class, BroadcastReceiver.class);
      }
    }
    else if (operatorSpec instanceof ReduceOperatorSpec) {
      final ReduceOperatorSpec reduceOperatorSpec = (ReduceOperatorSpec) operatorSpec;
      jcb.bindNamedParameter(ReduceFunctionParam.class, reduceOperatorSpec.getRedFuncClass());
      if (taskId.equals(reduceOperatorSpec.getReceiverId())) {
        jcb.bindImplementation(GroupCommOperator.class, ReduceReceiver.class);
      } else {
        jcb.bindImplementation(GroupCommOperator.class, ReduceSender.class);
      }
    }
    final Configuration retConf = jcb.build();
    LOG.exiting("TreeTopology", "getTaskConfig", getQualifiedName() + confSer.toString(retConf));
    return retConf;
  }

  @Override
  public int getNodeVersion (final String taskId) {
    LOG.entering("TreeTopology", "getNodeVersion", new Object[] { getQualifiedName(), taskId });
    final TaskNode node = nodes.get(taskId);
    if (node == null) {
      throw new RuntimeException(getQualifiedName() + taskId + " is not available on the nodes map");
    }
    final int version = node.getVersion();
    LOG.exiting("TreeTopology", "getNodeVersion", getQualifiedName() + " " + taskId + " " + version);
    return version;
  }

  @Override
  public void removeTask (final String taskId) {
    LOG.entering("TreeTopology", "removeTask", new Object[] { getQualifiedName(), taskId });
    if (!nodes.containsKey(taskId)) {
      LOG.fine("Trying to remove a non-existent node in the task graph");
      LOG.exiting("TreeTopology", "removeTask", getQualifiedName());
      return;
    }
    if (taskId.equals(rootId)) {
      unsetRootNode(taskId);
    } else {
      removeChild(taskId);
    }
    LOG.exiting("TreeTopology", "removeTask", getQualifiedName() + taskId);
  }

  @Override
  public void addTask (final String taskId) {
    LOG.entering("TreeTopology", "addTask", new Object[] { getQualifiedName(), taskId });
    if (nodes.containsKey(taskId)) {
      LOG.fine("Got a request to add a task that is already in the graph. " +
      		"We need to block this request till the delete finishes. ***CAUTION***");
    }

    if (taskId.equals(rootId)) {
      setRootNode(taskId);
    } else {
      addChild(taskId);
    }
    prev = nodes.get(taskId);
    LOG.exiting("TreeTopology", "addTask", getQualifiedName() + taskId);
  }

  private void addChild (final String taskId) {
    LOG.entering("TreeTopology", "addChild", new Object[] { getQualifiedName(), taskId });
    LOG.finest(getQualifiedName() + "Adding leaf " + taskId);
    final TaskNode node = new TaskNodeImpl(senderStage, groupName, operName, taskId, driverId, false);
    if (logicalRoot != null) {
      addTaskNode(node);
    }
    nodes.put(taskId, node);
    LOG.exiting("TreeTopology", "addChild", getQualifiedName() + taskId);
  }

  private void addTaskNode (final TaskNode node) {
    LOG.entering("TreeTopology", "addTaskNode", new Object[] { getQualifiedName(), node });
    if (logicalRoot.getNumberOfChildren() >= this.fanOut) {
      logicalRoot = logicalRoot.successor();
    }
    node.setParent(logicalRoot);
    logicalRoot.addChild(node);
    prev.setSibling(node);
    LOG.exiting("TreeTopology", "addTaskNode", getQualifiedName() + node);
  }

  private void removeChild (final String taskId) {
    LOG.entering("TreeTopology", "removeChild", new Object[] { getQualifiedName(), taskId });
    if (root != null) {
      root.removeChild(nodes.get(taskId));
    }
    nodes.remove(taskId);
    LOG.exiting("TreeTopology", "removeChild", getQualifiedName() + taskId);
  }

  private void setRootNode (final String rootId) {
    LOG.entering("TreeTopology", "setRootNode", new Object[] { getQualifiedName(), rootId });
    final TaskNode node = new TaskNodeImpl(senderStage, groupName, operName, rootId, driverId, true);
    this.root = node;
    this.logicalRoot = this.root;
    this.prev = this.root;

    for (final Map.Entry<String, TaskNode> nodeEntry : nodes.entrySet()) {
      final TaskNode leaf = nodeEntry.getValue();
      addTaskNode(leaf);
      this.prev = leaf;
    }
    nodes.put(rootId, root);
    LOG.exiting("TreeTopology", "setRootNode", getQualifiedName() + rootId);
  }

  private void unsetRootNode (final String taskId) {
    LOG.entering("TreeTopology", "unsetRootNode", new Object[] { getQualifiedName(), taskId });
    nodes.remove(rootId);

    for (final Map.Entry<String, TaskNode> nodeEntry : nodes.entrySet()) {
      final String id = nodeEntry.getKey();
      final TaskNode leaf = nodeEntry.getValue();
      leaf.setParent(null);
    }
    LOG.exiting("TreeTopology", "unsetRootNode", getQualifiedName() + taskId);
  }

  @Override
  public void onFailedTask (final String taskId) {
    LOG.entering("TreeTopology", "onFailedTask", new Object[] { getQualifiedName(), taskId });
    final TaskNode taskNode = nodes.get(taskId);
    if (taskNode == null) {
      throw new RuntimeException(getQualifiedName() + taskId + " does not exist");
    }
    taskNode.onFailedTask();
    LOG.exiting("TreeTopology", "onFailedTask", getQualifiedName() + taskId);
  }

  @Override
  public void onRunningTask (final String taskId) {
    LOG.entering("TreeTopology", "onRunningTask", new Object[] { getQualifiedName(), taskId });
    final TaskNode taskNode = nodes.get(taskId);
    if (taskNode == null) {
      throw new RuntimeException(getQualifiedName() + taskId + " does not exist");
    }
    taskNode.onRunningTask();
    LOG.exiting("TreeTopology", "onRunningTask", getQualifiedName() + taskId);
  }

  @Override
  public void onReceiptOfMessage (final GroupCommunicationMessage msg) {
    LOG.entering("TreeTopology", "onReceiptOfMessage", new Object[] { getQualifiedName(), msg });
    switch (msg.getType()) {
    case TopologyChanges:
      onTopologyChanges(msg);
      break;
    case UpdateTopology:
      onUpdateTopology(msg);
      break;

    default:
      nodes.get(msg.getSrcid()).onReceiptOfAcknowledgement(msg);
      break;
    }
    LOG.exiting("TreeTopology", "onReceiptOfMessage", getQualifiedName() + msg);
  }

  private void onUpdateTopology (final GroupCommunicationMessage msg) {
    LOG.entering("TreeTopology", "onUpdateTopology", new Object[] { getQualifiedName(), msg });
    LOG.fine(getQualifiedName() + "Update affected parts of Topology");
    final String dstId = msg.getSrcid();
    final int version = getNodeVersion(dstId);

    LOG.finest(getQualifiedName() + "Creating NodeTopologyUpdateWaitStage to wait on nodes to be updated");
    final EventHandler<List<TaskNode>> topoUpdateWaitHandler = new TopologyUpdateWaitHandler(senderStage, groupName,
                                                                                             operName, driverId, 0,
                                                                                             dstId, version,
                                                                                             getQualifiedName());
    final EStage<List<TaskNode>> nodeTopologyUpdateWaitStage = new SingleThreadStage<>("NodeTopologyUpdateWaitStage",
                                                                                       topoUpdateWaitHandler,
                                                                                       nodes.size());

    final List<TaskNode> toBeUpdatedNodes = new ArrayList<>(nodes.size());
    LOG.finest(getQualifiedName() + "Checking which nodes need to be updated");
    for (final TaskNode node : nodes.values()) {
      if (node.isRunning() && node.hasChanges() && node.resetTopologySetupSent()) {
          toBeUpdatedNodes.add(node);
      }
    }
    for (final TaskNode node : toBeUpdatedNodes) {
      node.updatingTopology();
      LOG.fine(getQualifiedName() + "Asking " + node + " to UpdateTopology");
      senderStage.onNext(Utils.bldVersionedGCM(groupName, operName, Type.UpdateTopology, driverId, 0, node.getTaskId(),
                                               node.getVersion(), Utils.EmptyByteArr));
    }
    nodeTopologyUpdateWaitStage.onNext(toBeUpdatedNodes);
    LOG.exiting("TreeTopology", "onUpdateTopology", getQualifiedName() + msg);
  }

  private void onTopologyChanges (final GroupCommunicationMessage msg) {
    LOG.entering("TreeTopology", "onTopologyChanges", new Object[] { getQualifiedName(), msg });
    LOG.fine(getQualifiedName() + "Check TopologyChanges");
    final String dstId = msg.getSrcid();
    boolean hasTopologyChanged = false;
    LOG.finest(getQualifiedName() + "Checking which nodes need to be updated");
    for (final TaskNode node : nodes.values()) {
      if(!node.isRunning() || node.hasChanges()) {
        hasTopologyChanged = true;
        break;
      }
    }
    final GroupChanges changes = new GroupChangesImpl(hasTopologyChanged);
    final Codec<GroupChanges> changesCodec = new GroupChangesCodec();
    LOG.fine(getQualifiedName() + "TopologyChanges: " + changes);
    senderStage.onNext(Utils.bldVersionedGCM(groupName, operName, Type.TopologyChanges, driverId, 0, dstId, getNodeVersion(dstId),
                                             changesCodec.encode(changes)));
    LOG.exiting("TreeTopology", "onTopologyChanges", getQualifiedName() + msg);
  }

  private String getQualifiedName () {
    return Utils.simpleName(groupName) + ":" + Utils.simpleName(operName) + " - ";
  }
}
