/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.operatornames;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Name used for the Reduce operator for loss and gradient aggregation.
 */
@NamedParameter()
public final class LossAndGradientReducer implements Name<String> {
}