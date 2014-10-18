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
package com.microsoft.reef.examples.nggroup.tron;

import com.microsoft.reef.examples.nggroup.tron.math.DenseVector;
import com.microsoft.reef.examples.nggroup.tron.math.Vector;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.util.Pair;

import javax.inject.Inject;

public class SumUpIntVecPairReduceFunction
    implements ReduceFunction<Pair<Integer, Vector>> {

  @Inject
  public SumUpIntVecPairReduceFunction() {
  }

  @Override
  public Pair<Integer, Vector> apply(
      final Iterable<Pair<Integer, Vector>> pgs) {

    int numEx = 0;
    Vector combinedProjectedDirection = null;

    for (final Pair<Integer, Vector> pg : pgs) {
      if (combinedProjectedDirection == null) {
        combinedProjectedDirection = new DenseVector(pg.second);
      } else {
        combinedProjectedDirection.add(pg.second);
      }
      numEx += pg.first;
    }

    return new Pair<>(numEx, combinedProjectedDirection);
  }
}
