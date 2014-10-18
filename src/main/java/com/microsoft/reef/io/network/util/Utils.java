/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.util;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.google.protobuf.ByteString;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupMessageBody;
import com.microsoft.wake.ComparableIdentifier;
import com.microsoft.wake.Identifier;
import com.microsoft.wake.IdentifierFactory;

public class Utils {

  private static final String DELIMITER = "-";

  /**
   * TODO: Merge with parseListCmp() into one generic implementation.
   */
  public static List<Identifier> parseList(
      final String ids, final IdentifierFactory factory) {
    final List<Identifier> result = new ArrayList<>();
    for (final String token : ids.split(DELIMITER)) {
      result.add(factory.getNewInstance(token.trim()));
    }
    return result;
  }

  /**
   * TODO: Merge with parseList() into one generic implementation.
   */
  public static List<ComparableIdentifier> parseListCmp(
      final String ids, final IdentifierFactory factory) {
    final List<ComparableIdentifier> result = new ArrayList<>();
    for (final String token : ids.split(DELIMITER)) {
      result.add((ComparableIdentifier) factory.getNewInstance(token.trim()));
    }
    return result;
  }

  public static String listToString(final List<ComparableIdentifier> ids) {
    return StringUtils.join(ids, DELIMITER);
  }

  public static List<Integer> createUniformCounts(final int elemSize, final int childSize) {
    final int remainder = elemSize % childSize;
    final int quotient = elemSize / childSize;
    final ArrayList<Integer> result = new ArrayList<>(childSize);
    result.addAll(Collections.nCopies(remainder, quotient + 1));
    result.addAll(Collections.nCopies(childSize - remainder, quotient));
    return Collections.unmodifiableList(result);
  }

  private static class AddressComparator implements Comparator<Inet4Address> {
    @Override
    public int compare(final Inet4Address aa, final Inet4Address ba) {
      final byte[] a = aa.getAddress();
      final byte[] b = ba.getAddress();
      // local subnet comes after all else.
      if (a[0] == 127 && b[0] != 127) {
        return 1;
      }
      if (a[0] != 127 && b[0] == 127) {
        return -1;
      }
      for (int i = 0; i < 4; i++) {
        if (a[i] < b[i]) {
          return -1;
        }
        if (a[i] > b[i]) {
          return 1;
        }
      }
      return 0;
    }
  }

  public static GroupCommMessage bldGCM(
      final Type msgType, final Identifier from, final Identifier to, final byte[]... elements) {

    final GroupCommMessage.Builder GCMBuilder = GroupCommMessage.newBuilder()
        .setType(msgType)
        .setSrcid(from.toString())
        .setDestid(to.toString());

    final GroupMessageBody.Builder bodyBuilder = GroupMessageBody.newBuilder();

    for (final byte[] element : elements) {
      bodyBuilder.setData(ByteString.copyFrom(element));
      GCMBuilder.addMsgs(bodyBuilder.build());
    }

    return GCMBuilder.build();
  }
}
