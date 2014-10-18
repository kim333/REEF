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

import java.util.ArrayList;

import javax.inject.Inject;

import com.microsoft.reef.examples.nggroup.tron.math.DenseVector;
import com.microsoft.reef.examples.nggroup.tron.math.Vector;
import com.microsoft.reef.examples.nggroup.tron.operations.CG.TruncatedConjugateGradient;
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
import com.microsoft.reef.examples.nggroup.tron.parameters.EnableRampup;
import com.microsoft.reef.examples.nggroup.tron.parameters.Iterations;
import com.microsoft.reef.examples.nggroup.tron.parameters.Lambda;
import com.microsoft.reef.examples.nggroup.tron.parameters.ModelDimensions;
import com.microsoft.reef.examples.nggroup.tron.utils.StepSizes;
import com.microsoft.reef.examples.nggroup.tron.utils.Timer;
import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.Tuple;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Reduce.Receiver;
import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.io.network.util.Pair;
import com.microsoft.reef.io.serialization.Codec;
import com.microsoft.reef.io.serialization.SerializableCodec;
import com.microsoft.reef.task.Task;
import com.microsoft.tang.annotations.Parameter;

/**
 *
 */
public class MasterTask implements Task {
  public static final String TASK_ID = "MasterTask";

  private static final int maxIterations = 200;

  private static final double tolerance = 0.1;

  private final CommunicationGroupClient communicationGroupClient;
  private final Broadcast.Sender<ControlMessages> controlMessageBroadcaster;
  private final Broadcast.Sender<Vector> modelBroadcaster;
  private final Broadcast.Sender<Vector> conjugateDirectionBroadcaster;
  private final Reduce.Receiver<Pair<Pair<Double, Integer>, Vector>> lossAndGradientReducer;
  private final Reduce.Receiver<Pair<Integer, Vector>> projectedDirectionReducer;
  private final Broadcast.Sender<Pair<Vector, Vector>> modelAndDescentDirectionBroadcaster;
  private final Broadcast.Sender<Vector> descentDriectionBroadcaster;
  private final Reduce.Receiver<Pair<Vector, Integer>> lineSearchEvaluationsReducer;
  private final Broadcast.Sender<Double> minEtaBroadcaster;
  private final Reduce.Receiver<Boolean> lossSecDerCompletionReducer;
  private final boolean ignoreAndContinue;
  private final com.microsoft.reef.examples.nggroup.tron.utils.StepSizes ts;
  private final double lambda;
  private final int maxIters;
  final ArrayList<Double> losses = new ArrayList<>();
  final Codec<ArrayList<Double>> lossCodec = new SerializableCodec<ArrayList<Double>>();
  private final Vector model;

  boolean sendModel = true;
  double minEta = 0;

  private double truncationBound;

  double[] Eta = new double[] { 0.0001, 0.25, 0.75 };
  double[] Sigma = new double[] { 0.25, 0.5, 4.0 };


  @Inject
  public MasterTask(
      final GroupCommClient groupCommClient,
      @Parameter(ModelDimensions.class) final int dimensions,
      @Parameter(Lambda.class) final double lambda,
      @Parameter(Iterations.class) final int maxIters,
      @Parameter(EnableRampup.class) final boolean rampup,
      final StepSizes ts) {
    this.lambda = lambda;
    this.maxIters = maxIters;
    this.ts = ts;
    this.ignoreAndContinue = rampup;
    this.model = new DenseVector(dimensions);
    this.communicationGroupClient = groupCommClient.getCommunicationGroup(AllCommunicationGroup.class);
    this.controlMessageBroadcaster = communicationGroupClient.getBroadcastSender(ControlMessageBroadcaster.class);
    this.modelBroadcaster = communicationGroupClient.getBroadcastSender(ModelBroadcaster.class);
    this.conjugateDirectionBroadcaster = communicationGroupClient.getBroadcastSender(ConjugateDirectionBroadcaster.class);
    this.lossAndGradientReducer = communicationGroupClient.getReduceReceiver(LossAndGradientReducer.class);
    this.projectedDirectionReducer = communicationGroupClient.getReduceReceiver(ProjectedDirectionReducer.class);
    this.modelAndDescentDirectionBroadcaster = communicationGroupClient.getBroadcastSender(ModelAndDescentDirectionBroadcaster.class);
    this.descentDriectionBroadcaster = communicationGroupClient.getBroadcastSender(DescentDirectionBroadcaster.class);
    this.lineSearchEvaluationsReducer = communicationGroupClient.getReduceReceiver(LineSearchEvaluationsReducer.class);
    this.minEtaBroadcaster = communicationGroupClient.getBroadcastSender(MinEtaBroadcaster.class);
    this.lossSecDerCompletionReducer = communicationGroupClient.getReduceReceiver(LossSecondDerivativeCompletionReducer.class);
  }

  @Override
  public byte[] call(final byte[] memento) throws Exception {
    Vector descentDirection = null;
    Vector gradient = null;
    double predictedReductionInObjcFunc = 0;
    double actualReductionInObjFunc = 0;
    double currentObjFunc = 0;
    double desDirDotGrad = 0;
    double relGradientNorm = Double.MAX_VALUE;
    double initGradNorm = 0;
    for (int iteration = 1; !converged(iteration, relGradientNorm); ++iteration) {
      try(Timer t = new Timer("Current Iteration(" + (iteration) + ")")) {
        //loss = obj func value
        final Pair<Double,Vector> lossAndGradient = computeLossAndGradient();
        final double newObjFunc = lossAndGradient.first;
        losses.add(newObjFunc);

        if(iteration!=1) {
          actualReductionInObjFunc = currentObjFunc - newObjFunc;

          final boolean endTron = updateTrustRegionBoundary(descentDirection, actualReductionInObjFunc,
                                    predictedReductionInObjcFunc, desDirDotGrad);
          if(endTron) {
            System.out.println("Stopping criteria too rigid...ending TRON");
            break;
          }
          if(actualReductionInObjFunc > Eta[0] * predictedReductionInObjcFunc) {
            gradient = lossAndGradient.second;
            currentObjFunc = newObjFunc;
            computeLossSecondDerivative();
          }
          else {
            descentDirection.scale(-1);
            updateModel(descentDirection);
          }
        }
        else {
          gradient = lossAndGradient.second;
          currentObjFunc = newObjFunc;
          truncationBound = initGradNorm = gradient.norm2();
          computeLossSecondDerivative();
        }
        relGradientNorm = gradient.norm2() / initGradNorm;
        System.out.println("Grad Norm: " + gradient.norm2() + " Rel Gradient Norm: " + relGradientNorm);
        final Pair<Vector,Vector> descentDirectionResidual = getDescentDirection(gradient);
        descentDirection = descentDirectionResidual.first;
        final Vector residual = descentDirectionResidual.second;
        desDirDotGrad = descentDirection.dot(gradient);
        predictedReductionInObjcFunc = 0.5 * (descentDirection.dot(residual) - desDirDotGrad);
        updateModel(descentDirection);
      }
    }
    System.out.println("Stop");
    controlMessageBroadcaster.send(ControlMessages.Stop);

    for (final Double loss : losses) {
      System.out.println(loss);
    }
    return lossCodec.encode(losses);
  }

  /**
   * @throws InterruptedException
   * @throws NetworkException
   *
   */
  private void computeLossSecondDerivative () throws NetworkException, InterruptedException {
    do {
      try(Timer t = new Timer("ComputeLossSecondDerivative")) {
        System.out.println("ComputeLossSecondDerivative");
        if(sendModel) {
          controlMessageBroadcaster.send(ControlMessages.ComputeLossSecondDerivativeWithModel);
          modelBroadcaster.send(this.model);
        }
        else {
          controlMessageBroadcaster.send(ControlMessages.ComputeLossSecondDerivative);
        }
        lossSecDerCompletionReducer.reduce();
      }
      sendModel = SynchronizationUtils.chkAndUpdate(communicationGroupClient);
    } while (!ignoreAndContinue && sendModel);
  }

  /**
   * @param actualReductionInObjFunc
   * @param predictedReductionInObjcFunc
   * @param desDirDotGrad
   *
   */
  private boolean updateTrustRegionBoundary (final Vector ConjugateGradientStep, final double actualReductionInObjFunc, final double predictedReductionInObjcFunc, final double desDirDotGrad) {
    double Alpha;
    final double ConjugateGradientStepNorm = ConjugateGradientStep.norm2();
    if (-actualReductionInObjFunc - desDirDotGrad <= 0)
    {
        //Debug.WriteLine("Warning: Stopping criteria too strict getting in to numerical issue");
        Alpha = Sigma[2];
        return true;
    }
    else
    {
        Alpha = Math.max(Sigma[0], -0.5 * (desDirDotGrad / (-actualReductionInObjFunc - desDirDotGrad)));
    }


    // Update the trust region bound according to the ratio of actual to predicted reduction.
    if (actualReductionInObjFunc < Eta[0] * predictedReductionInObjcFunc)
    {
        truncationBound = Math.min(Math.max(Alpha, Sigma[0]) * ConjugateGradientStepNorm, Sigma[1] * truncationBound);
    }
    else if (actualReductionInObjFunc < Eta[1] * predictedReductionInObjcFunc)
    {
        truncationBound = Math.max(Sigma[0] * truncationBound, Math.min(Alpha * ConjugateGradientStepNorm, Sigma[1] * truncationBound));
    }
    else if (actualReductionInObjFunc < Eta[2] * predictedReductionInObjcFunc)
    {
        truncationBound = Math.max(Sigma[0] * truncationBound, Math.min(Alpha * ConjugateGradientStepNorm, Sigma[2] * truncationBound));
    }
    else
    {
        truncationBound = Math.max(truncationBound, Math.min(Alpha * ConjugateGradientStepNorm, Sigma[2] * truncationBound));
    }

    System.err.println("trust region boundary set to " + truncationBound);
    return false;

  }

  private void updateModel(final Vector descentDirection) throws NetworkException, InterruptedException {
    try(Timer t = new Timer("UpdateModel")) {
      model.add(descentDirection);
    }

    System.out.println("New Model: " + model);
  }

  /**
   * @param minEta
   * @param model
   * @param sendModel
   * @param lossAndGradient
   * @return
   * @throws InterruptedException
   * @throws NetworkException
   */
  private Pair<Double,Vector> computeLossAndGradient() throws NetworkException, InterruptedException {
    Pair<Double,Vector> returnValue = null;
    boolean allDead = false;
    do {
      try(Timer t = new Timer("Broadcast(Model) + Reduce(LossAndGradient)")) {
        System.out.println("ComputeGradientWithModel");
        controlMessageBroadcaster.send(ControlMessages.ComputeGradientWithModel);
        modelBroadcaster.send(model);

        final Pair<Pair<Double, Integer>, Vector> lossAndGradient = lossAndGradientReducer.reduce();

        if (lossAndGradient!=null) {
          final int numExamples = lossAndGradient.first.second;
          System.out.println("#Examples: " + numExamples);
          final double lossPerExample = lossAndGradient.first.first / numExamples;
          System.out.println("Loss: " + lossPerExample);
          final double objFunc = ((lambda / 2) * model.norm2Sqr()) + lossPerExample;
          System.out.println("Objective Func Value: " + objFunc);
          final Vector gradient = lossAndGradient.second;
          gradient.scale(1.0 / numExamples);
          gradient.multAdd(this.lambda, this.model);
          System.out.println("Gradient: " + gradient);
          returnValue = new Pair<>(objFunc, gradient);
          allDead = false;
        } else {
          allDead = true;
        }
      }
      sendModel = SynchronizationUtils.chkAndUpdate(communicationGroupClient);
    } while (allDead || (!ignoreAndContinue && sendModel));
    return returnValue;
  }

  /**
   * @param loss
   * @return
   */
  private boolean converged(final int iters, final double gradNorm) {
    return iters >= maxIters || Math.abs(gradNorm) <= 1e-3;
  }

  /**
   * @param gradient
   * @return
   * @throws InterruptedException
   * @throws NetworkException
   */
  private Pair<Vector,Vector> getDescentDirection(final Vector gradient) throws NetworkException, InterruptedException {
    final HessianCGProjector hessianCGProjector = new HessianCGProjector(
                                                                         this.model,
                                                                         this.lambda,
                                                                         this.ignoreAndContinue,
                                                                         this.communicationGroupClient,
                                                                         this.controlMessageBroadcaster,
                                                                         this.modelBroadcaster);

    final Pair<Vector,Vector> modelStepResidual = TruncatedConjugateGradient.compute(hessianCGProjector, gradient,
                                       truncationBound, tolerance, maxIterations);
    modelStepResidual.first.scale(-1);
    modelStepResidual.second.scale(-1);
    return modelStepResidual;
  }

}
