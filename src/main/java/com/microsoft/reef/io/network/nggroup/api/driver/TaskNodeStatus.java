/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.driver;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;

/**
 * Tracks the Status of the ctrl msgs sent to a
 * task node in the topology -
 *   what msgs have been sent to this node and
 *   what msgs have been ACKed as received by this node
 *   Status of neighbors
 * This is used to see whether the local topology
 * of a Task is completely set-up
 * It also offers convenience methods for waiting
 * on receiving ACKs from the task.
 */
public interface TaskNodeStatus {

  boolean hasChanges();

  void onTopologySetupMessageSent();

  boolean isActive(String neighborId);

  /**
   * Process the msg that was received and update
   * state accordingly
   */
  void processAcknowledgement(GroupCommunicationMessage msg);

  /**
   * To be called before sending a ctrl msg to the task
   * represented by this node. All ctrl msgs sent to this
   * node need to be ACKed.
   * Ctrl msgs will be sent        from a src &
   * ACK sent from the task will be for a src.
   * As this is called from the TaskNodeImpl use srcId of msg
   * In TaskNodeImpl while processMsg        use dstId of msg
   */
  public void expectAckFor(final Type msgType, final String srcId);

  /**
   * Used when the task has failed to clear all
   * the state that is associated with this task
   * Also should release the locks held for implementing
   * the convenience wait* methods
   */
  void clearStateAndReleaseLocks();

  /**
   * This should remove state concerning neighboring tasks
   * that have failed
   */
  void updateFailureOf(String taskId);

  void waitForTopologySetup();

  /**
   * Called to denote that a UpdateTopology msg will
   * be sent
   */
  void updatingTopology ();
}
