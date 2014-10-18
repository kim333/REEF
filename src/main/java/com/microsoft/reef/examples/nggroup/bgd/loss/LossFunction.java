/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.loss;

import com.microsoft.tang.annotations.DefaultImplementation;

/**
 * Interface for Loss Functions.
 */
@DefaultImplementation(SquaredErrorLossFunction.class)
public interface LossFunction {

  /**
   * Computes the loss incurred by predicting f, if y is the true label.
   *
   * @param y the label
   * @param f the prediction
   * @return the loss incurred by predicting f, if y is the true label.
   */
  double computeLoss(final double y, final double f);

  /**
   * Computes the gradient with respect to f, if y is the true label.
   *
   * @param y the label
   * @param f the prediction
   * @return the gradient with respect to f
   */
  double computeGradient(final double y, final double f);
}
