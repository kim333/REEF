/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.services.network.nggroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessageCodec;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.exceptions.InjectionException;

/**
 *
 */
public class GroupCommunicationMessageCodecTest {

  @NamedParameter
  class GroupName implements Name<String> { }

  @NamedParameter
  class OperName implements Name<String> { }

  @Test(timeout=100)
  public final void testInstantiation() throws InjectionException {
    final GroupCommunicationMessageCodec codec = Tang.Factory.getTang().newInjector().getInstance(GroupCommunicationMessageCodec.class);
    Assert.assertNotNull("tang.getInstance(GroupCommunicationMessageCodec.class): ", codec);
  }

  @Test(timeout=100)
  public final void testEncodeDecode() {
    final Random r = new Random();
    final byte[] data = new byte[100];
    r.nextBytes(data);
    final GroupCommunicationMessage expMsg = Utils.bldVersionedGCM(GroupName.class, OperName.class, Type.ChildAdd, "From", 0, "To", 1, data);
    final GroupCommunicationMessageCodec codec = new GroupCommunicationMessageCodec();
    final GroupCommunicationMessage actMsg1 = codec.decode(codec.encode(expMsg));
    Assert.assertEquals("decode(encode(msg)): ", expMsg, actMsg1);
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream daos = new DataOutputStream(baos);
    codec.encodeToStream(expMsg, daos);
    final GroupCommunicationMessage actMsg2 = codec.decodeFromStream(new DataInputStream(new ByteArrayInputStream(baos.toByteArray())));
    Assert.assertEquals("decodeFromStream(encodeToStream(msg)): ", expMsg, actMsg2);
  }
}
