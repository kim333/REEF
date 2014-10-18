/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.groupcomm.matmul;

import java.io.Serializable;

/**
 * A dense {@link Vector} implementation backed by a double[]
 * 
 * @author Markus Weimer <mweimer@microsoft.com>
 * 
 */
public class DenseVector extends AbstractVector implements Serializable {

    private static final long serialVersionUID = 1L;
    private final double[] values;

    /**
     * Creates a dense vector of the given size
     * 
     * @param size
     */
    public DenseVector(final int size) {
        this(new double[size]);

    }

    private DenseVector(final double[] values) {
        this.values = values;

    }

    @Override
    public void set(final int i, final double v) {
        this.values[i] = v;

    }

    @Override
    public double get(final int i) {
        return this.values[i];
    }

    @Override
    public int size() {
        return this.values.length;
    }

    /**
     * Creates a random Vector of size 'size' where each element is individually
     * drawn from Math.random()
     * 
     * @param size
     * @return a random Vector of the given size where each element is
     *         individually drawn from Math.random()
     */
    public static Vector rand(final int size) {
        final DenseVector vec = new DenseVector(size);
        for (int i = 0; i < size; ++i) {
            vec.values[i] = Math.random();
        }
        return vec;
    }
}
