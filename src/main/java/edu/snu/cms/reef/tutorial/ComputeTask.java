/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package edu.snu.cms.reef.tutorial;


import com.microsoft.reef.task.Task;
import com.microsoft.reef.io.data.loading.api.DataSet;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Scatter;
import com.microsoft.reef.io.network.util.Utils.Pair;
import com.microsoft.tang.annotations.Parameter;

import javax.inject.Inject;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ComputeTask Receives the partial matrix(row partitioned) it is
 * responsible for. Also receives one column vector per iteration and computes
 * the partial product of this vector with its assigned partial matrix The
 * partial product across all the compute tasks are concatenated by doing a
 * Reduce with Concat as the Reduce Function
 * 
 * @author shravan
 * 
 */

public class ComputeTask implements Task{
  private final Logger logger = Logger.getLogger(ComputeTask.class
      .getName());
	/**
	 * The Group Communication Operators that are needed by this task. These
	 * will be injected into the constructor by TANG. The operators used here
	 * are complementary to the ones used in the ControllerTask
	 */
	Scatter.Receiver<Vector> scatterReceiver;
	Broadcast.Receiver<Vector> broadcastReceiver;
	Reduce.Sender<Vector> reduceSender;
	private final DataSet<LongWritable, Text> dataSet;
	private SGD_Linear sgd = new SGD_Linear();
	private double [] hypo;
	private double learnRate;
	private int numParam;
	private int targetParam;
	private double [] attr;
	private String [] attrStr;
	/**
	 * This class is instantiated by TANG
	 * 
	 * @param scatterReceiver
	 *            The receiver for the scatter operation
	 * @param broadcastReceiver
	 *            The receiver for the broadcast operation
	 * @param reduceSender
	 *            The sender for the reduce operation
	 */
	@Inject
	public ComputeTask(Scatter.Receiver<Vector> scatterReceiver,
                     Broadcast.Receiver<Vector> broadcastReceiver,
                     Reduce.Sender<Vector> reduceSender,
                     final DataSet<LongWritable, Text> dataSet, 
                     final @Parameter(MatMultDriver.Parameters.NumParam.class) int numParam, 
                     final @Parameter(MatMultDriver.Parameters.LearnRate.class) double learnRate, 
                     final @Parameter(MatMultDriver.Parameters.TargetParam.class) int targetParam){
		super();
		this.scatterReceiver = scatterReceiver;
		this.broadcastReceiver = broadcastReceiver;
		this.reduceSender = reduceSender;
		this.dataSet = dataSet;
		this.numParam = numParam;
		this.learnRate = learnRate;
		this.targetParam = targetParam;
	}

	@Override
	public byte[] call(byte[] memento) throws Exception {
	    int numEx = 0;
	    for (final Pair<LongWritable, Text> keyValue : dataSet) {
	      // LOG.log(Level.FINEST, "Read line: {0}", keyValue);
	      ++numEx;
	      System.out.println("Dataset "+ numEx + " = " + keyValue.first.toString() + " , " + keyValue.second.toString());
	      attrStr = keyValue.second.toString().split(",");
	      
	      for(int i=0; i < numParam; i++){
	    	  attr[i] = Double.parseDouble(attrStr[i].trim());
	      }
	      
	      for(int i =0; i<  numParam; i++){
	    	 hypo[i] = sgd.hypothesis(attr, hypo, i, targetParam, learnRate);
	      }
	      
	    }
		Vector result = new DenseVector(numParam);
		for(int i =0; i < numParam; i++)
			result.set(i,hypo[i]);
	    reduceSender.send(result);
		return null;
	}

	private Vector computeAx(List<Vector> partialA, Vector x) {
		Vector result = new DenseVector(partialA.size());
		int i = 0;
		for (Vector row : partialA) {
			result.set(i++, row.dot(x));
		}
		return result;
	}
}
