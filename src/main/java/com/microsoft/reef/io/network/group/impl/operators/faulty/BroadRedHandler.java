/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.faulty;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import com.microsoft.reef.io.network.Message;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.wake.EventHandler;

public class BroadRedHandler implements EventHandler<Message<GroupCommMessage>>{
  
  @NamedParameter(doc = "List of Identifiers on which the handler should listen")
  public static class IDs implements Name<Set<String>> {  }
  
  private static Object ctrlLock = new Object();
  private static AtomicBoolean firstSync = new AtomicBoolean(false);
  private static CountDownLatch srcAddLatch = new CountDownLatch(2);
  
  
  private final BroadcastHandler broadHandler;
  private final ReduceHandler redHandler;
  
  @Inject
  public BroadRedHandler(BroadcastHandler broadHandler, ReduceHandler redHandler) {
    this.broadHandler = broadHandler;
    this.redHandler = redHandler;
  }

  @Override
  public void onNext(Message<GroupCommMessage> msg) {
    GroupCommMessage oneVal = null;
    if(msg.getData().iterator().hasNext())
      oneVal = msg.getData().iterator().next();
    switch (oneVal.getType()) {
    case Reduce:
      redHandler.onNext(msg);
      break;
    case Broadcast:
      broadHandler.onNext(msg);
      break;
    case SourceAdd:
      synchronized (ctrlLock) {
        redHandler.onNext(msg);
        broadHandler.onNext(msg);
        srcAddLatch.countDown();
      }
      break;
    case SourceDead:
      synchronized (ctrlLock) {
        redHandler.onNext(msg);
        broadHandler.onNext(msg); 
      }
      break;
    default:
      break;
    }
  }
  
  public static void waitForSrcAdd(BroadcastOp.Sender<?> brSender, ReduceOp.Receiver<?> redReceiver){
    if(firstSync.compareAndSet(false, true)){
      try {
        srcAddLatch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while waiting for src add", e);
      }
      synchronized (ctrlLock) {
        brSender.sync();
        redReceiver.sync();
      }
    }
  }
  
  public static void waitForSrcAdd(BroadcastOp.Receiver<?> brReceiver, ReduceOp.Sender<?> redSender){
    if(firstSync.compareAndSet(false, true)){
      try {
        srcAddLatch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while waiting for src add", e);
      }
      synchronized (ctrlLock) {
        brReceiver.sync();
        redSender.sync();
      }
    }
  }

  public static void sync(BroadcastOp.Sender<?> brSender, ReduceOp.Receiver<?> redReceiver){
    synchronized(ctrlLock){
      brSender.sync();
      redReceiver.sync();
    }
  }
  
  public static void sync(BroadcastOp.Receiver<?> brReceiver, ReduceOp.Sender<?> redSender){
    synchronized (ctrlLock) {
     brReceiver.sync();
     redSender.sync();
    }
  }
}
