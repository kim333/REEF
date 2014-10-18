/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package edu.snu.cms.reef.tutorial;
import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import edu.snu.cms.reef.tutorial.MatMultDriver;
import com.microsoft.reef.io.data.loading.api.DataLoadingRequestBuilder;

import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.runtime.yarn.client.YarnClientConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.CommandLine;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.mapred.TextInputFormat;

/**
 * The Client for the Matrix Multiplication REEF example
 * using REEF group communication operators.
 */
public final class MatMultREEF {
  private static final Logger LOG = Logger.getLogger(MatMultREEF.class.getName());

  public static LauncherStatus run(final Configuration runtimeConfiguration,  EvaluatorRequest computeRequest, String inputDir
		  							, double learnRate, int numParam, int targetParam) {
    try {
     final Configuration dataLoadConfiguration = new DataLoadingRequestBuilder()
         .setMemoryMB(1024)
         .setInputFormatClass(TextInputFormat.class)
         .setInputPath(inputDir)
         .setNumberOfDesiredSplits(5)
         .setComputeRequest(computeRequest)
         .setDriverConfigurationModule(DriverConfiguration.CONF
                 .set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(MatMultDriver.class))
                 .set(DriverConfiguration.ON_DRIVER_STARTED, MatMultDriver.StartHandler.class)
                 .set(DriverConfiguration.DRIVER_IDENTIFIER, "MatrixMultiply")
                 .set(DriverConfiguration.ON_EVALUATOR_ALLOCATED, MatMultDriver.AllocatedEvaluatorHandler.class)
                 .set(DriverConfiguration.ON_TASK_RUNNING, MatMultDriver.RunningTaskHandler.class)
                 .set(DriverConfiguration.ON_TASK_COMPLETED, MatMultDriver.CompletedTaskHandler.class)
                 .set(DriverConfiguration.ON_CONTEXT_ACTIVE, MatMultDriver.ActiveContextHandler.class)
         )
         .build();
    

     final Configuration  driverConfiguration = Tang.Factory.getTang().newConfigurationBuilder(
    	        dataLoadConfiguration)
    	        .bindNamedParameter(MatMultDriver.Parameters.InputDir.class, inputDir)
    	        .bindNamedParameter(MatMultDriver.Parameters.NumParam.class, ""+numParam)
    	        .bindNamedParameter(MatMultDriver.Parameters.LearnRate.class, ""+learnRate)
    	        .bindNamedParameter(MatMultDriver.Parameters.TargetParam.class, ""+targetParam)
    	        .build();
    	/*
      final Configuration driverConfiguration = DriverConfiguration.CONF
          .set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(MatMultDriver.class))
          .set(DriverConfiguration.ON_DRIVER_STARTED, MatMultDriver.StartHandler.class)
          .set(DriverConfiguration.DRIVER_IDENTIFIER, "MatrixMultiply")
          .set(DriverConfiguration.ON_EVALUATOR_ALLOCATED, MatMultDriver.AllocatedEvaluatorHandler.class)
          .set(DriverConfiguration.ON_TASK_RUNNING, MatMultDriver.RunningTaskHandler.class)
          .set(DriverConfiguration.ON_TASK_COMPLETED, MatMultDriver.CompletedTaskHandler.class)
          .set(DriverConfiguration.ON_CONTEXT_ACTIVE, MatMultDriver.ActiveContextHandler.class)
          .build();
      */
      
      
      return DriverLauncher.getLauncher(runtimeConfiguration).run(driverConfiguration, 100000);
    } catch (final BindException | InjectionException ex) {
      LOG.log(Level.SEVERE, "Fatal Exception during job", ex);
      return LauncherStatus.FAILED(ex);
    }
  }

  /**
   * Start MatMult REEF job. Runs method runMatMultReef().
   *
   * @param args command line parameters.
   * @throws BindException      configuration error.
   * @throws InjectionException configuration error.
 * @throws IOException 
   */
  public static void main(final String[] args) throws BindException, InjectionException, IOException {
	final Tang tang = Tang.Factory.getTang();

	final JavaConfigurationBuilder cb = tang.newConfigurationBuilder();

	new CommandLine(cb)
        .registerShortNameOfClass(MatMultDriver.Parameters.Local.class)
        .registerShortNameOfClass(MatMultDriver.Parameters.TimeOut.class)
        .registerShortNameOfClass(MatMultDriver.Parameters.InputDir.class)
        .registerShortNameOfClass(MatMultDriver.Parameters.LearnRate.class)
        .registerShortNameOfClass(MatMultDriver.Parameters.NumParam.class)
        .registerShortNameOfClass(MatMultDriver.Parameters.TargetParam.class)
        .processCommandLine(args);
	  
    final Injector injector = tang.newInjector(cb.build());

    final boolean isLocal = injector.getNamedInstance(MatMultDriver.Parameters.Local.class);
    final int jobTimeout = injector.getNamedInstance(MatMultDriver.Parameters.TimeOut.class) * 60 * 1000;
    final String inputDir = injector.getNamedInstance(MatMultDriver.Parameters.InputDir.class);
    final double learnRate = injector.getNamedInstance(MatMultDriver.Parameters.LearnRate.class);
    final int numParam = injector.getNamedInstance(MatMultDriver.Parameters.NumParam.class);
    final int targetParam = injector.getNamedInstance(MatMultDriver.Parameters.TargetParam.class);

    final Configuration runtimeConfiguration;
    if (isLocal) {
      LOG.log(Level.INFO, "Running Data Loading demo on the local runtime");
      runtimeConfiguration = LocalRuntimeConfiguration.CONF
          .set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, 6)
          .build();
    } else {
      LOG.log(Level.INFO, "Running Data Loading demo on YARN");
      runtimeConfiguration = YarnClientConfiguration.CONF.build();
    }

    final EvaluatorRequest computeRequest = EvaluatorRequest.newBuilder()
            .setNumber(1)
            .setMemory(512)
       //     .setNumberOfCores(1)
            .build();

   
    final LauncherStatus status = run(runtimeConfiguration, computeRequest, inputDir, learnRate, numParam, targetParam);
    LOG.log(Level.INFO, "Matrix multiply returned: {0}", status);
  }
}
