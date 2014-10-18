/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.util.logging.Logger;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;

/**
 *
 */
public class ParentNodeStruct extends NodeStructImpl {

  private static final Logger LOG = Logger.getLogger(ParentNodeStruct.class.getName());

  public ParentNodeStruct (final String id, final int version) {
    super(id, version);
  }

  @Override
  public boolean checkDead (final GroupCommunicationMessage gcm) {
    LOG.entering("ParentNodeStruct", "checkDead", gcm);
    final boolean retVal = gcm.getType() == Type.ParentDead ? true : false;
    LOG.exiting("ParentNodeStruct", "checkDead", gcm);
    return retVal;
  }

}
