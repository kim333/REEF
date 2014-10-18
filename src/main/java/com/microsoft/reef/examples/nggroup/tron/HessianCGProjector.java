/*
 * Copyright 2013 Microsoft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.reef.examples.nggroup.tron;

import com.microsoft.reef.examples.nggroup.tron.math.Vector;
import com.microsoft.reef.examples.nggroup.tron.operations.CG.CGDirectionProjector;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ConjugateDirectionBroadcaster;
import com.microsoft.reef.examples.nggroup.tron.operatornames.LossAndGradientReducer;
import com.microsoft.reef.examples.nggroup.tron.operatornames.LossSecondDerivativeCompletionReducer;
import com.microsoft.reef.examples.nggroup.tron.operatornames.ProjectedDirectionReducer;
import com.microsoft.reef.examples.nggroup.tron.utils.Timer;
import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Broadcast.Sender;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Reduce.Receiver;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.util.Pair;

/**
 *
 */
public class HessianCGProjector implements CGDirectionProjector {

  private final Sender<Vector> conjugateDirectionBroadcaster;
  private final Receiver<Pair<Integer, Vector>> projectedDirectionReducer;
  private final Sender<ControlMessages> controlMessageBroadcaster;
  private final Reduce.Receiver<Boolean> lossSecDerCompletionReducer;
  private final Vector model;
  private final Sender<Vector> modelBroadcaster;
  private final double lambda;
  private boolean sendModel = false;
  private final CommunicationGroupClient communicationGroupClient;
  private final boolean ignoreAndContinue;

  public HessianCGProjector (final Vector model, final double lambda, final boolean ignoreAndContinue,
                             final CommunicationGroupClient communicationGroupClient,
                             final Broadcast.Sender<ControlMessages> controlMessageBroadcaster,
                             final Broadcast.Sender<Vector> modelBroadcaster) {
    this.model = model;
    this.lambda = lambda;
    this.ignoreAndContinue = ignoreAndContinue;
    this.communicationGroupClient = communicationGroupClient;
    this.controlMessageBroadcaster = controlMessageBroadcaster;
    this.modelBroadcaster = modelBroadcaster;
    this.conjugateDirectionBroadcaster = communicationGroupClient.getBroadcastSender(ConjugateDirectionBroadcaster.class);
    this.projectedDirectionReducer = communicationGroupClient.getReduceReceiver(ProjectedDirectionReducer.class);
    this.lossSecDerCompletionReducer = communicationGroupClient.getReduceReceiver(LossSecondDerivativeCompletionReducer.class);
  }

  @Override
  public void project (final Vector CGDirection, final Vector projectedCGDirection) throws NetworkException, InterruptedException {
    boolean allDead = false;
    do {
      try(Timer t = new Timer(sendModel ? "Broadcast(Model), " : "" + "Broadcast(CGDirection) + Reduce(LossAndGradient)")) {
        if (sendModel) {
          System.out.println("ComputeLossSecondDerivativeWithModel");
          controlMessageBroadcaster.send(ControlMessages.ComputeLossSecondDerivativeWithModel);
          modelBroadcaster.send(model);
          lossSecDerCompletionReducer.reduce();
        }
        System.out.println("ComputeProjectionDirection");
        controlMessageBroadcaster.send(ControlMessages.ComputeProjectionDirection);
        conjugateDirectionBroadcaster.send(CGDirection);

        final Pair<Integer, Vector> projectedDirection = projectedDirectionReducer.reduce();
        if(projectedDirection!=null) {
          final int numExamples = projectedDirection.first;
          System.out.println("#Examples: " + numExamples);
          projectedCGDirection.scale(0);
          projectedCGDirection.add(projectedDirection.second);
          projectedCGDirection.scale(1.0/numExamples);
          projectedCGDirection.multAdd(lambda, CGDirection);
          allDead = false;
        } else {
          allDead = true;
        }
        sendModel = SynchronizationUtils.chkAndUpdate(this.communicationGroupClient);
      }
    } while (allDead || (!ignoreAndContinue && sendModel));
  }

}
