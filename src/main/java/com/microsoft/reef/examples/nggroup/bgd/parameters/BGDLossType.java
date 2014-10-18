/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.parameters;

import javax.inject.Inject;

import com.microsoft.reef.examples.nggroup.bgd.loss.LogisticLossFunction;
import com.microsoft.reef.examples.nggroup.bgd.loss.LossFunction;
import com.microsoft.reef.examples.nggroup.bgd.loss.SquaredErrorLossFunction;
import com.microsoft.reef.examples.nggroup.bgd.loss.WeightedLogisticLossFunction;
import com.microsoft.tang.annotations.Parameter;

import java.util.HashMap;
import java.util.Map;

public class BGDLossType {

  private static final Map<String, Class<? extends LossFunction>> LOSS_FUNCTIONS =
      new HashMap<String, Class<? extends LossFunction>>() {{
        put("logLoss", LogisticLossFunction.class);
        put("weightedLogLoss", WeightedLogisticLossFunction.class);
        put("squaredError", SquaredErrorLossFunction.class);
  }};

  private final Class<? extends LossFunction> lossFunction;

  private final String lossFunctionStr;

  @Inject
  public BGDLossType(@Parameter(LossFunctionType.class) final String lossFunctionStr) {
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
