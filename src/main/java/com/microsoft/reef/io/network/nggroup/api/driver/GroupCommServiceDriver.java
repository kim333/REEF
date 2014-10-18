/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.driver;

import com.microsoft.reef.annotations.Provided;
import com.microsoft.reef.annotations.audience.Private;
import com.microsoft.reef.driver.evaluator.FailedEvaluator;
import com.microsoft.reef.driver.task.FailedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.io.network.nggroup.impl.driver.GroupCommDriverImpl;
import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.wake.EStage;

@Private
@Provided
@DefaultImplementation(value = GroupCommDriverImpl.class)
public interface GroupCommServiceDriver extends GroupCommDriver {

  /**
   * Not user facing but used the Group Communication Service class
   *
   * @return The running task stage that will handle the RunningTask
   *         events
   */
  EStage<RunningTask> getGroupCommRunningTaskStage();

  /**
   * Not user facing but used the Group Communication Service class
   *
   * @return The running task stage that will handle the FailedTask
   *         events
   */
  EStage<FailedTask> getGroupCommFailedTaskStage();

  /**
   * Not user facing but used the Group Communication Service class
   *
   * @return The running task stage that will handle the FailedEvaluator
   *         events
   */
  EStage<FailedEvaluator> getGroupCommFailedEvaluatorStage();
}
