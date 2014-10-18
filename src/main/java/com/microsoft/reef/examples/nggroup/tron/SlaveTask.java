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
package com.microsoft.reef.examples.nggroup.tron;

import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.examples.nggroup.tron.data.Example;
import com.microsoft.reef.examples.nggroup.tron.data.parser.Parser;
import com.microsoft.reef.examples.nggroup.tron.loss.LossFunction;
import com.microsoft.reef.examples.nggroup.tron.math.DenseVector;
import com.microsoft.reef.examples.nggroup.tron.math.Vector;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ConjugateDirectionBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ControlMessageBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.DescentDirectionBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.LineSearchEvaluationsReducer;
import com.microsoft.reef.examples.nggroup.tron.operatornames.LossAndGradientReducer;
import com.microsoft.reef.examples.nggroup.tron.operatornames.LossSecondDerivativeCompletionReducer;
import com.microsoft.reef.examples.nggroup.tron.operatornames.MinEtaBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ModelAndDescentDirectionBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ModelBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ProjectedDirectionReducer;
import com.microsoft.reef.examples.nggroup.tron.parameters.AllCommunicationGroup;
import com.microsoft.reef.examples.nggroup.tron.parameters.ProbabilityOfFailure;
import com.microsoft.reef.examples.nggroup.tron.utils.StepSizes;
import com.microsoft.reef.examples.nggroup.tron.data.DataMatrix;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Reduce.Receiver;
import com.microsoft.reef.io.network.group.operators.Reduce.Sender;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.io.network.util.Pair;
import com.microsoft.reef.task.Task;
import com.microsoft.tang.annotations.Parameter;

public class SlaveTask implements Task {

  private static final Logger LOG = Logger.getLogger(SlaveTask.class.getName());

  private final double FAILURE_PROB;

  private final CommunicationGroupClient communicationGroup;
  private final Broadcast.Receiver<ControlMessages> controlMessageBroadcaster;
  private final Broadcast.Receiver<Vector> modelBroadcaster;
  private final Reduce.Sender<Pair<Pair<Double, Integer>, Vector>> lossAndGradientReducer;
  private final Broadcast.Receiver<Pair<Vector, Vector>> modelAndDescentDirectionBroadcaster;
  private final Broadcast.Receiver<Vector> descentDirectionBroadcaster;
  private final Reduce.Sender<Pair<Vector, Integer>> lineSearchEvaluationsReducer;
  private final Broadcast.Receiver<Double> minEtaBroadcaster;
  private final Reduce.Sender<Boolean> lossSecDerCompletionReducer;
  private final Broadcast.Receiver<Vector> conjugateDirectionBroadcaster;
  private final Reduce.Sender<Pair<Integer, Vector>> projectedDirectionReducer;
  private final DataMatrix dataMatrix;
  private final LossFunction lossFunction;
  private final StepSizes ts;

  private Vector model = null;
  private final Vector descentDirection = null;

  private Vector dataTimesModel = null;
  private Vector lossFirstDerivative = null;
  private Vector lossSecondDerivative = null;

  @Inject
  public SlaveTask(
      final GroupCommClient groupCommClient,
      final DataMatrix dataMatrix,
      final LossFunction lossFunction,
      @Parameter(ProbabilityOfFailure.class) final double pFailure,
      final StepSizes ts) {

    this.dataMatrix = dataMatrix;
    this.lossFunction = lossFunction;
    this.FAILURE_PROB = pFailure;
    LOG.info("Using pFailure=" + this.FAILURE_PROB);
    this.ts = ts;

    this.communicationGroup = groupCommClient.getCommunicationGroup(AllCommunicationGroup.class);
    this.controlMessageBroadcaster = communicationGroup.getBroadcastReceiver(ControlMessageBroadcaster.class);
    this.modelBroadcaster = communicationGroup.getBroadcastReceiver(ModelBroadcaster.class);
    this.lossAndGradientReducer = communicationGroup.getReduceSender(LossAndGradientReducer.class);
    this.modelAndDescentDirectionBroadcaster = communicationGroup.getBroadcastReceiver(ModelAndDescentDirectionBroadcaster.class);
    this.descentDirectionBroadcaster = communicationGroup.getBroadcastReceiver(DescentDirectionBroadcaster.class);
    this.lineSearchEvaluationsReducer = communicationGroup.getReduceSender(LineSearchEvaluationsReducer.class);
    this.minEtaBroadcaster = communicationGroup.getBroadcastReceiver(MinEtaBroadcaster.class);
    this.lossSecDerCompletionReducer = communicationGroup.getReduceSender(LossSecondDerivativeCompletionReducer.class);
    this.conjugateDirectionBroadcaster = communicationGroup.getBroadcastReceiver(ConjugateDirectionBroadcaster.class);
    this.projectedDirectionReducer = communicationGroup.getReduceSender(ProjectedDirectionReducer.class);
  }

  @Override
  public byte[] call(final byte[] memento) throws Exception {
    for (boolean repeat = true; repeat; ) {

      final ControlMessages controlMessage = controlMessageBroadcaster.receive();
      switch (controlMessage) {

        case Stop:
          repeat = false;
          break;

        case ComputeGradientWithModel:
          failPerhaps();
          this.model = modelBroadcaster.receive();
          if(dataTimesModel==null) {
            dataTimesModel = new DenseVector(dataMatrix.getNumberOfExamples());
            lossFirstDerivative = new DenseVector(dataMatrix.getNumberOfExamples());
            lossSecondDerivative = new DenseVector(dataMatrix.getNumberOfExamples());
          }
          lossAndGradientReducer.send(computeLossAndGradient());
          break;

        case ComputeLossSecondDerivative:
          failPerhaps();
          computeLossSecondDerivative();
          lossSecDerCompletionReducer.send(true);
          break;

        case ComputeLossSecondDerivativeWithModel:
          failPerhaps();
          this.model = modelBroadcaster.receive();
          dataMatrix.times(model, dataTimesModel);
          computeLossSecondDerivative();
          lossSecDerCompletionReducer.send(true);
          break;

        case ComputeProjectionDirection:
          failPerhaps();
          final Vector ConjugateDirection = conjugateDirectionBroadcaster.receive();
          projectedDirectionReducer.send(computeProjectionDirection(ConjugateDirection));
          break;

        default:
          break;
      }
    }

    return null;
  }

  /**
   * @return
   */
  private Pair<Integer, Vector> computeProjectionDirection (final Vector conjugateDirection) {
    final Vector tempVec = new DenseVector(dataMatrix.getNumberOfExamples());
    dataMatrix.times(conjugateDirection, tempVec);
    for(int i=0; i<dataMatrix.getNumberOfExamples();i++) {
      tempVec.set(i, tempVec.get(i) * lossSecondDerivative.get(i));
    }
    final Vector result = new DenseVector(this.model.size());
    dataMatrix.transposeTimes(tempVec, result);
    return new Pair<>(dataMatrix.getNumberOfExamples(), result);
  }

  /**
   *
   */
  private void computeLossSecondDerivative () {
    int i=0;
    for(final Example example : dataMatrix) {
      final double lossSecDerPerExample = this.lossFunction.computeSecondGradient(example.getLabel(), dataTimesModel.get(i));
      lossSecondDerivative.set(i, lossSecDerPerExample);
      i++;
    }
  }

  private void failPerhaps() {
    if (Math.random() < FAILURE_PROB) {
      throw new RuntimeException("Simulated Failure");
    }
  }

  private Pair<Pair<Double, Integer>, Vector> computeLossAndGradient() {
    dataMatrix.times(model, dataTimesModel);
    int i=0;
    double loss = 0;
    for(final Example example : dataMatrix) {
      final double lossPerExample = this.lossFunction.computeLoss(example.getLabel(), dataTimesModel.get(i));
      final double lossDerPerExample = this.lossFunction.computeGradient(example.getLabel(), dataTimesModel.get(i));
      lossFirstDerivative.set(i, lossDerPerExample);
      loss += lossPerExample;
      i++;
    }
    final Vector gradient = new DenseVector(model.size());
    dataMatrix.transposeTimes(lossFirstDerivative, gradient);
    return new Pair<>(new Pair<>(loss, dataMatrix.getNumberOfExamples()), gradient);
  }
}
