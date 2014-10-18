/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.utils;

import java.util.concurrent.CountDownLatch;

public class ResettingCountDownLatch {
  private CountDownLatch latch;

  public ResettingCountDownLatch (final int initialCount) {
    latch = new CountDownLatch(initialCount);
  }

  /**
   *
   */
  public void await () {
    try {
      latch.await();
    } catch (final InterruptedException e) {
      throw new RuntimeException("InterruptedException while waiting for latch", e);
    }
  }

  public void awaitAndReset (final int resetCount) {
    try {
      latch.await();
      latch = new CountDownLatch(resetCount);
    } catch (final InterruptedException e) {
      throw new RuntimeException("InterruptedException while waiting for latch", e);
    }
  }

  /**
   *
   */
  public void countDown () {
    latch.countDown();
  }

}
