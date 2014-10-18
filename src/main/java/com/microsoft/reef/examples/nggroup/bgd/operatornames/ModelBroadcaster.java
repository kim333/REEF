/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.operatornames;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * The name of the broadcast operator used for model broadcasts.
 */
@NamedParameter()
public final class ModelBroadcaster implements Name<String> {
}