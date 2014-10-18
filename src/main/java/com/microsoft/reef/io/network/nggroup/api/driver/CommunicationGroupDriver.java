/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.driver;

import com.microsoft.reef.annotations.audience.DriverSide;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.io.network.nggroup.impl.config.BroadcastOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.ReduceOperatorSpec;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.annotations.Name;

/**
 * The driver side interface of a Communication Group
 * Lets one add opertaors and tasks.
 * Main function is to extract the configuration related
 * to the Group Communication for a task in the comm group
 */
@DriverSide
public interface CommunicationGroupDriver {

  /**
   * Add the broadcast operator specified by the
   * 'spec' with name 'operatorName' into this
   * Communication Group
   *
   * @param operatorName
   * @param spec
   * @return
   */
  public CommunicationGroupDriver addBroadcast(Class<? extends Name<String>> operatorName, BroadcastOperatorSpec spec);

  /**
   * Add the reduce operator specified by the
   * 'spec' with name 'operatorName' into this
   * Communication Group
   *
   * @param operatorName
   * @param spec
   * @return
   */
  public CommunicationGroupDriver addReduce(Class<? extends Name<String>> operatorName, ReduceOperatorSpec spec);

  /**
   * This signals to the service that no more
   * operator specs will be added to this communication
   * group and an attempt to do that will throw an
   * IllegalStateException
   */
  public void finalise();

  /**
   * Returns a configuration that includes the partial task
   * configuration passed in as 'taskConf' and makes the
   * current communication group and the operators configured
   * on it available on the Task side. Provides for injection
   * of {@link GroupCommClient}
   * @param taskConf
   * @return
   */
  public Configuration getTaskConfiguration(Configuration taskConf);

  /**
   * Add the task represented by this configuration to this
   * communication group. The configuration needs to contain
   * the id of the Task that will be used
   * @param partialTaskConf
   */
  public void addTask(Configuration partialTaskConf);
}