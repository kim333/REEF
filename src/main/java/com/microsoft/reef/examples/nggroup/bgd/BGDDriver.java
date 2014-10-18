/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.annotations.audience.DriverSide;
import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.context.ServiceConfiguration;
import com.microsoft.reef.driver.task.CompletedTask;
import com.microsoft.reef.driver.task.FailedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.reef.evaluator.context.parameters.ContextIdentifier;
import com.microsoft.reef.examples.nggroup.bgd.data.parser.Parser;
import com.microsoft.reef.examples.nggroup.bgd.data.parser.SVMLightParser;
import com.microsoft.reef.examples.nggroup.bgd.loss.LossFunction;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.ControlMessageBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.DescentDirectionBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.LineSearchEvaluationsReducer;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.LossAndGradientReducer;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.MinEtaBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.ModelAndDescentDirectionBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.ModelBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.parameters.AllCommunicationGroup;
import com.microsoft.reef.examples.nggroup.bgd.parameters.BGDControlParameters;
import com.microsoft.reef.examples.nggroup.bgd.parameters.ModelDimensions;
import com.microsoft.reef.examples.nggroup.bgd.parameters.ProbabilityOfFailure;
import com.microsoft.reef.io.data.loading.api.DataLoadingService;
import com.microsoft.reef.io.network.nggroup.api.driver.CommunicationGroupDriver;
import com.microsoft.reef.io.network.nggroup.api.driver.GroupCommDriver;
import com.microsoft.reef.io.network.nggroup.impl.config.BroadcastOperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.config.ReduceOperatorSpec;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.reef.io.serialization.SerializableCodec;
import com.microsoft.reef.poison.PoisonedConfiguration;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Configurations;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.ConfigurationSerializer;
import com.microsoft.wake.EventHandler;


@DriverSide
@Unit
public class BGDDriver {
  private static final Logger LOG = Logger.getLogger(BGDDriver.class.getName());
  private static final Tang TANG = Tang.Factory.getTang();
  private static final double STARTUP_FAILURE_PROB = 0.01;

  private final DataLoadingService dataLoadingService;
  private final GroupCommDriver groupCommDriver;
  private final ConfigurationSerializer confSerializer;
  private final CommunicationGroupDriver communicationsGroup;
  private final AtomicBoolean masterSubmitted = new AtomicBoolean(false);
  private final AtomicInteger slaveIds = new AtomicInteger(0);
  private final Map<String, RunningTask> runningTasks = new HashMap<>();
  private final AtomicBoolean jobComplete = new AtomicBoolean(false);
  private final Codec<ArrayList<Double>> lossCodec = new SerializableCodec<>();
  private final BGDControlParameters bgdControlParameters;

  private String communicationsGroupMasterContextId;

  @Inject
  public BGDDriver(final DataLoadingService dataLoadingService,
                   final GroupCommDriver groupCommDriver,
                   final ConfigurationSerializer confSerializer,
                   final BGDControlParameters bgdControlParameters) {
    this.dataLoadingService = dataLoadingService;
    this.groupCommDriver = groupCommDriver;
    this.confSerializer = confSerializer;
    this.bgdControlParameters = bgdControlParameters;

    final int minNumOfPartitions =
            bgdControlParameters.isRampup()
            ? bgdControlParameters.getMinParts()
            : dataLoadingService.getNumberOfPartitions();
    final int numParticipants = minNumOfPartitions + 1;
    this.communicationsGroup = this.groupCommDriver.newCommunicationGroup(
        AllCommunicationGroup.class,                               // NAME
        numParticipants);                                          // Number of participants
    LOG.log(Level.INFO, "Obtained entire communication group: start with {0} partitions", numParticipants);


    this.communicationsGroup
        .addBroadcast(ControlMessageBroadcaster.class,
            BroadcastOperatorSpec.newBuilder()
                .setSenderId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .build())
        .addBroadcast(ModelBroadcaster.class,
            BroadcastOperatorSpec.newBuilder()
                .setSenderId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .build())
        .addReduce(LossAndGradientReducer.class,
            ReduceOperatorSpec.newBuilder()
                .setReceiverId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .setReduceFunctionClass(LossAndGradientReduceFunction.class)
                .build())
        .addBroadcast(ModelAndDescentDirectionBroadcaster.class,
            BroadcastOperatorSpec.newBuilder()
                .setSenderId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .build())
        .addBroadcast(DescentDirectionBroadcaster.class,
            BroadcastOperatorSpec.newBuilder()
                .setSenderId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .build())
        .addReduce(LineSearchEvaluationsReducer.class,
            ReduceOperatorSpec.newBuilder()
                .setReceiverId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .setReduceFunctionClass(LineSearchReduceFunction.class)
                .build())
        .addBroadcast(MinEtaBroadcaster.class,
            BroadcastOperatorSpec.newBuilder()
                .setSenderId(MasterTask.TASK_ID)
                .setDataCodecClass(SerializableCodec.class)
                .build())
        .finalise();

    LOG.log(Level.INFO, "Added operators to communicationsGroup");
  }

  final class ContextActiveHandler implements EventHandler<ActiveContext> {

    @Override
    public void onNext(final ActiveContext activeContext) {
      LOG.log(Level.INFO, "Got active context: {0}", activeContext.getId());
      if(jobRunning(activeContext)) {
        if (!groupCommDriver.isConfigured(activeContext)) {
          // The Context is not configured with the group communications service let's do that.
          submitGroupCommunicationsService(activeContext);
        }
        else {
          // The group communications service is already active on this context. We can submit the task.
          submitTask(activeContext);
        }
      }
    }

    /**
     * @param activeContext a context to be configured with group communications.
     */
    private void submitGroupCommunicationsService(final ActiveContext activeContext) {
      final Configuration contextConf = groupCommDriver.getContextConfiguration();
      final String contextId = getContextId(contextConf);
      final Configuration serviceConf;
      if (!dataLoadingService.isDataLoadedContext(activeContext)) {
        communicationsGroupMasterContextId = contextId;
        serviceConf = groupCommDriver.getServiceConfiguration();
      } else {
        final Configuration parsedDataServiceConf = ServiceConfiguration.CONF
            .set(ServiceConfiguration.SERVICES,ExampleList.class)
            .build();
        serviceConf = Tang.Factory.getTang()
            .newConfigurationBuilder(groupCommDriver.getServiceConfiguration(), parsedDataServiceConf)
            .bindImplementation(Parser.class, SVMLightParser.class)
            .build();
      }

      LOG.log(Level.FINEST, "Submit GCContext conf: {0} and Service conf: {1}", new Object[] {
          confSerializer.toString(contextConf), confSerializer.toString(serviceConf) });

      activeContext.submitContextAndService(contextConf, serviceConf);
    }

    /**
     * @param activeContext
     */
    private void submitTask(final ActiveContext activeContext) {
      assert (groupCommDriver.isConfigured(activeContext));

      final Configuration partialTaskConfiguration;
      if (activeContext.getId().equals(communicationsGroupMasterContextId) && !masterTaskSubmitted()) {
        partialTaskConfiguration = getMasterTaskConfiguration();
        LOG.info("Submitting MasterTask conf");
      } else {
        partialTaskConfiguration = Configurations.merge(
            getSlaveTaskConfiguration(getSlaveId(activeContext)));
            //, getTaskPoisonConfiguration());
        LOG.info("Submitting SlaveTask conf");
      }
      communicationsGroup.addTask(partialTaskConfiguration);
      final Configuration taskConfiguration = groupCommDriver.getTaskConfiguration(partialTaskConfiguration);
      LOG.log(Level.FINEST, "{0}", confSerializer.toString(taskConfiguration));
      activeContext.submitTask(taskConfiguration);
    }


    /**
     * @param activeContext
     * @return
     */
    private boolean jobRunning(final ActiveContext activeContext) {
      synchronized (runningTasks) {
        if (!jobComplete.get()) {
          return true;
        } else {
          LOG.log(Level.INFO, "Job complete. Not submitting any task. Closing context {0}", activeContext);
          activeContext.close();
          return false;
        }
      }
    }


  }

  final class TaskRunningHandler implements EventHandler<RunningTask> {

    @Override
    public void onNext(final RunningTask runningTask) {
      synchronized (runningTasks) {
        if (!jobComplete.get()) {
          LOG.log(Level.INFO, "Job has not completed yet. Adding to runningTasks: {0}", runningTask);
          runningTasks.put(runningTask.getId(), runningTask);
        } else {
          LOG.log(Level.INFO, "Job complete. Closing context: {0}", runningTask.getActiveContext().getId());
          runningTask.getActiveContext().close();
        }
      }
    }
  }

  final class TaskFailedHandler implements EventHandler<FailedTask> {

    @Override
    public void onNext(final FailedTask failedTask) {
      final String failedTaskId = failedTask.getId();
      LOG.log(Level.WARNING, "Got failed Task: " + failedTaskId);//, failedTask.asError());

      if (jobRunning(failedTaskId)) {

        final ActiveContext activeContext = failedTask.getActiveContext().get();
        final Configuration partialTaskConf = getSlaveTaskConfiguration(failedTaskId);

        // Do not add the task back:
        // allCommGroup.addTask(partialTaskConf);

        final Configuration taskConf = groupCommDriver.getTaskConfiguration(partialTaskConf);
        LOG.log(Level.FINEST, "Submit SlaveTask conf: {0}", confSerializer.toString(taskConf));

        activeContext.submitTask(taskConf);
      }
    }

    /**
     * @param failedTaskId
     * @return
     */
    private boolean jobRunning(final String failedTaskId) {
      synchronized (runningTasks) {
        if (!jobComplete.get()) {
          return true;
        } else {
          final RunningTask rTask = runningTasks.remove(failedTaskId);
          LOG.log(Level.INFO, "Job has completed. Not resubmitting");
          if (rTask != null) {
            LOG.log(Level.INFO, "Closing activecontext");
            rTask.getActiveContext().close();
          }
          else {
            LOG.log(Level.INFO, "Master must have closed my context");
          }
          return false;
        }
      }
    }
  }

  final class TaskCompletedHandler implements EventHandler<CompletedTask> {

    @Override
    public void onNext(final CompletedTask task) {
      LOG.log(Level.INFO, "Got CompletedTask: {0}", task.getId());
      final byte[] retVal = task.get();
      if (retVal != null) {
        final List<Double> losses = BGDDriver.this.lossCodec.decode(retVal);
        for (final Double loss : losses) {
          System.out.println(loss);
        }
      }
      synchronized (runningTasks) {
        LOG.log(Level.INFO, "Acquired lock on runningTasks. Removing {0}", task.getId());
        final RunningTask rTask = runningTasks.remove(task.getId());
        if (rTask != null) {
          LOG.log(Level.INFO, "Closing active context: {0}", task.getActiveContext().getId());
          task.getActiveContext().close();
        } else {
          LOG.log(Level.INFO, "Master must have closed active context already for task {0}", task.getId());
        }

        if (MasterTask.TASK_ID.equals(task.getId())) {
          jobComplete.set(true);
          LOG.log(Level.INFO, "Master(=>Job) complete. Closing other running tasks: {0}", runningTasks.values());
          for (final RunningTask runTask : runningTasks.values()) {
            runTask.getActiveContext().close();
          }
          LOG.finest("Clearing runningTasks");
          runningTasks.clear();
        }
      }
    }
  }



  /**
   * @return Configuration for the MasterTask
   */
  public Configuration getMasterTaskConfiguration() {
    return Configurations.merge(
            TaskConfiguration.CONF
              .set(TaskConfiguration.IDENTIFIER, MasterTask.TASK_ID)
              .set(TaskConfiguration.TASK, MasterTask.class)
              .build(),
            bgdControlParameters.getConfiguration());
  }

  /**
   * @return Configuration for the SlaveTask
   */
  private Configuration getSlaveTaskConfiguration(final String taskId) {
    final double pSuccess = bgdControlParameters.getProbOfSuccessfulIteration();
    final int numberOfPartitions = dataLoadingService.getNumberOfPartitions();
    final double pFailure = 1 - Math.pow(pSuccess,1.0 / numberOfPartitions);
    return Tang.Factory.getTang()
        .newConfigurationBuilder(
            TaskConfiguration.CONF
                .set(TaskConfiguration.IDENTIFIER, taskId)
                .set(TaskConfiguration.TASK, SlaveTask.class)
                .build())
        .bindNamedParameter(ModelDimensions.class, "" + bgdControlParameters.getDimensions())
        .bindImplementation(LossFunction.class, bgdControlParameters.getLossFunction())
        .bindNamedParameter(ProbabilityOfFailure.class, Double.toString(pFailure))
        .build();
  }

  private Configuration getTaskPoisonConfiguration() {
    return PoisonedConfiguration.TASK_CONF
        .set(PoisonedConfiguration.CRASH_PROBABILITY, STARTUP_FAILURE_PROB)
        .set(PoisonedConfiguration.CRASH_TIMEOUT, 1)
        .build();
  }

  private String getContextId(final Configuration contextConf) {
    try {
      return TANG.newInjector(contextConf).getNamedInstance(ContextIdentifier.class);
    } catch (final InjectionException e) {
      throw new RuntimeException("Unable to inject context identifier from context conf", e);
    }
  }

  private String getSlaveId(final ActiveContext activeContext) {
    return "SlaveTask-" + slaveIds.getAndIncrement();
  }

  private boolean masterTaskSubmitted() {
    return !masterSubmitted.compareAndSet(false, true);
  }

}
