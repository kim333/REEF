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
package com.microsoft.reef.examples.nggroup.tron.operations.CG;

import com.microsoft.reef.examples.nggroup.tron.math.DenseVector;
import com.microsoft.reef.examples.nggroup.tron.math.Vector;

public class TestProjector implements CGDirectionProjector {

  private final Vector[] matrix;

  public TestProjector (final Vector[] matrix) {
    this.matrix = matrix;
  }

  @Override
  public void project (final Vector CGDirection, final Vector projectedCGDirection) {
    for(int i=0;i<matrix.length;i++) {
      final Vector row = matrix[i];
      projectedCGDirection.set(i, row.dot(CGDirection));
    }
  }

}