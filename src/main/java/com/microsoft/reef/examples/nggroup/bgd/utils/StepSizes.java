/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.utils;

import javax.inject.Inject;
import java.util.Arrays;

public class StepSizes {

  private final double[] t;
  private final int gridSize = 21;

  @Inject
  public StepSizes() {
    this.t = new double[gridSize];
    final int mid = (gridSize / 2);
    t[mid] = 1;
    for (int i = mid - 1; i >= 0; i--) {
      t[i] = t[i + 1] / 2.0;
    }
    for (int i = mid + 1; i < gridSize; i++) {
      t[i] = t[i - 1] * 2.0;
    }
  }

  public double[] getT() {
    return t;
  }

  public int getGridSize() {
    return gridSize;
  }

  public static void main(final String[] args) {
    // TODO Auto-generated method stub
    final StepSizes t = new StepSizes();
    System.out.println(Arrays.toString(t.getT()));
  }
}
