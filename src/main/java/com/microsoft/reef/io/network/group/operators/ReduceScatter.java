/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.operators;

import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.impl.operators.basic.ReduceScatterOp;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.wake.Identifier;

import java.util.List;

/**
 * MPI Reduce Scatter operator.
 *
 * Each task has a list of elements. Assume that each task reduces
 * each element in the list to form a list of reduced elements at a dummy root.
 * The dummy root then keeps the portion of the list assigned to it and
 * scatters the remaining among the other tasks
 */
@DefaultImplementation(ReduceScatterOp.class)
public interface ReduceScatter<T> extends GroupCommOperator {

  /**
   * Apply this operation on elements where counts specify the distribution of
   * elements to each task. Ordering is assumed to be default.
   *
   * Here counts is of the same size as the entire group not just children.
   *
   * @return List of values that result from applying reduce function on
   *         corresponding elements of each list received as a result of
   *         applying scatter.
   */
  List<T> apply(List<T> elements, List<Integer> counts) throws InterruptedException, NetworkException;

  /**
   * Apply this operation on elements where counts specify the distribution of
   * elements to each task. Ordering is specified using order
   *
   * Here counts is of the same size as the entire group not just children
   *
   * @return List of values that result from applying reduce function on
   *         corresponding elements of each list received as a result of
   *         applying scatter.
   */
  List<T> apply(List<T> elements, List<Integer> counts,
      List<? extends Identifier> order) throws InterruptedException, NetworkException;

  /**
   * get {@link ReduceFunction} configured
   *
   * @return {@link ReduceFunction}
   */
  Reduce.ReduceFunction<T> getReduceFunction();
}
