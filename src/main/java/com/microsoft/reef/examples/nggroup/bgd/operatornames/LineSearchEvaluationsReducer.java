/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.operatornames;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Name of the reducer used to aggregate line search results.
 */
@NamedParameter()
public final class LineSearchEvaluationsReducer implements Name<String> {
}