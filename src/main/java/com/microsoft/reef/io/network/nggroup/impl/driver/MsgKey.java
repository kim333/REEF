/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;

/**
 * The key object used in map to aggregate msgs from
 * all the operators before updating state on driver
 */
public class MsgKey {
  private final String src;
  private final String dst;
  private final Type msgType;

  public MsgKey (final String src, final String dst, final Type msgType) {
    this.src = src;
    this.dst = dst;
    this.msgType = msgType;
  }

  public MsgKey (final GroupCommunicationMessage msg) {
    this.src = msg.getSrcid() + ":" + msg.getSrcVersion();
    this.dst = msg.getDestid() + ":" + msg.getVersion();
    this.msgType = msg.getType();
  }

  public String getSrc () {
    return src.split(":",2)[0];
  }

  public String getDst () {
    return dst.split(":",2)[0];
  }

  public Type getMsgType () {
    return msgType;
  }

  @Override
  public String toString () {
    return "(" + src + "," + dst + "," + msgType + ")";
  }

  @Override
  public boolean equals (final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MsgKey)) {
      return false;
    }
    final MsgKey that = (MsgKey) obj;
    if (!this.src.equals(that.src)) {
      return false;
    }
    if (!this.dst.equals(that.dst)) {
      return false;
    }
    if (!this.msgType.equals(that.msgType)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode () {
    int result = src.hashCode();
    result = 31 * result + dst.hashCode();
    result = 31 * result + msgType.hashCode();
    return result;
  }
}
