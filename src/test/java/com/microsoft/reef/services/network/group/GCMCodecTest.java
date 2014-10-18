/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.services.network.group;

import com.microsoft.reef.io.network.group.impl.GCMCodec;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.reef.io.network.util.StringIdentifierFactory;
import com.microsoft.reef.services.network.util.TestUtils;
import com.microsoft.tang.Tang;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import org.junit.Assert;
import org.junit.Test;

public class GCMCodecTest {

  /**
   * Test method for {@link com.microsoft.reef.io.network.group.impl.GCMCodec#GCMCodec()}.
   *
   * @throws BindException
   * @throws InjectionException
   */
  @Test(timeout = 1000)
  public final void testGCMCodec() throws InjectionException, BindException {
    final GCMCodec codec = Tang.Factory.getTang().newInjector().getInstance(GCMCodec.class);
    Assert.assertNotNull("tang.getInstance(GCMCodec.class)", codec);
  }

  /**
   * Test method for {@link com.microsoft.reef.io.network.group.impl.GCMCodec#decode(byte[])}.
   */
  @Test(timeout = 1000)
  public final void testDecode() {
    final GroupCommMessage expected = TestUtils.bldGCM(Type.Scatter,
        new StringIdentifierFactory().getNewInstance("Task1"),
        new StringIdentifierFactory().getNewInstance("Task2"), "Hello".getBytes());
    final byte[] msgBytes = expected.toByteArray();
    final GCMCodec codec = new GCMCodec();
    final GroupCommMessage decoded = codec.decode(msgBytes);
    Assert.assertEquals("GCMCodec.decode():", expected, decoded);
  }

  /**
   * Test method for {@link com.microsoft.reef.io.network.group.impl.GCMCodec#encode(com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage)}.
   */
  @Test(timeout = 1000)
  public final void testEncode() {
    final GroupCommMessage msg = TestUtils.bldGCM(Type.Scatter,
        new StringIdentifierFactory().getNewInstance("Task1"),
        new StringIdentifierFactory().getNewInstance("Task2"), "Hello".getBytes());
    final byte[] expected = msg.toByteArray();
    final GCMCodec codec = new GCMCodec();
    final byte[] encoded = codec.encode(msg);
    Assert.assertArrayEquals("GCMCodec.encode():", expected, encoded);
  }

}
