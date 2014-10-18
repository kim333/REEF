/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd;

import java.io.Serializable;

public enum ControlMessages implements Serializable {
  ComputeGradientWithModel,
  ComputeGradientWithMinEta,
  DoLineSearch,
  DoLineSearchWithModel,
  Synchronize,
  Stop
}