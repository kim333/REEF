/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Break criterion for the optimizer. If the progress in mean loss between
 * two iterations is less than this, the optimization stops.
 */
@NamedParameter(short_name = "psuccess", default_value = "0.5")
public final class ProbabilityOfSuccesfulIteration implements Name<Double> {
}