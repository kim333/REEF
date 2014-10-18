/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.driver;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;

/**
 * A node in the Topology representing a Task on the driver
 * Impl should maintain state relating to whether task is running/dead and
 * status of neighboring nodes and send ctrl msgs to the tasks indicating
 * topology changing events
 */
public interface TaskNode {

  String getTaskId();

  int getVersion();

  int getNumberOfChildren();

  TaskNode getParent();

  void setParent(TaskNode parent);

  void addChild(TaskNode child);

  void removeChild(TaskNode taskNode);

  boolean isRunning();

  void onRunningTask();

  void onFailedTask();

  boolean hasChanges();

  boolean isNeighborActive(String neighborId);

  void onReceiptOfAcknowledgement(GroupCommunicationMessage msg);

  void onParentRunning();

  void onParentDead();

  void onChildRunning(String childId);

  void onChildDead(String childId);

  /**
   * Check if this node is ready for sending
   * TopologySetup
   */
  void checkAndSendTopologySetupMessage();

  /**
   * Check if the neighbor node with id source
   * is ready for sending TopologySetup
   * @param source
   */
  void checkAndSendTopologySetupMessageFor(String source);

  /**
   * reset topology setup ensures that update topology is not sent to someone
   * who is already updating topology which is usually when they are just
   * (re)starting
   *
   * @return
   */
  boolean resetTopologySetupSent();

  void waitForTopologySetupOrFailure();

  void setSibling(TaskNode leaf);

  TaskNode successor();

  void updatingTopology ();
}
