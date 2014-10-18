/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

@NamedParameter(doc = "Name of the operator")
public final class OperatorName implements Name<String> {
  private OperatorName () {
  }
}
