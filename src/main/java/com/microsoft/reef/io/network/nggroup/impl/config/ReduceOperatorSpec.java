/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.config;

import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.nggroup.api.config.OperatorSpec;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.serialization.Codec;

/**
 * The specification for the Reduce operator
 */
public class ReduceOperatorSpec implements OperatorSpec {

  private final String receiverId;

  /**
   * Codec to be used to serialize data
   */
  private final Class<? extends Codec> dataCodecClass;

  /**
   * The reduce function to be used for operations that do reduction
   */
  public final Class<? extends ReduceFunction> redFuncClass;


  public ReduceOperatorSpec(final String receiverId,
                            final Class<? extends Codec> dataCodecClass,
                            final Class<? extends ReduceFunction> redFuncClass) {
    super();
    this.receiverId = receiverId;
    this.dataCodecClass = dataCodecClass;
    this.redFuncClass = redFuncClass;
  }

  public String getReceiverId() {
    return receiverId;
  }

  /**
   * @return the redFuncClass
   */
  public Class<? extends ReduceFunction> getRedFuncClass() {
    return redFuncClass;
  }

  @Override
  public Class<? extends Codec> getDataCodecClass() {
    return dataCodecClass;
  }

  @Override
  public String toString () {
    return "Reduce Operator Spec: [receiver=" + receiverId + "] [dataCodecClass=" + Utils.simpleName(dataCodecClass)
           + "] [reduceFunctionClass=" + Utils.simpleName(redFuncClass) + "]";
  }

  public static Builder newBuilder() {
    return new ReduceOperatorSpec.Builder();
  }

  public static class Builder implements com.microsoft.reef.util.Builder<ReduceOperatorSpec> {

    private String receiverId;

    private Class<? extends Codec> dataCodecClass;

    private Class<? extends ReduceFunction> redFuncClass;

    public Builder setReceiverId(final String receiverId) {
      this.receiverId = receiverId;
      return this;
    }

    public Builder setDataCodecClass(final Class<? extends Codec> codecClazz) {
      this.dataCodecClass = codecClazz;
      return this;
    }

    public Builder setReduceFunctionClass(final Class<? extends ReduceFunction> redFuncClass) {
      this.redFuncClass = redFuncClass;
      return this;
    }

    @Override
    public ReduceOperatorSpec build() {
      return new ReduceOperatorSpec(receiverId, dataCodecClass, redFuncClass);
    }
  }
}
