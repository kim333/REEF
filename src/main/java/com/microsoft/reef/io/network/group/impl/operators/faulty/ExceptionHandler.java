/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.faulty;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.wake.EventHandler;

public class ExceptionHandler implements EventHandler<Exception> {
  private static final Logger logger = Logger.getLogger(ExceptionHandler.class.getName());
  List<Exception> exceptions = new ArrayList<>();
  
  @Inject
  public ExceptionHandler() {  }

  @Override
  public synchronized void onNext(Exception arg0) {
    exceptions.add(arg0);
    logger.fine("Got an exception. Added it to list(" + exceptions.size() + ")");
  }
  
  public synchronized boolean hasExceptions(){
    boolean ret = !exceptions.isEmpty();
    logger.fine("There are " + exceptions.size() + " exceptions. Clearing now");
    exceptions.clear();
    return ret;
  }

}
