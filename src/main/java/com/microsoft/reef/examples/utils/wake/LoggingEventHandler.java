/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.utils.wake;

import com.microsoft.wake.EventHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An EventHandler that logs its events before handing it to a downstream
 * EventHandler.
 *
 * @param <T>
 */
public class LoggingEventHandler<T> implements EventHandler<T> {

  private final EventHandler<T> downstreamEventHandler;
  private final String prefix;
  private final String suffix;

  /**
   * @param prefix                 to be logged before the event
   * @param downstreamEventHandler the event handler to hand the event to
   * @param suffix                 to be logged after the event
   */
  public LoggingEventHandler(final String prefix, EventHandler<T> downstreamEventHandler, final String suffix) {
    this.downstreamEventHandler = downstreamEventHandler;
    this.prefix = prefix;
    this.suffix = suffix;
  }

  public LoggingEventHandler(final EventHandler<T> downstreamEventHandler) {
    this("", downstreamEventHandler, "");
  }

  @Override
  public void onNext(final T value) {
    Logger.getLogger(LoggingEventHandler.class.getName()).log(Level.INFO, prefix + value.toString() + suffix);
    this.downstreamEventHandler.onNext(value);
  }
}
