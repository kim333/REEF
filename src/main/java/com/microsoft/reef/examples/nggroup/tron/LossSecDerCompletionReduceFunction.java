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

import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import javax.inject.Inject;

public class LossSecDerCompletionReduceFunction
    implements ReduceFunction<Boolean> {

  @Inject
  public LossSecDerCompletionReduceFunction() {
  }

  @Override
  public Boolean apply(
      final Iterable<Boolean> completions) {
    for(final Boolean b : completions) {
      if(!b) {
        return false;
      }
    }
    return true;
  }
}