/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.config;

import com.microsoft.reef.io.network.group.config.OP_TYPE;
import com.microsoft.wake.remote.Codec;

/**
 * Base class for description of group communication operators
 */
public class GroupOperatorDescription {
  /** Type of the operator */
  public final OP_TYPE operatorType;

  /** Codec to be used to serialize data */
  public final Class<? extends Codec<?>> dataCodecClass;

  /**
   * Constructor for fields
   * 
   * @param operatorType
   * @param dataCodecClass
   */
  public GroupOperatorDescription(OP_TYPE operatorType,
      Class<? extends Codec<?>> dataCodecClass) {
    super();
    this.operatorType = operatorType;
    this.dataCodecClass = dataCodecClass;
  }
}
