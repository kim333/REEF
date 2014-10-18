/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * The regularization constant
 */
@NamedParameter(doc = "The regularization constant", short_name = "lambda", default_value = "1e-4")
public final class Lambda implements Name<Double> {
}
