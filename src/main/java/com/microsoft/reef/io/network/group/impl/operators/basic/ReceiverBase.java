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
 * The base class for Receivers of Asymmetric operators
 * {@link Scatter}, {@link Broadcast}, {@link Gather}, {@link Reduce}
 *
 * @param <T>
 */
public class ReceiverBase<T> extends SenderReceiverBase {
  protected ReceiverHelper<T> dataReceiver;
  protected SenderHelper<String> ackSender;

  public ReceiverBase() {
    super();
  }

  public ReceiverBase(
      ReceiverHelper<T> dataReceiver,
      SenderHelper<String> ackSender,
      Identifier self, Identifier parent,
      List<ComparableIdentifier> children) {
    super(self, parent, children);
    this.dataReceiver = dataReceiver;
    this.ackSender = ackSender;
  }

}