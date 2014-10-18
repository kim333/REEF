/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd;

import com.microsoft.reef.examples.nggroup.bgd.math.DenseVector;
import com.microsoft.reef.examples.nggroup.bgd.math.Vector;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.util.Pair;

import javax.inject.Inject;

public class LossAndGradientReduceFunction
    implements ReduceFunction<Pair<Pair<Double, Integer>, Vector>> {

  @Inject
  public LossAndGradientReduceFunction() {
  }

  @Override
  public Pair<Pair<Double, Integer>, Vector> apply(
      final Iterable<Pair<Pair<Double, Integer>, Vector>> lags) {

    double lossSum = 0.0;
    int numEx = 0;
    Vector combinedGradient = null;

    for (final Pair<Pair<Double,Integer>, Vector> lag : lags) {
      if (combinedGradient == null) {
        combinedGradient = new DenseVector(lag.second);
      } else {
        combinedGradient.add(lag.second);
      }
      lossSum += lag.first.first;
      numEx += lag.first.second;
    }

    return new Pair<>(new Pair<>(lossSum, numEx), combinedGradient);
  }
}
