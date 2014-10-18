/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.utils;

public class Timer implements AutoCloseable {

  private final String description;
  private final long t1;
  public static final int MINUTES = 60 * 1000;  // ms
  public static final int HOURS = 60 * MINUTES;

  public Timer(final String description) {
    this.description = description;
    System.out.println();
    System.out.println(description + " Starting:");
    t1 = System.currentTimeMillis();
  }

  @Override
  public void close() {
    final long t2 = System.currentTimeMillis();
    System.out.println();
    System.out.println(description + " Ended:");
    System.out.println(description + " took " + (t2 - t1) / 1000.0 + " sec");
    System.out.println();
  }

}