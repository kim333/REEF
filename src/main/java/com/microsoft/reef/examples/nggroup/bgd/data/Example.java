/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.data;

import com.microsoft.reef.examples.nggroup.bgd.math.Vector;

import java.io.Serializable;

/**
 * Base interface for Examples for linear models.
 */
public interface Example extends Serializable {

  /**
   * Access to the label.
   *
   * @return the label
   */
  double getLabel();

  /**
   * Computes the prediction for this Example, given the model w.
   * <p/>
   * w.dot(this.getFeatures())
   *
   * @param w the model
   * @return the prediction for this Example, given the model w.
   */
  double predict(Vector w);

  /**
   * Adds the current example's gradient to the gradientVector, assuming that
   * the gradient with respect to the prediction is gradient.
   */
  void addGradient(Vector gradientVector, double gradient);
}
