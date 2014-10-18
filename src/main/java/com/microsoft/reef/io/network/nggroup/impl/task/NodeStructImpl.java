/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.microsoft.reef.io.network.nggroup.api.task.NodeStruct;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;

public abstract class NodeStructImpl implements NodeStruct {

  private static final Logger LOG = Logger.getLogger(NodeStructImpl.class.getName());

  private final String id;
  private final BlockingQueue<GroupCommunicationMessage> dataQue = new LinkedBlockingQueue<>();

  private int version;

  public NodeStructImpl (final String id, final int version) {
    super();
    this.id = id;
    this.version = version;
  }

  @Override
  public int getVersion () {
    return version;
  }

  @Override
  public void setVersion (final int version) {
    this.version = version;
  }

  @Override
  public String getId () {
    return id;
  }

  @Override
  public void addData (final GroupCommunicationMessage msg) {
    LOG.entering("NodeStructImpl", "addData", msg);
    dataQue.add(msg);
    LOG.exiting("NodeStructImpl", "addData", msg);
  }

  @Override
  public byte[] getData () {
    LOG.entering("NodeStructImpl", "getData");
    GroupCommunicationMessage gcm;
    try {
      gcm = dataQue.take();
    } catch (final InterruptedException e) {
      throw new RuntimeException("InterruptedException while waiting for data from " + id, e);
    }

    final byte[] retVal = checkDead(gcm) ? null : Utils.getData(gcm);
    LOG.exiting("NodeStructImpl", "getData", retVal);
    return retVal;
  }

  @Override
  public String toString () {
    return "(" + id + "," + version + ")";
  }

  @Override
  public boolean equals (final Object obj) {
    if (obj instanceof NodeStructImpl) {
      final NodeStructImpl that = (NodeStructImpl) obj;
      return this.id.equals(that.id) && this.version == that.version;
    } else {
      return false;
    }
  }

  public abstract boolean checkDead (final GroupCommunicationMessage gcm);

}
