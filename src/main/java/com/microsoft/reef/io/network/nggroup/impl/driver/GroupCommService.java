/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.evaluator.FailedEvaluator;
import com.microsoft.reef.driver.parameters.EvaluatorDispatcherThreads;
import com.microsoft.reef.driver.parameters.ServiceEvaluatorFailedHandlers;
import com.microsoft.reef.driver.parameters.ServiceTaskFailedHandlers;
import com.microsoft.reef.driver.parameters.TaskRunningHandlers;
import com.microsoft.reef.driver.task.FailedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.io.network.nggroup.api.driver.GroupCommServiceDriver;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.TreeTopologyFanOut;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.formats.AvroConfigurationSerializer;
import com.microsoft.tang.formats.ConfigurationSerializer;
import com.microsoft.wake.EventHandler;

/**
 * The Group Communication Service
 */
@Unit
public class GroupCommService {

  private static final Logger LOG = Logger.getLogger(GroupCommService.class.getName());
  private static final ConfigurationSerializer confSer = new AvroConfigurationSerializer();

  private final GroupCommServiceDriver groupCommDriver;

  @Inject
  public GroupCommService (final GroupCommServiceDriver groupCommDriver) {
    this.groupCommDriver = groupCommDriver;
  }

  public static Configuration getConfiguration () {
    LOG.entering("GroupCommService", "getConfiguration");
    final JavaConfigurationBuilder jcb = Tang.Factory.getTang().newConfigurationBuilder();
    jcb.bindSetEntry(TaskRunningHandlers.class, RunningTaskHandler.class);
    jcb.bindSetEntry(ServiceTaskFailedHandlers.class, FailedTaskHandler.class);
    jcb.bindSetEntry(ServiceEvaluatorFailedHandlers.class, FailedEvaluatorHandler.class);
    jcb.bindNamedParameter(EvaluatorDispatcherThreads.class, "1");
    final Configuration retVal = jcb.build();
    LOG.exiting("GroupCommService", "getConfiguration", confSer.toString(retVal));
    return retVal;
  }

  public static Configuration getConfiguration (final int fanOut) {
    LOG.entering("GroupCommService", "getConfiguration", fanOut);
    final Configuration baseConf = getConfiguration();
    final Configuration retConf = Tang.Factory.getTang().newConfigurationBuilder(baseConf)
                                        .bindNamedParameter(TreeTopologyFanOut.class, Integer.toString(fanOut)).build();
    LOG.exiting("GroupCommService", "getConfiguration", confSer.toString(retConf));
    return retConf;
  }

  public class FailedEvaluatorHandler implements EventHandler<FailedEvaluator> {

    @Override
    public void onNext (final FailedEvaluator failedEvaluator) {
      LOG.entering("GroupCommService.FailedEvaluatorHandler", "onNext", failedEvaluator.getId());
      groupCommDriver.getGroupCommFailedEvaluatorStage().onNext(failedEvaluator);
      LOG.exiting("GroupCommService.FailedEvaluatorHandler", "onNext", failedEvaluator.getId());
    }

  }

  public class RunningTaskHandler implements EventHandler<RunningTask> {

    @Override
    public void onNext (final RunningTask runningTask) {
      LOG.entering("GroupCommService.RunningTaskHandler", "onNext", runningTask.getId());
      groupCommDriver.getGroupCommRunningTaskStage().onNext(runningTask);
      LOG.exiting("GroupCommService.RunningTaskHandler", "onNext", runningTask.getId());
    }

  }

  public class FailedTaskHandler implements EventHandler<FailedTask> {

    @Override
    public void onNext (final FailedTask failedTask) {
      LOG.entering("GroupCommService.FailedTaskHandler", "onNext", failedTask.getId());
      groupCommDriver.getGroupCommFailedTaskStage().onNext(failedTask);
      LOG.exiting("GroupCommService.FailedTaskHandler", "onNext", failedTask.getId());
    }

  }

}
