/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.List;
import java.util.logging.Logger;

import com.microsoft.reef.io.network.nggroup.api.driver.TaskNode;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage.Type;
import com.microsoft.tang.annotations.Name;
import com.microsoft.wake.EStage;
import com.microsoft.wake.EventHandler;

/**
 *
 */
public class TopologyUpdateWaitHandler implements EventHandler<List<TaskNode>> {

  private static final Logger LOG = Logger.getLogger(TopologyUpdateWaitHandler.class.getName());
  private final EStage<GroupCommunicationMessage> senderStage;
  private final Class<? extends Name<String>> groupName;
  private final Class<? extends Name<String>> operName;
  private final String driverId;
  private final int driverVersion;
  private final String dstId;
  private final int dstVersion;
  private final String qualifiedName;


  /**
   * The handler will wait for all nodes to acquire topoLock
   * and send TopologySetup msg. Then it will send TopologyUpdated
   * msg. However, any local topology changes are not in effect
   * till driver sends TopologySetup once statusMap is emptied
   * The operations in the tasks that have topology changes will
   * wait for this. However other tasks that do not have any changes
   * will continue their regular operation
   */
  public TopologyUpdateWaitHandler (final EStage<GroupCommunicationMessage> senderStage,
                                    final Class<? extends Name<String>> groupName, final Class<? extends Name<String>> operName,
                                    final String driverId, final int driverVersion, final String dstId, final int dstVersion,
                                    final String qualifiedName) {
    super();
    this.senderStage = senderStage;
    this.groupName = groupName;
    this.operName = operName;
    this.driverId = driverId;
    this.driverVersion = driverVersion;
    this.dstId = dstId;
    this.dstVersion = dstVersion;
    this.qualifiedName = qualifiedName;
  }



  @Override
  public void onNext (final List<TaskNode> nodes) {
    LOG.entering("TopologyUpdateWaitHandler", "onNext", new Object[] { qualifiedName, nodes });

    for (final TaskNode node : nodes) {
      LOG.fine(qualifiedName + "Waiting for " + node + " to enter TopologyUdate phase");
      node.waitForTopologySetupOrFailure();
      if(node.isRunning()) {
        LOG.fine(qualifiedName + node + " is in TopologyUpdate phase");
      } else {
        LOG.fine(qualifiedName + node + " has failed");
      }
    }
    LOG.finest(qualifiedName + "NodeTopologyUpdateWaitStage All to be updated nodes " + "have received TopologySetup");
    LOG.fine(qualifiedName + "All affected parts of the topology are in TopologyUpdate phase. Will send a note to ("
             + dstId + "," + dstVersion + ")");
    senderStage.onNext(Utils.bldVersionedGCM(groupName, operName, Type.TopologyUpdated, driverId, driverVersion, dstId,
                                             dstVersion, Utils.EmptyByteArr));
    LOG.exiting("TopologyUpdateWaitHandler", "onNext", qualifiedName);
  }

}
