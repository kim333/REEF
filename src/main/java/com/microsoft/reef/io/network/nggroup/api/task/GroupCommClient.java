/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.task;

import com.microsoft.reef.annotations.Provided;
import com.microsoft.reef.annotations.audience.TaskSide;
import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.tang.annotations.Name;


/**
 * The task side interface for the Group Communication Service
 */
@TaskSide
@Provided
@DefaultImplementation(value = com.microsoft.reef.io.network.nggroup.impl.task.GroupCommClientImpl.class)
public interface GroupCommClient {

  /**
   * @param string
   * @return The communication group client with the given name that gives access
   *         to the operators configured on it that will be used to do group communication
   */
  CommunicationGroupClient getCommunicationGroup(Class<? extends Name<String>> groupName);
}
