/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.tang.annotations.Name;

/**
 * Helper class to wrap msg and the operator name in the msg
 */
public class IndexedMsg {
  private final Class<? extends Name<String>> operName;
  private final GroupCommunicationMessage msg;

  public IndexedMsg (final GroupCommunicationMessage msg) {
    super();
    this.operName = Utils.getClass(msg.getOperatorname());
    this.msg = msg;
  }

  public Class<? extends Name<String>> getOperName () {
    return operName;
  }

  public GroupCommunicationMessage getMsg () {
    return msg;
  }

  @Override
  public int hashCode () {
    return operName.getName().hashCode();
  }

  @Override
  public boolean equals (final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof IndexedMsg)) {
      return false;
    }
    final IndexedMsg that = (IndexedMsg) obj;
    if (this.operName == that.operName) {
      return true;
    }
    return false;
  }

  @Override
  public String toString () {
    return operName.getSimpleName();
  }

}
