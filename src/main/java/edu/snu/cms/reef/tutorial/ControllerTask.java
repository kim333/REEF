/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package edu.snu.cms.reef.tutorial;
import com.microsoft.reef.task.Task;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Scatter;
import com.microsoft.tang.annotations.Parameter;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ControllerTask Controls the matrix multiplication task Splits up the
 * input matrix into parts(row-wise) and scatters them amongst the compute
 * tasks Broadcasts each column vector Receives the reduced(concatenated)
 * partial products as the output vector
 * 
 * @author shravan
 * 
 */
public class ControllerTask implements Task {

  private final Logger logger = Logger.getLogger(ControllerTask.class.getName());

	/**
	 * The Group Communication Operators that are needed by this task. These
	 * will be injected into the constructor by TANG. The operators used here
	 * are complementary to the ones used in the ComputeTask
	 */
	Scatter.Sender<Vector> scatterSender;
	Broadcast.Sender<Vector> broadcastSender;
	Reduce.Receiver<Vector> reduceReceiver;
	int numParam;
	// The matrices
	List<Vector> X, A;

	// We compute AX'

	/**
	 * This class is instantiated by TANG
	 * 
	 * @param scatterSender
	 *            The sender for the scatter operation
	 * @param broadcastSender
	 *            The sender for the broadcast operation
	 * @param reduceReceiver
	 *            The receiver for the reduce operation
	 */
	@Inject
	public ControllerTask(Scatter.Sender<Vector> scatterSender,
                        Broadcast.Sender<Vector> broadcastSender,
                        Reduce.Receiver<Vector> reduceReceiver,
                        final @Parameter(MatMultDriver.Parameters.NumParam.class) int numParam) {
		super();
		this.scatterSender = scatterSender;
		this.broadcastSender = broadcastSender;
		this.reduceReceiver = reduceReceiver;
		this.numParam = numParam;
	}

	/**
	 * Computes AX'
	 */
	@Override
	public byte[] call(byte[] memento) throws Exception {
		// Scatter the matrix A
	  logger.log(Level.FINE, "Scattering A");
	    int iter = 20;
	    
		Vector result = new DenseVector(numParam);
		Vector Ax;

		// Just use Iterable with a Matrix class
		for (int i=0; i < iter;i++){
			// Broadcast each column
			//broadcastSender.send(x);
			// Receive a concatenated vector of the
			// partial sums computed by each computeTask
			// Accumulate the result
			Ax = reduceReceiver.reduce();
			result.add(Ax);
		}

		String resStr = result.toString();
		return resStr.getBytes();
	}

	
}
