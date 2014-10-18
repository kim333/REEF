/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Maximum Number of Iterations.
 */
@NamedParameter(doc = "Should we ram-up?", short_name = "rampup", default_value = "false")
public final class EnableRampup implements Name<Boolean> {
}
