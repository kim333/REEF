/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.logging.Logger;

import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.Connection;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.Identifier;
import com.microsoft.wake.IdentifierFactory;

/**
 * Event handler that receives ctrl msgs and
 * dispatched them using network service
 */
public class CtrlMsgSender implements EventHandler<GroupCommunicationMessage> {

  private static final Logger LOG = Logger.getLogger(CtrlMsgSender.class.getName());
  private final IdentifierFactory idFac;
  private final NetworkService<GroupCommunicationMessage> netService;

  public CtrlMsgSender (final IdentifierFactory idFac, final NetworkService<GroupCommunicationMessage> netService) {
    this.idFac = idFac;
    this.netService = netService;
  }

  @Override
  public void onNext (final GroupCommunicationMessage srcCtrlMsg) {
    LOG.entering("CtrlMsgSender", "onNext", srcCtrlMsg);
    final Identifier id = idFac.getNewInstance(srcCtrlMsg.getDestid());
    final Connection<GroupCommunicationMessage> link = netService.newConnection(id);
    try {
      link.open();
      link.write(srcCtrlMsg);
    } catch (final NetworkException e) {
      throw new RuntimeException("Unable to send ctrl task msg to parent " + id, e);
    }
    LOG.exiting("CtrlMsgSender", "onNext", srcCtrlMsg);
  }

}
