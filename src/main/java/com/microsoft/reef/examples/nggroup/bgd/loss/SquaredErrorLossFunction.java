/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.loss;

import javax.inject.Inject;

/**
 * The Squarred Error {@link LossFunction}.
 *
 * @author Markus Weimer <mweimer@microsoft.com>
 */
public class SquaredErrorLossFunction implements LossFunction {

  /**
   * Trivial constructor.
   */
  @Inject
  public SquaredErrorLossFunction() {
  }

  @Override
  public double computeLoss(double y, double f) {
    return Math.pow(y - f, 2.0);
  }

  @Override
  public double computeGradient(double y, double f) {
    return (f - y) * 0.5;
  }

  @Override
  public String toString() {
    return "SquaredErrorLossFunction{}";
  }
}
