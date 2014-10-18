/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;

@NamedParameter(doc="The reduce function class that is associated with a reduce operator")
public final class ReduceFunctionParam implements Name<ReduceFunction> {
  private ReduceFunctionParam () {
  }
}
