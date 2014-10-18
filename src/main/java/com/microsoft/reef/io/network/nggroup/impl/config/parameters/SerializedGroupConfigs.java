/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

import java.util.Set;

@NamedParameter(doc="Serialized communication group configurations")
public final class SerializedGroupConfigs implements Name<Set<String>> {
  private SerializedGroupConfigs () {
  }
}
