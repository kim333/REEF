/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.groupcomm.matmul;

/**
 * An interface for Linear Alebra Vectors.
 * 
 * @author Markus Weimer <mweimer@microsoft.com>
 * 
 */
public interface Vector {

    /**
     * Set dimension i of the Vector to value v
     * 
     * @param i
     *            the index
     * @param v
     *            value
     */
    public void set(int i, double v);

    /**
     * Access the value of the Vector at dimension i
     * 
     * @param i
     *            index
     * @return the value at index i
     */
    public double get(int i);

    /**
     * The size (dimensionality) of the Vector
     * 
     * @return the size of the Vector.
     */
    public int size();

    /**
     * Computes the inner product with another Vector.
     * 
     * @param that
     * @return the inner product between two Vectors.
     */
    public double dot(Vector that);

    /**
     * Adds the Vector that to this one in place: this += that.
     * 
     * @param that
     */
    public void add(final Vector that);

    /**
     * this += factor * that.
     * 
     * @param factor
     * @param that
     */
    public void multAdd(double factor, Vector that);

    /**
     * Scales this Vector: this *= factor.
     * 
     * @param factor
     *            the scaling factor.
     */
    public void scale(double factor);

    /**
     * Computes the sum of all entries in the Vector.
     * 
     * @return the sum of all entries in this Vector
     */
    public double sum();

    /**
     * Computes the L2 norm of this Vector.
     * 
     * @return the L2 norm of this Vector.
     */
    public double norm2();

    /**
     * Normalizes the Vector according to the L2 norm.
     */
    public void normalize();

}