/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.loss;

import javax.inject.Inject;

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
  public double computeLoss(double y, double f) {

    final double predictedTimesLabel = y * f;
    final double weight = y == -1 ? this.negWeight : this.posWeight;

    if (predictedTimesLabel >= 0) {
      return weight * Math.log(1 + Math.exp(-predictedTimesLabel));
    } else {
      return weight * (-predictedTimesLabel + Math.log(1 + Math.exp(predictedTimesLabel)));
    }
  }

  @Override
  public double computeGradient(double y, double f) {

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
}
