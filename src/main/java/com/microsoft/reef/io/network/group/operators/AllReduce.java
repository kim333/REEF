/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.operators;

import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.impl.operators.basic.AllReduceOp;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.wake.Identifier;

import java.util.List;

/**
 * MPI All Reduce Operator. Each task applies this operator on an element of
 * type T. The result will be an element which is result of applying a reduce
 * function on the list of all elements on which this operator has been applied
 */
@DefaultImplementation(AllReduceOp.class)
public interface AllReduce<T> extends GroupCommOperator{

  /**
   * Apply the operation on element.
   *
   * @return result of all-reduce on all elements operation was applied on.
   *         Reduce function is applied based on default order.
   */
  T apply(T aElement) throws InterruptedException, NetworkException;

  /**
   * Apply the operation on element.
   *
   * @return result of all-reduce on all elements operation was applied on.
   *         Reduce function is applied based on specified order.
   */
  T apply(T element, List<? extends Identifier> order) throws InterruptedException, NetworkException;

  /**
   * Get the {@link ReduceFunction} configured.
   *
   * @return {@link ReduceFunction}
   */
  Reduce.ReduceFunction<T> getReduceFunction();
}
