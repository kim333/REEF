/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.groupcomm.matmul;

import com.microsoft.reef.io.network.group.operators.Reduce;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * A Reduce function that concatenates an iterable of vectors into a single
 * vector
 * 
 * @author shravan
 * 
 */
public class VectorConcat implements Reduce.ReduceFunction<Vector> {

	@Inject
	public VectorConcat() {
	}

	@Override
	public Vector apply(Iterable<Vector> elements) {
		List<Double> resultLst = new ArrayList<>();
		for (Vector element : elements) {
			for (int i = 0; i < element.size(); i++)
				resultLst.add(element.get(i));
		}
		Vector result = new DenseVector(resultLst.size());
		int i = 0;
		for (double elem : resultLst)
			result.set(i++, elem);
		return result;
	}

}