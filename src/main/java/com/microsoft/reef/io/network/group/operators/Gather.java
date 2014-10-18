/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.operators;

import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.impl.operators.basic.GatherOp;
import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.wake.Identifier;

import java.util.List;

/**
 * MPI Gather Operator.
 *
 * This is an operator where the root is a receiver and there are multiple senders.
 * The root or receiver gathers all the elements sent by the senders in a List.
 *
 */
public interface Gather {

  /**
   * Senders or non-roots.
   */
  @DefaultImplementation(GatherOp.Sender.class)
  static interface Sender<T> extends GroupCommOperator {

    /**
     * Send the element to the root/receiver.
     */
    void send(T element) throws InterruptedException, NetworkException;
  }

  /**
   * Receiver or Root
   */
  @DefaultImplementation(GatherOp.Receiver.class)
  static interface Receiver<T> extends GroupCommOperator {

    /**
     * Receive the elements sent by the senders in default order.
     *
     * @return elements sent by senders as a List in default order
     */
    List<T> receive() throws InterruptedException, NetworkException;

    /**
     * Receive the elements sent by the senders in specified order
     *
     * @return elements sent by senders as a List in specified order
     */
    List<T> receive(List<? extends Identifier> order) throws InterruptedException, NetworkException;
  }
}
