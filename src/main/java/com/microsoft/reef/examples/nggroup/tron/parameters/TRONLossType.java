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
package com.microsoft.reef.examples.nggroup.tron.parameters;

import javax.inject.Inject;

import com.microsoft.reef.examples.nggroup.tron.loss.LogisticLossFunction;
import com.microsoft.reef.examples.nggroup.tron.loss.LossFunction;
import com.microsoft.reef.examples.nggroup.tron.loss.SquaredErrorLossFunction;
import com.microsoft.reef.examples.nggroup.tron.loss.WeightedLogisticLossFunction;
import com.microsoft.tang.annotations.Parameter;

import java.util.HashMap;
import java.util.Map;

public class TRONLossType {

  private static final Map<String, Class<? extends LossFunction>> LOSS_FUNCTIONS =
      new HashMap<String, Class<? extends LossFunction>>() {{
        put("logLoss", LogisticLossFunction.class);
        put("weightedLogLoss", WeightedLogisticLossFunction.class);
        put("squaredError", SquaredErrorLossFunction.class);
  }};

  private final Class<? extends LossFunction> lossFunction;

  private final String lossFunctionStr;

  @Inject
  public TRONLossType(@Parameter(LossFunctionType.class) final String lossFunctionStr) {
    this.lossFunctionStr = lossFunctionStr;
    this.lossFunction = LOSS_FUNCTIONS.get(lossFunctionStr);
    if (this.lossFunction == null) {
      throw new RuntimeException("Specified loss function type: " + lossFunctionStr +
              " is not implemented. Supported types are logLoss|weightedLogLoss|squaredError");
    }
  }

  public Class<? extends LossFunction> getLossFunction() {
    return this.lossFunction;
  }

  public String lossFunctionString() {
    return lossFunctionStr;
  }
}
