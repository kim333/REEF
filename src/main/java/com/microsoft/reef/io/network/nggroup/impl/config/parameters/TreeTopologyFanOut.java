/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config.parameters;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;

@NamedParameter(doc = "The fan out for the tree topology", default_value="2", short_name="fanout")
public final class TreeTopologyFanOut implements Name<Integer> {
  private TreeTopologyFanOut () {
  }
}
