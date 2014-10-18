/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.task;

import java.util.Collection;
import java.util.Set;

import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.operators.Sender;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.tang.annotations.Name;

/**
 * The actual local topology maintaining the
 * children and parent that reacts to update
 * and data msgs. The actual nodes are represented
 * by NodeStruct and it handles receiving &
 * providing data
 */
public interface OperatorTopologyStruct {

  Class<? extends Name<String>> getGroupName();

  Class<? extends Name<String>> getOperName();

  String getSelfId();

  int getVersion();

  NodeStruct getParent();

  Collection<? extends NodeStruct> getChildren();

  String getDriverId();

  Sender getSender();

  boolean hasChanges();

  void setChanges(boolean b);

  void addAsData(GroupCommunicationMessage msg);

  void update(Set<GroupCommunicationMessage> deletionDeltas);

  void update(GroupCommunicationMessage msg);

  void sendToParent(byte[] data, Type msgType);

  byte[] recvFromParent();

  void sendToChildren(byte[] data, Type msgType);

  <T> T recvFromChildren(ReduceFunction<T> redFunc, Codec<T> dataCodec);
}
