/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.math;

import com.microsoft.wake.remote.Codec;

import javax.inject.Inject;
import java.io.*;

/**
 * Codec for the Vector type Uses Data*Stream
 *
 * @author shravan
 */
public class VectorCodec implements Codec<Vector> {
  /**
   * This class is instantiated by TANG
   */
  @Inject
  public VectorCodec() {
    // Intentionally blank
  }

  @Override
  public Vector decode(byte[] data) {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    Vector result;
    try (DataInputStream dais = new DataInputStream(bais)) {
      int size = dais.readInt();
      result = new DenseVector(size);
      for (int i = 0; i < size; i++)
        result.set(i, dais.readDouble());
    } catch (IOException e) {
      throw new RuntimeException(e.getCause());
    }
    return result;
  }

  @Override
  public byte[] encode(Vector vec) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(vec.size()
        * (Double.SIZE / Byte.SIZE));
    try (DataOutputStream daos = new DataOutputStream(baos)) {
      daos.writeInt(vec.size());
      for (int i = 0; i < vec.size(); i++) {
        daos.writeDouble(vec.get(i));
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getCause());
    }
    return baos.toByteArray();
  }

}
