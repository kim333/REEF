/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.utils;

import com.microsoft.wake.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BroadcastingEventHandler<T> implements EventHandler<T> {

  List<EventHandler<T>> handlers = new ArrayList<>();

  public void addHandler (final EventHandler<T> handler) {
    handlers.add(handler);
  }

  @Override
  public void onNext (final T msg) {
    for (final EventHandler<T> handler : handlers) {
      handler.onNext(msg);
    }
  }

}
