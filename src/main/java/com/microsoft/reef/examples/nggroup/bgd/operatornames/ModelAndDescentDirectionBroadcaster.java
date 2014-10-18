/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.operatornames;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Name of the broadcast operator used to send a model and descent direction during line search.
 */
@NamedParameter()
public final class ModelAndDescentDirectionBroadcaster implements Name<String> {
}