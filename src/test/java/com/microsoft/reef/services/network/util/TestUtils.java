/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.services.network.util;

import com.google.protobuf.ByteString;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupMessageBody;
import com.microsoft.wake.Identifier;

public class TestUtils {
  public static GroupCommMessage bldGCM(final Type msgType, final Identifier from, final Identifier to, final byte[]... elements) {
    final GroupCommMessage.Builder GCMBuilder = GroupCommMessage.newBuilder();
    GCMBuilder.setType(msgType);
    GCMBuilder.setSrcid(from.toString());
    GCMBuilder.setDestid(to.toString());
    final GroupMessageBody.Builder bodyBuilder = GroupMessageBody.newBuilder();
    for (final byte[] element : elements) {
      bodyBuilder.setData(ByteString.copyFrom(element));
      GCMBuilder.addMsgs(bodyBuilder.build());
    }
    final GroupCommMessage msg = GCMBuilder.build();
    return msg;
  }

  /**
   * @param type
   * @return
   */
  public static boolean controlMessage(final GroupCommMessage.Type type) {
    switch(type){
    /*case SourceAdd:
    case SourceDead:
    case ParentAdd:
    case ParentDead:
    case ChildAdd:
    case ChildDead:
    case ChildRemoved:
    case ParentRemoved:
    case ChildAdded:
    case ParentAdded:
    case TopologyChanges:
    case TopologySetup:
    case TopologyUpdated:
    case UpdateTopology:
      return true;*/

    case AllGather:
    case AllReduce:
    case Broadcast:
    case Gather:
    case Reduce:
    case ReduceScatter:
    case Scatter:
      return false;

      default:
        return true;
    }
  }
}
