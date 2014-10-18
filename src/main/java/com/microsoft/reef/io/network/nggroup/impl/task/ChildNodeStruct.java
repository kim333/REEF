/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.util.logging.Logger;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;

public class ChildNodeStruct extends NodeStructImpl {

  private static final Logger LOG = Logger.getLogger(ChildNodeStruct.class.getName());

  public ChildNodeStruct (final String id, final int version) {
    super(id, version);
  }

  @Override
  public boolean checkDead (final GroupCommunicationMessage gcm) {
    LOG.entering("ChildNodeStruct", "checkDead", gcm);
    final boolean retVal = gcm.getType() == Type.ChildDead ? true : false;
    LOG.exiting("ChildNodeStruct", "checkDead", gcm);
    return retVal;
  }

}
