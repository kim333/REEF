/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.broadcast;

import com.microsoft.reef.examples.nggroup.bgd.math.Vector;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.ControlMessageBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.parameters.AllCommunicationGroup;
import com.microsoft.reef.examples.nggroup.broadcast.parameters.ModelBroadcaster;
import com.microsoft.reef.examples.nggroup.broadcast.parameters.ModelReceiveAckReducer;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.task.Task;

import javax.inject.Inject;

/**
 *
 */
public class SlaveTask implements Task {
  private final CommunicationGroupClient communicationGroupClient;
  private final Broadcast.Receiver<ControlMessages> controlMessageBroadcaster;
  private final Broadcast.Receiver<Vector> modelBroadcaster;
  private final Reduce.Sender<Boolean> modelReceiveAckReducer;

  @Inject
  public SlaveTask(
      final GroupCommClient groupCommClient) {
    this.communicationGroupClient = groupCommClient.getCommunicationGroup(AllCommunicationGroup.class);
    this.controlMessageBroadcaster = communicationGroupClient.getBroadcastReceiver(ControlMessageBroadcaster.class);
    this.modelBroadcaster = communicationGroupClient.getBroadcastReceiver(ModelBroadcaster.class);
    this.modelReceiveAckReducer = communicationGroupClient.getReduceSender(ModelReceiveAckReducer.class);
  }

  @Override
  public byte[] call(final byte[] memento) throws Exception {
    boolean stop = false;
    while (!stop) {
      final ControlMessages controlMessage = controlMessageBroadcaster.receive();
      switch (controlMessage) {
        case Stop:
          stop = true;
          break;

        case ReceiveModel:
          modelBroadcaster.receive();
          if (Math.random() < 0.1) {
            throw new RuntimeException("Simulated Failure");
          }
          modelReceiveAckReducer.send(true);
          break;

        default:
          break;
      }
    }
    return null;
  }
}
