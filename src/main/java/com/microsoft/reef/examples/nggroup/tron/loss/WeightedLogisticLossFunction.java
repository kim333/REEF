/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.reef.examples.nggroup.tron.loss;

import javax.inject.Inject;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public final class WeightedLogisticLossFunction implements LossFunction {

  private static final double POS = 0.0025;
  private static final double NEG = 0.9975;

  private final double posWeight;
  private final double negWeight;

  /**
   * Trivial constructor.
   */
  @Inject
  public WeightedLogisticLossFunction() {
    this.posWeight = (this.POS + this.NEG) / (2 * this.POS);
    this.negWeight = (this.POS + this.NEG) / (2 * this.NEG);
  }

  @Override
  public double computeLoss(final double y, final double f) {

    final double predictedTimesLabel = y * f;
    final double weight = y == -1 ? this.negWeight : this.posWeight;

    if (predictedTimesLabel >= 0) {
      return weight * Math.log(1 + Math.exp(-predictedTimesLabel));
    } else {
      return weight * (-predictedTimesLabel + Math.log(1 + Math.exp(predictedTimesLabel)));
    }
  }

  @Override
  public double computeGradient(final double y, final double f) {

    final double predictedTimesLabel = y * f;
    final double weight = y == -1 ? this.negWeight : this.posWeight;

    final double probability;
    if (predictedTimesLabel >= 0) {
      probability = 1 / (1 + Math.exp(-predictedTimesLabel));
    } else {
      final double ExpVal = Math.exp(predictedTimesLabel);
      probability = ExpVal / (1 + ExpVal);
    }

    return (probability - 1) * y * weight;
  }

  @Override
  public String toString() {
    return "WeightedLogisticLossFunction{}";
  }

  @Override
  public double computeSecondGradient (final double y, final double f) {
    throw new NotImplementedException();
  }
}
