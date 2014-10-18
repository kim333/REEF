/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.task;

import com.microsoft.reef.io.network.exception.ParentDeadException;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.serialization.Codec;

/**
 * Represents the local topology of tasks for an operator. It
 * provides methods to send/rcv from parents & children
 *
 * Every operator is an EventHandler<GroupCommunicationMessage>
 * and it will use an instance of this type to delegate the
 * handling of the message and also uses it to communicate
 * with its parents and children
 *
 * This is an operator facing interface. The actual topology is
 * maintained in OperatorTopologyStruct. Current strategy is to
 * maintain two versions of the topology and current operations
 * are always delegated to effectiveTopology and the baseTopology
 * is updated while initialization & when user calls updateTopology.
 * So this is only a wrapper around the two versions of topologies
 * and manages when to create/update them based on the messages from
 * the driver.
 */
public interface OperatorTopology {

  void handle(GroupCommunicationMessage msg);

  void sendToParent(byte[] encode, Type reduce) throws ParentDeadException;

  byte[] recvFromParent() throws ParentDeadException;

  void sendToChildren(byte[] data, Type msgType) throws ParentDeadException;

  <T> T recvFromChildren(ReduceFunction<T> redFunc, Codec<T> dataCodec) throws ParentDeadException;

  void initialize() throws ParentDeadException;
}
