/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.broadcast;

import com.microsoft.reef.examples.nggroup.bgd.math.DenseVector;
import com.microsoft.reef.examples.nggroup.bgd.math.Vector;
import com.microsoft.reef.examples.nggroup.bgd.operatornames.ControlMessageBroadcaster;
import com.microsoft.reef.examples.nggroup.bgd.parameters.AllCommunicationGroup;
import com.microsoft.reef.examples.nggroup.bgd.parameters.ModelDimensions;
import com.microsoft.reef.examples.nggroup.broadcast.parameters.ModelBroadcaster;
import com.microsoft.reef.examples.nggroup.broadcast.parameters.ModelReceiveAckReducer;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.task.Task;
import com.microsoft.tang.annotations.Parameter;
import org.mortbay.log.Log;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterTask implements Task {

  public static final String TASK_ID = "MasterTask";

  private static final Logger LOG = Logger.getLogger(MasterTask.class.getName());

  private final CommunicationGroupClient communicationGroupClient;
  private final Broadcast.Sender<ControlMessages> controlMessageBroadcaster;
  private final Broadcast.Sender<Vector> modelBroadcaster;
  private final Reduce.Receiver<Boolean> modelReceiveAckReducer;

  private final int dimensions;

  @Inject
  public MasterTask(
      final GroupCommClient groupCommClient,
      final @Parameter(ModelDimensions.class) int dimensions) {

    this.dimensions = dimensions;

    this.communicationGroupClient = groupCommClient.getCommunicationGroup(AllCommunicationGroup.class);
    this.controlMessageBroadcaster = communicationGroupClient.getBroadcastSender(ControlMessageBroadcaster.class);
    this.modelBroadcaster = communicationGroupClient.getBroadcastSender(ModelBroadcaster.class);
    this.modelReceiveAckReducer = communicationGroupClient.getReduceReceiver(ModelReceiveAckReducer.class);
  }

  @Override
  public byte[] call(final byte[] memento) throws Exception {

    final Vector model = new DenseVector(dimensions);
    final long time1 = System.currentTimeMillis();
    final int numIters = 10;

    for (int i = 0; i < numIters; i++) {

      controlMessageBroadcaster.send(ControlMessages.ReceiveModel);
      modelBroadcaster.send(model);
      modelReceiveAckReducer.reduce();

      final GroupChanges changes = communicationGroupClient.getTopologyChanges();
      if (changes.exist()) {
        Log.info("There exist topology changes. Asking to update Topology");
        communicationGroupClient.updateTopology();
      } else {
        Log.info("No changes in topology exist. So not updating topology");
      }
    }

    final long time2 = System.currentTimeMillis();
    LOG.log(Level.FINE, "Broadcasting vector of dimensions {0} took {1} secs",
        new Object[] { dimensions, (time2 - time1) / (numIters * 1000.0) });

    controlMessageBroadcaster.send(ControlMessages.Stop);

    return null;
  }
}
