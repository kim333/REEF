/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.driver;

import com.microsoft.reef.io.network.nggroup.api.config.OperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.tang.Configuration;

/**
 * A topology should implement the following
 * interface so that it can work with the
 * elastic group communication framework
 * Currently we have two implementations
 * 1. Flat 2. Tree
 */
public interface Topology {

  /**
   * Get the version of the Task 'taskId'
   * that belongs to this topology
   * @param taskId
   * @return
   */
  int getNodeVersion(String taskId);

  /**
   * Get the id of the root task
   * @return
   */
  String getRootId();

  /**
   * Set task with id 'senderId' as
   * the root of this topology
   * @param senderId
   */
  void setRootTask(String senderId);

  /**
   * Add task with id 'taskId' to
   * the topology
   * @param taskId
   */
  void addTask(String taskId);

  /**
   * Remove task with id 'taskId' from
   * the topology
   * @param taskId
   */
  void removeTask(String taskId);

  /**
   * Update state on receipt of RunningTask
   * event for task with id 'id'
   * @param id
   */
  void onRunningTask(String id);

  /**
   * Update state on receipt of FailedTask
   * event for task with id 'id'
   * @param id
   */
  void onFailedTask(String id);

  /**
   * Set operator specification of the operator
   * that is the owner of this topology instance
   * @param spec
   */
  void setOperatorSpecification(OperatorSpec spec);

  /**
   * Get the topology portion of the Configuration
   * for the task 'taskId' that belongs to this
   * topology
   * @param taskId
   * @return
   */
  Configuration getTaskConfiguration(String taskId);

  /**
   * Update state on receipt of a message
   * from the tasks
   * @param msg
   */
  void onReceiptOfMessage(GroupCommunicationMessage msg);
}
