/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.utils.wake;

import com.microsoft.wake.EventHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * An EventHandler that blocks until a set number of Events has been received.
 * Once they have been received, the downstream event handler is called with an
 * Iterable of the events spooled.
 *
 * @param <T>
 */
public final class BlockingEventHandler<T> implements EventHandler<T> {

  private final int expectedSize;
  private List<T> events = new ArrayList<>();
  private final EventHandler<Iterable<T>> destination;

  public BlockingEventHandler(final int expectedSize, final EventHandler<Iterable<T>> destination) {
    this.expectedSize = expectedSize;
    this.destination = destination;
  }

  @Override
  public final void onNext(final T event) {
    if (this.isComplete()) {
      throw new IllegalStateException("Received more Events than expected");
    }
    this.events.add(event);
    if (this.isComplete()) {
      this.destination.onNext(events);
      this.reset();
    }
  }

  private boolean isComplete() {
    return this.events.size() >= expectedSize;
  }

  private void reset() {
    this.events = new ArrayList<>();
  }
}
