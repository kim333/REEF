/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Maximum Number of Iterations.
 */
@NamedParameter(doc = "Number of iterations", short_name = "iterations", default_value = "100")
public final class Iterations implements Name<Integer> {
}
