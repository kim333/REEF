/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.wake.remote.Codec;

import javax.inject.Inject;

/**
 * Codec for {@link GroupCommMessage}
 */
public class GCMCodec implements Codec<GroupCommMessage> {

  @Inject
  public GCMCodec() {
  }

  @Override
  public GroupCommMessage decode(final byte[] data) {
    try {
      return GroupCommMessage.parseFrom(data);
    } catch (final InvalidProtocolBufferException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  @Override
  public byte[] encode(final GroupCommMessage msg) {
    return msg.toByteArray();
  }
}
