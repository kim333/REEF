/**
 * Copyright (C) 2014 Microsoft Corporation
 */
/**
 * Contains a basic implementation of the interfaces in
 * com.microsoft.reef.io.network.group.operators
 *
 * Also has classes that help in creating {@link com.microsoft.tang.Configuration}
 * for the same implementations in the config package
 *
 * The implementation here are basic in the sense that
 * they treat the tasks to form a single level tree
 * containing the sender or receiver at the root and all
 * other receivers or senders respectively to form leaves
 * of the tree.
 *
 * The Symmetric Operators are implemented as combination
 * of asymmetric operators:
 * AllGather := Gather + Broadcast<List>
 * AllReduce := Reduce + Broadcast<List>
 * ReduceScatter := Reduce + Scatter
 *
 * The state is managed through a hierarchy of objects:
 * SenderReceiverBase (extended by all symmetric operators)
 * |
 * |--SenderBase (extended by senders of asymmetric operators)
 * |
 * |--ReceiverBase (extended by receivers of asymmetric operators)
 */
@java.lang.Deprecated
package com.microsoft.reef.io.network.group.impl.operators.basic;
