/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.utils;

import java.util.Iterator;

import com.microsoft.reef.io.network.Message;
import com.microsoft.reef.io.network.nggroup.api.driver.TaskNode;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.tang.annotations.Name;

/**
 *
 */
public class Utils {

  public static final byte[] EmptyByteArr = new byte[0];

  public static GroupCommunicationMessage bldVersionedGCM (final Class<? extends Name<String>> groupName,
                                                           final Class<? extends Name<String>> operName,
                                                           final Type msgType, final String from, final int srcVersion,
                                                           final String to, final int dstVersion, final byte[]... data) {

    return new GroupCommunicationMessage(groupName.getName(), operName.getName(), msgType, from, srcVersion, to,
                                         dstVersion, data);
  }

  public static Class<? extends Name<String>> getClass (final String className) {
    try {
      return (Class<? extends Name<String>>) Class.forName(className);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException("Unable to find class " + className, e);
    }
  }

  public static String simpleName (final Class<?> className) {
    if (className != null) {
      return className.getSimpleName();
    } else {
      return "NULL";
    }
  }

  public static byte[] getData (final GroupCommunicationMessage gcm) {
    return (gcm.getMsgsCount() == 1) ? gcm.getData()[0] : null;
  }

  /**
   * @param msg
   * @return
   */
  public static GroupCommunicationMessage getGCM (final Message<GroupCommunicationMessage> msg) {
    final Iterator<GroupCommunicationMessage> gcmIterator = msg.getData().iterator();
    if (gcmIterator.hasNext()) {
      final GroupCommunicationMessage gcm = gcmIterator.next();
      if (gcmIterator.hasNext()) {
        throw new RuntimeException("Expecting exactly one GCM object inside Message but found more");
      }
      return gcm;
    } else {
      throw new RuntimeException("Expecting exactly one GCM object inside Message but found none");
    }
  }
}
