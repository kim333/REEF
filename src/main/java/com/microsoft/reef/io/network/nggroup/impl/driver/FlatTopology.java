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
import com.microsoft.wake.EStage;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.impl.SingleThreadStage;

/**
 * Implements a one level Tree Topology
 */
public class FlatTopology implements Topology {

  private static final Logger LOG = Logger.getLogger(FlatTopology.class.getName());

  private final EStage<GroupCommunicationMessage> senderStage;
  private final Class<? extends Name<String>> groupName;
  private final Class<? extends Name<String>> operName;
  private final String driverId;
  private String rootId;
  private OperatorSpec operatorSpec;

  private TaskNode root;
  private final ConcurrentMap<String, TaskNode> nodes = new ConcurrentSkipListMap<>();

  public FlatTopology (final EStage<GroupCommunicationMessage> senderStage,
                       final Class<? extends Name<String>> groupName, final Class<? extends Name<String>> operatorName,
                       final String driverId, final int numberOfTasks) {
    this.senderStage = senderStage;
    this.groupName = groupName;
    this.operName = operatorName;
    this.driverId = driverId;
  }

  @Override
  public void setRootTask (final String rootId) {
    this.rootId = rootId;
  }

  /**
   * @return the rootId
   */
  @Override
  public String getRootId () {
    return rootId;
  }

  @Override
  public void setOperatorSpecification (final OperatorSpec spec) {
    this.operatorSpec = spec;
  }

  @Override
  public Configuration getTaskConfiguration (final String taskId) {
    LOG.finest(getQualifiedName() + "Getting config for task " + taskId);
    final TaskNode taskNode = nodes.get(taskId);
    if (taskNode == null) {
      throw new RuntimeException(getQualifiedName() + taskId + " does not exist");
    }

    final int version;
    version = getNodeVersion(taskId);
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
    if (operatorSpec instanceof ReduceOperatorSpec) {
      final ReduceOperatorSpec reduceOperatorSpec = (ReduceOperatorSpec) operatorSpec;
      jcb.bindNamedParameter(ReduceFunctionParam.class, reduceOperatorSpec.getRedFuncClass());
      if (taskId.equals(reduceOperatorSpec.getReceiverId())) {
        jcb.bindImplementation(GroupCommOperator.class, ReduceReceiver.class);
      } else {
        jcb.bindImplementation(GroupCommOperator.class, ReduceSender.class);
      }
    }
    return jcb.build();
  }

  @Override
  public int getNodeVersion (final String taskId) {
    final TaskNode node = nodes.get(taskId);
    if (node == null) {
      throw new RuntimeException(getQualifiedName() + taskId + " is not available on the nodes map");
    }
    final int version = node.getVersion();
    return version;
  }

  @Override
  public void removeTask (final String taskId) {
    if (!nodes.containsKey(taskId)) {
      LOG.warning("Trying to remove a non-existent node in the task graph");
      return;
    }
    if (taskId.equals(rootId)) {
      unsetRootNode(taskId);
    } else {
      removeChild(taskId);
    }
  }

  @Override
  public void addTask (final String taskId) {
    if (nodes.containsKey(taskId)) {
      LOG.warning("Got a request to add a task that is already in the graph");
      LOG.warning("We need to block this request till the delete finishes");
    }
    if (taskId.equals(rootId)) {
      setRootNode(taskId);
    } else {
      addChild(taskId);
    }
  }

  /**
   * @param taskId
   */
  private void addChild (final String taskId) {
    LOG.finest(getQualifiedName() + "Adding leaf " + taskId);
    final TaskNode node = new TaskNodeImpl(senderStage, groupName, operName, taskId, driverId, false);
    final TaskNode leaf = node;
    if (root != null) {
      leaf.setParent(root);
      root.addChild(leaf);
    }
    nodes.put(taskId, leaf);
  }

  /**
   * @param taskId
   */
  private void removeChild (final String taskId) {
    LOG.finest(getQualifiedName() + "Removing leaf " + taskId);
    if (root != null) {
      root.removeChild(nodes.get(taskId));
    }
    nodes.remove(taskId);
  }

  private void setRootNode (final String rootId) {
    LOG.finest(getQualifiedName() + "Setting " + rootId + " as root");
    final TaskNode node = new TaskNodeImpl(senderStage, groupName, operName, rootId, driverId, true);
    this.root = node;

    for (final Map.Entry<String, TaskNode> nodeEntry : nodes.entrySet()) {
      final String id = nodeEntry.getKey();

      final TaskNode leaf = nodeEntry.getValue();
      root.addChild(leaf);
      leaf.setParent(root);
    }
    nodes.put(rootId, root);
  }

  /**
   * @param taskId
   */
  private void unsetRootNode (final String taskId) {
    LOG.finest(getQualifiedName() + "Unsetting " + rootId + " as root");
    nodes.remove(rootId);

    for (final Map.Entry<String, TaskNode> nodeEntry : nodes.entrySet()) {
      final String id = nodeEntry.getKey();
      final TaskNode leaf = nodeEntry.getValue();
      leaf.setParent(null);
    }
  }

  @Override
  public void onFailedTask (final String id) {
    LOG.finest(getQualifiedName() + "Task-" + id + " failed");
    final TaskNode taskNode = nodes.get(id);
    if (taskNode == null) {
      throw new RuntimeException(getQualifiedName() + id + " does not exist");
    }

    taskNode.onFailedTask();
  }

  @Override
  public void onRunningTask (final String id) {
    LOG.finest(getQualifiedName() + "Task-" + id + " is running");
    final TaskNode taskNode = nodes.get(id);
    if (taskNode == null) {
      throw new RuntimeException(getQualifiedName() + id + " does not exist");
    }

    taskNode.onRunningTask();
  }

  @Override
  public void onReceiptOfMessage (final GroupCommunicationMessage msg) {
    LOG.finest(getQualifiedName() + "processing " + msg.getType() + " from " + msg.getSrcid());
    if (msg.getType().equals(Type.TopologyChanges)) {
      processTopologyChanges(msg);
      return;
    }
    if (msg.getType().equals(Type.UpdateTopology)) {
      processUpdateTopology(msg);
      return;
    }
    final String id = msg.getSrcid();
    nodes.get(id).onReceiptOfAcknowledgement(msg);
  }

  private void processUpdateTopology (final GroupCommunicationMessage msg) {
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
      senderStage.onNext(Utils.bldVersionedGCM(groupName, operName, Type.UpdateTopology, driverId, 0, node.getTaskId(),
                                               node.getVersion(), Utils.EmptyByteArr));
    }
    nodeTopologyUpdateWaitStage.onNext(toBeUpdatedNodes);
  }

  private void processTopologyChanges (final GroupCommunicationMessage msg) {
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
    senderStage.onNext(Utils.bldVersionedGCM(groupName, operName, Type.TopologyChanges, driverId, 0, dstId, getNodeVersion(dstId),
                                             changesCodec.encode(changes)));
  }

  private String getQualifiedName () {
    return Utils.simpleName(groupName) + ":" + Utils.simpleName(operName) + " - ";
  }
}
