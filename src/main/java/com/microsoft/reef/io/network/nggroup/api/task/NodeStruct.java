/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.api.task;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;

/**
 * The actual node that is part of the operator topology
 *
 * Receives data from the handlers & provides them to the
 * operators/OperatorTopologyStruct when they need it.
 *
 * This implementation decouples the send & receive.
 */
public interface NodeStruct {

  String getId();

  int getVersion();

  void setVersion(int version);

  byte[] getData();

  void addData(GroupCommunicationMessage msg);
}
