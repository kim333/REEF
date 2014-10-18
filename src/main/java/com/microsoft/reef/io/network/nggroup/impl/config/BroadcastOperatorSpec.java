/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config;

import com.microsoft.reef.io.network.nggroup.api.config.OperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.serialization.Codec;


/**
 * The specification for the broadcast operator
 */
public class BroadcastOperatorSpec implements OperatorSpec {
  private final String senderId;

  /**
   * Codec to be used to serialize data
   */
  private final Class<? extends Codec> dataCodecClass;


  public BroadcastOperatorSpec(final String senderId,
                               final Class<? extends Codec> dataCodecClass) {
    super();
    this.senderId = senderId;
    this.dataCodecClass = dataCodecClass;
  }

  public String getSenderId() {
    return senderId;
  }

  @Override
  public Class<? extends Codec> getDataCodecClass() {
    return dataCodecClass;
  }

  @Override
  public String toString () {
    return "Broadcast Operator Spec: [sender=" + senderId + "] [dataCodecClass=" + Utils.simpleName(dataCodecClass)
           + "]";
  }

  public static Builder newBuilder() {
    return new BroadcastOperatorSpec.Builder();
  }

  public static class Builder implements com.microsoft.reef.util.Builder<BroadcastOperatorSpec> {
    private String senderId;

    private Class<? extends Codec> dataCodecClass;


    public Builder setSenderId(final String senderId) {
      this.senderId = senderId;
      return this;
    }

    public Builder setDataCodecClass(final Class<? extends Codec> codecClazz) {
      this.dataCodecClass = codecClazz;
      return this;
    }

    @Override
    public BroadcastOperatorSpec build() {
      return new BroadcastOperatorSpec(senderId, dataCodecClass);
    }
  }

}
