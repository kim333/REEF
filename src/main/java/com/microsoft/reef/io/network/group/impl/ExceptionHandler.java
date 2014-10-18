/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl;

import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.wake.EventHandler;

import javax.inject.Inject;
import java.util.logging.Logger;

/**
 * ExceptionHandler registered with {@link NetworkService}
 */
public class ExceptionHandler implements EventHandler<Exception> {
  /**
   * Standard Java logger object.
   */
  private static final Logger logger = Logger.getLogger(ExceptionHandler.class.getName());

  @Inject
  public ExceptionHandler() {
    //intentionally blank
  }

  @Override
  public void onNext(Exception e) {
    logger
        .severe("Exception occurred while processing a GroupComm operation caused by "
            + e.getCause());
  }

}
