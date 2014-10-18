/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.basic;

import com.microsoft.reef.io.network.group.impl.operators.ReceiverHelper;
import com.microsoft.reef.io.network.group.impl.operators.SenderHelper;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Gather;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Scatter;
import com.microsoft.wake.ComparableIdentifier;
import com.microsoft.wake.Identifier;

import java.util.List;

/**
 * The base class for Senders of Asymmetric operators
 * {@link Scatter}, {@link Broadcast}, {@link Gather}, {@link Reduce}
 *
 * @param <T>
 */
public class SenderBase<T> extends SenderReceiverBase {
  protected SenderHelper<T> dataSender;
  protected ReceiverHelper<String> ackReceiver;

  public SenderBase() {
    super();
  }

  public SenderBase(
      SenderHelper<T> dataSender,
      ReceiverHelper<String> ackReceiver,
      Identifier self, Identifier parent,
      List<ComparableIdentifier> children) {
    super(self, parent, children);
    this.dataSender = dataSender;
    this.ackReceiver = ackReceiver;
  }
}