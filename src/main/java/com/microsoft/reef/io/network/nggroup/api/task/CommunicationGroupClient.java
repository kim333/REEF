/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.task;

import com.microsoft.reef.annotations.audience.TaskSide;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.tang.annotations.Name;

/**
 * The Task side interface of a communication group
 * Lets one get the operators configured for this task
 * and use them for communication between tasks configured
 * in this communication group
 */
@TaskSide
@DefaultImplementation(value = com.microsoft.reef.io.network.nggroup.impl.task.CommunicationGroupClientImpl.class)
public interface CommunicationGroupClient {

  /**
   * @return The name configured on this communication group
   */
  Class<? extends Name<String>> getName();

  /**
   * The broadcast sender configured on this communication group
   * with the given oepratorName
   *
   * @param operatorName
   * @return
   */
  Broadcast.Sender getBroadcastSender(Class<? extends Name<String>> operatorName);

  /**
   * The broadcast receiver configured on this communication group
   * with the given oepratorName
   *
   * @param operatorName
   * @return
   */
  Broadcast.Receiver getBroadcastReceiver(Class<? extends Name<String>> operatorName);

  /**
   * The reduce receiver configured on this communication group
   * with the given oepratorName
   *
   * @param operatorName
   * @return
   */
  Reduce.Receiver getReduceReceiver(Class<? extends Name<String>> operatorName);

  /**
   * The reduce sender configured on this communication group
   * with the given oepratorName
   *
   * @param operatorName
   * @return
   */
  Reduce.Sender getReduceSender(Class<? extends Name<String>> operatorName);

  /**
   *
   * @return Changes in topology of this communication group since the last time
   *         this method was called
   */
  GroupChanges getTopologyChanges();

  /**
   * Asks the driver to update the topology of this communication group. This can
   * be an expensive call depending on what the minimum number of tasks is for this
   * group to function as this first tells the driver, driver then tells the affected
   * tasks and the driver gives a green only after affected tasks have had a chance
   * to be sure that their topology will be updated before the next message is
   * communicated
   */
  void updateTopology();


}
