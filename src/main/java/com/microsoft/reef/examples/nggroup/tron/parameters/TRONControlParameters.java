/*
 * Copyright 2013 Microsoft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.reef.examples.nggroup.tron.parameters;

import javax.inject.Inject;

import com.microsoft.reef.examples.nggroup.tron.loss.LossFunction;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.formats.CommandLine;

public final class TRONControlParameters {

  private final int dimensions;
  private final double lambda;
  private final double eps;
  private final int iters;
  private final int minParts;
  private final boolean rampup;

  private final double probOfSuccessfulIteration;
  private final TRONLossType lossType;

  @Inject
  public TRONControlParameters(
          final @Parameter(ModelDimensions.class) int dimensions,
          final @Parameter(Lambda.class) double lambda,
          final @Parameter(Eps.class) double eps,
          final @Parameter(ProbabilityOfSuccesfulIteration.class) double probOfSuccessfulIteration,
          final @Parameter(Iterations.class) int iters,
          final @Parameter(EnableRampup.class) boolean rampup,
          final @Parameter(MinParts.class) int minParts,
          final TRONLossType lossType) {
    this.dimensions = dimensions;
    this.lambda = lambda;
    this.eps = eps;
    this.probOfSuccessfulIteration = probOfSuccessfulIteration;
    this.iters = iters;
    this.rampup = rampup;
    this.minParts = minParts;
    this.lossType = lossType;
  }

  public Configuration getConfiguration() {
    return Tang.Factory.getTang().newConfigurationBuilder()
        .bindNamedParameter(ModelDimensions.class, Integer.toString(this.dimensions))
        .bindNamedParameter(Lambda.class, Double.toString(this.lambda))
        .bindNamedParameter(Eps.class, Double.toString(this.eps))
        .bindNamedParameter(ProbabilityOfSuccesfulIteration.class, Double.toString(probOfSuccessfulIteration))
        .bindNamedParameter(Iterations.class, Integer.toString(this.iters))
        .bindNamedParameter(EnableRampup.class, Boolean.toString(this.rampup))
        .bindNamedParameter(MinParts.class, Integer.toString(this.minParts))
        .bindNamedParameter(LossFunctionType.class, lossType.lossFunctionString())
        .build();
  }

  public static CommandLine registerShortNames(final CommandLine commandLine) {
    return commandLine
        .registerShortNameOfClass(ModelDimensions.class)
        .registerShortNameOfClass(Lambda.class)
        .registerShortNameOfClass(Eps.class)
        .registerShortNameOfClass(ProbabilityOfSuccesfulIteration.class)
        .registerShortNameOfClass(Iterations.class)
        .registerShortNameOfClass(EnableRampup.class)
        .registerShortNameOfClass(MinParts.class)
        .registerShortNameOfClass(LossFunctionType.class);
  }

  public int getDimensions() {
    return this.dimensions;
  }

  public double getLambda() {
    return this.lambda;
  }

  public double getEps() {
    return this.eps;
  }

  public double getProbOfSuccessfulIteration() {
    return probOfSuccessfulIteration;
  }

  public int getIters() {
    return this.iters;
  }

  public int getMinParts() {
    return this.minParts;
  }

  public boolean isRampup() {
    return this.rampup;
  }

  public Class<? extends LossFunction> getLossFunction() {
    return this.lossType.getLossFunction();
  }
}
