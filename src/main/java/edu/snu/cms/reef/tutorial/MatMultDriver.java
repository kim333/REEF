/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package edu.snu.cms.reef.tutorial;
import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.evaluator.AllocatedEvaluator;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.driver.evaluator.EvaluatorRequestor;
import com.microsoft.reef.driver.task.CompletedTask;
import com.microsoft.reef.driver.task.RunningTask;

//import com.microsoft.reef.examples.utils.wake.BlockingEventHandler;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.impl.BlockingEventHandler;
import com.microsoft.wake.time.event.StartTime;

import javax.inject.Inject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Driver code for the Matrix Multiplication REEF Application
 */
@Unit
public final class MatMultDriver {

  /**
   * Standard Java logger object.
   */
  private final Logger LOG = Logger.getLogger(MatMultDriver.class.getName());

  /**
   * The number of compute tasks to be spawned
   */
  private final int computeTasks;

  /**
   * The sole Control Task
   */
  private static final int controllerTasks = 1;

  /**
   * Track the number of compute tasks that are running
   */
  private final AtomicInteger compTasksRunning = new AtomicInteger(0);

  /**
   * Task submission is delegated to
   */
  private final TaskSubmitter taskSubmitter;

  /**
   * Blocks till all evaluators are available and submits tasks using
   * task submitter. First all the compute tasks are submitted. Then
   * the control task is submitted.
   */
  private final BlockingEventHandler<ActiveContext> contextAccumulator;

  /**
   * Request evaluators using this
   */
  private final EvaluatorRequestor requestor;


  public static class Parameters {
    @NamedParameter(default_value = "5", doc = "The number of compute tasks to spawn")
    public static class ComputeTasks implements Name<Integer> {
    }

    @NamedParameter(default_value = "5678", doc = "Port on which Name Service should listen")
    public static class NameServicePort implements Name<Integer> {
    }
    
    @NamedParameter(doc = "Whether or not to run on the local runtime",
    short_name = "local", default_value = "true")
    public static final class Local implements Name<Boolean> {
    }

    @NamedParameter(doc = "Number of minutes before timeout",
    short_name = "timeout", default_value = "2")
    public static final class TimeOut implements Name<Integer> {
    }

    @NamedParameter(short_name = "input")
    public static final class InputDir implements Name<String> {
    }
    
    @NamedParameter(short_name = "learnRate", default_value = "0.5")
    public static final class LearnRate implements Name<Double> {
    }
    @NamedParameter(short_name = "numParam", default_value = "10")
    public static final class NumParam implements Name<Integer> {
    }
    @NamedParameter(short_name = "targetParam", default_value = "10")
    public static final class TargetParam implements Name<Integer> {
    }
  }

  /**
   * This class is instantiated by TANG
   *
   * @param requestor       evaluator requestor object used to create new evaluator
   *                        containers.
   * @param computeTasks    - named parameter
   * @param nameServicePort - named parameter
   */
  @Inject
  public MatMultDriver(
      final EvaluatorRequestor requestor,
      final @Parameter(Parameters.ComputeTasks.class) int computeTasks,
      final @Parameter(Parameters.NameServicePort.class) int nameServicePort,
      final @Parameter(Parameters.InputDir.class) String inputDir,
      final @Parameter(Parameters.NumParam.class) int numParam,
      final @Parameter(Parameters.LearnRate.class) double learnRate,
      final @Parameter(Parameters.TargetParam.class) int targetParam) {
    this.requestor = requestor;
    this.computeTasks = computeTasks;
    this.taskSubmitter = new TaskSubmitter(this.computeTasks, nameServicePort,learnRate,numParam,targetParam);
    this.contextAccumulator = new BlockingEventHandler<>(
        this.computeTasks + this.controllerTasks, this.taskSubmitter);
  }

  /**
   * Evaluator allocated.
   */
  final class AllocatedEvaluatorHandler implements EventHandler<AllocatedEvaluator> {
    @Override
    public final void onNext(final AllocatedEvaluator eval) {
      LOG.log(Level.INFO, "Received an AllocatedEvaluator. Submitting it.");
      try {
        eval.submitContext(ContextConfiguration.CONF.set(ContextConfiguration.IDENTIFIER, "MatMult").build());
      } catch (final BindException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Task is running. Track the compute tasks that are running.
   * Once all compute tasks are running submitTask the ControllerTask.
   */
  final class RunningTaskHandler implements EventHandler<RunningTask> {
    @Override
    public final void onNext(final RunningTask task) {
      LOG.log(Level.INFO, "Task \"{0}\" is running!", task.getId());
      if (compTasksRunning.incrementAndGet() == computeTasks) {
        // All compute tasks are running - launch controller task
        taskSubmitter.submitControlTask();
      }
    }
  }

  /**
   * Task has completed successfully.
   */
  final class CompletedTaskHandler implements EventHandler<CompletedTask> {
    @Override
    @SuppressWarnings("ConvertToTryWithResources")
    public final void onNext(final CompletedTask completed) {
      LOG.log(Level.INFO, "Task {0} is done.", completed.getId());
      if (taskSubmitter.controllerCompleted(completed.getId())) {
        // Get results from controller
        System.out.println("****************** RESULT ******************");
        System.out.println(new String(completed.get()));
        System.out.println("********************************************");
      }
      final ActiveContext context = completed.getActiveContext();
      LOG.log(Level.INFO, "Releasing Context {0}.", context.getId());
      context.close();
    }
  }

  final class ActiveContextHandler implements EventHandler<ActiveContext> {
    @Override
    public void onNext(final ActiveContext activeContext) {
      LOG.log(Level.INFO, "Received a RunningEvaluator with ID: {0}", activeContext.getId());
      contextAccumulator.onNext(activeContext);
    }
  }

  final class StartHandler implements EventHandler<StartTime> {
    @Override
    public void onNext(final StartTime startTime) {
      LOG.log(Level.INFO, "StartTime: {0}", startTime);
      MatMultDriver.this.requestor.submit(EvaluatorRequest.newBuilder()
          .setNumber(computeTasks + controllerTasks)
          .setMemory(128)
//          .setNumberOfCores(1)
          .build());
    }

    @Override
    public String toString() {
      return "HelloDriver.StartHandler";
    }
  }
}
