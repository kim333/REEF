/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.operatornames;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 * Used to identify the broadcast operator for control flow messages.
 */
@NamedParameter()
public final class ControlMessageBroadcaster implements Name<String> {
}