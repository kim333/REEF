/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import com.microsoft.wake.EventHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class ExceptionHandler implements EventHandler<Exception> {
  private static final Logger LOG = Logger.getLogger(ExceptionHandler.class.getName());
  List<Exception> exceptions = new ArrayList<>();

  @Inject
  public ExceptionHandler () {
  }

  @Override
  public synchronized void onNext (final Exception ex) {
    LOG.entering("ExceptionHandler", "onNext", new Object[] { ex });
    exceptions.add(ex);
    LOG.finest("Got an exception. Added it to list(" + exceptions.size() + ")");
    LOG.exiting("ExceptionHandler", "onNext");
  }

  public synchronized boolean hasExceptions () {
    LOG.entering("ExceptionHandler", "hasExceptions");
    final boolean ret = !exceptions.isEmpty();
    LOG.finest("There are " + exceptions.size() + " exceptions. Clearing now");
    exceptions.clear();
    LOG.exiting("ExceptionHandler", "hasExceptions", ret);
    return ret;
  }

}
