package edu.snu.cms.reef.tutorial;
/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.microsoft.reef.annotations.audience.TaskSide;
import com.microsoft.reef.io.data.loading.api.DataSet;
import com.microsoft.reef.io.network.util.Utils;
import com.microsoft.reef.io.network.util.Utils.Pair;
import com.microsoft.reef.task.Task;
import com.microsoft.tang.annotations.Parameter;

import edu.snu.cms.reef.tutorial.DataLoadingREEF.LearnRate;
import edu.snu.cms.reef.tutorial.DataLoadingREEF.NumParam;
import edu.snu.cms.reef.tutorial.DataLoadingREEF.TargetParam;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import javax.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The task that iterates over the data set to count the number of records.
 * Assumes TextInputFormat and that records represent lines.
 */
@TaskSide
public class LineCountingTask implements Task {

  private static final Logger LOG = Logger.getLogger(LineCountingTask.class.getName());

  private final DataSet<LongWritable, Text> dataSet;
  private SGD_Linear sgd = new SGD_Linear();
  private double [] hypo;
  private double learnRate;
  private int numParam;
  private int targetParam;
  private double [] attr;
  private String [] attrStr;
  @Inject
  public LineCountingTask(final DataSet<LongWritable, Text> dataSet,
		  				  final @Parameter(LearnRate.class) int learnRate,
		  				  final @Parameter(NumParam.class) int numParam,
		  				  final @Parameter(TargetParam.class) int targetParam) {
    this.dataSet = dataSet;
    this.learnRate = learnRate;
    this.numParam = numParam;
    this.targetParam = targetParam;
    hypo = new double[numParam];
    for(int i = 0; i < numParam; i ++){
    	hypo[i] = 0;
    }
    
    attr =  new double[numParam];
  }

  @Override 
  public byte[] call(final byte[] memento) throws Exception {
    LOG.log(Level.FINER, "LineCounting task started");
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
    LOG.log(Level.FINER, "LineCounting task finished: read {0} lines", numEx);
    return Integer.toString(numEx).getBytes();
  }
}