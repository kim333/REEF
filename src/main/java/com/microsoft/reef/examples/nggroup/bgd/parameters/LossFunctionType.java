/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

/**
 *
 */
@NamedParameter(doc="Loss Function to be used: logLoss|weightedLogLoss|squaredError", short_name="loss", default_value="logLoss")
public class LossFunctionType implements Name<String> {

}
