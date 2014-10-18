/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl;

import javax.inject.Inject;

import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.reef.io.serialization.Codec;

public class GroupChangesCodec implements Codec<GroupChanges> {

  @Inject
  public GroupChangesCodec () {
  }

  @Override
  public GroupChanges decode(final byte[] changeBytes) {
    return new GroupChangesImpl((changeBytes[0] == 1) ? true : false);
  }

  @Override
  public byte[] encode(final GroupChanges changes) {
    final byte[] retVal = new byte[1];
    if (changes.exist()) {
      retVal[0] = 1;
    }
    return retVal;
  }

  public static void main(final String[] args) {
    GroupChanges changes = new GroupChangesImpl(false);
    final GroupChangesCodec changesCodec = new GroupChangesCodec();
    GroupChanges changes1 = changesCodec.decode(changesCodec.encode(changes));
    test(changes, changes1);
    changes = new GroupChangesImpl(true);
    changes1 = changesCodec.decode(changesCodec.encode(changes));
    test(changes, changes1);
  }

  private static void test(final GroupChanges changes, final GroupChanges changes1) {
    final boolean c1 = changes.exist();
    final boolean c2 = changes1.exist();

    if (c1 != c2) {
      System.out.println("Something is wrong");
    } else {
      System.out.println("Codec is fine");
    }
  }
}
