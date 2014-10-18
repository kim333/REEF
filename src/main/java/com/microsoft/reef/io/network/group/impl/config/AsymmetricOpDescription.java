/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.config;

import com.microsoft.reef.io.network.group.config.OP_TYPE;
import com.microsoft.wake.remote.Codec;

/**
 * This is a type of {@link GroupOperatorDescription} and is actually a marker
 * for better readability
 * 
 * This is the base class for descriptions of all asymmetric operators
 */
public class AsymmetricOpDescription extends GroupOperatorDescription {

  public AsymmetricOpDescription(OP_TYPE operatorType,
      Class<? extends Codec<?>> dataCodecClass) {
    super(operatorType, dataCodecClass);
  }

}
