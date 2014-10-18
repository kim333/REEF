/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd;

import com.microsoft.reef.examples.nggroup.bgd.math.DenseVector;
import com.microsoft.reef.examples.nggroup.bgd.math.Vector;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.util.Pair;

import javax.inject.Inject;

public class LineSearchReduceFunction implements ReduceFunction<Pair<Vector, Integer>> {

  @Inject
  public LineSearchReduceFunction() {
  }

  @Override
  public Pair<Vector, Integer> apply(final Iterable<Pair<Vector, Integer>> evals) {

    Vector combinedEvaluations = null;
    int numEx = 0;

    for (final Pair<Vector, Integer> eval : evals) {
      if (combinedEvaluations == null) {
        combinedEvaluations = new DenseVector(eval.first);
      } else {
        combinedEvaluations.add(eval.first);
      }
      numEx += eval.second;
    }

    return new Pair<>(combinedEvaluations, numEx);
  }
}
