/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.basic;

import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.impl.GroupCommNetworkHandler;
import com.microsoft.reef.io.network.group.impl.operators.basic.config.GroupParameters;
import com.microsoft.reef.io.network.group.operators.AllReduce;
import com.microsoft.reef.io.network.group.operators.Broadcast;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.reef.io.network.util.Utils;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.wake.ComparableIdentifier;
import com.microsoft.wake.Identifier;
import com.microsoft.wake.IdentifierFactory;
import com.microsoft.wake.remote.Codec;

import javax.inject.Inject;
import java.util.List;



/**
 * Implementation of {@link AllReduce}
 * @param <T>
 */
public class AllReduceOp<T> extends SenderReceiverBase implements AllReduce<T> {

	Reduce.Sender<T> reduceSender;
	Reduce.Receiver<T> reduceReceiver;
	Broadcast.Sender<T> broadcastSender;
	Broadcast.Receiver<T> broadcastReceiver;

	@Inject
	public AllReduceOp(
			NetworkService<GroupCommMessage> netService,
			GroupCommNetworkHandler multiHandler,
			@Parameter(GroupParameters.AllReduce.DataCodec.class) Codec<T> codec,
			@Parameter(GroupParameters.AllReduce.SelfId.class) String self,
			@Parameter(GroupParameters.AllReduce.ParentId.class) String parent, 
			@Parameter(GroupParameters.AllReduce.ChildIds.class) String children,
			@Parameter(GroupParameters.IDFactory.class) IdentifierFactory idFac,
			@Parameter(GroupParameters.AllReduce.ReduceFunction.class) ReduceFunction<T> redFunc){
		this(
				new ReduceOp.Sender<>(netService, multiHandler, codec, self, parent, children, idFac, redFunc),
				new ReduceOp.Receiver<>(netService, multiHandler, codec, self, parent, children, idFac, redFunc),
				new BroadcastOp.Sender<>(netService, multiHandler, codec, self, parent, children, idFac),
				new BroadcastOp.Receiver<>(netService, multiHandler, codec, self, parent, children, idFac),
				idFac.getNewInstance(self),
				(parent.equals(GroupParameters.defaultValue)) ? null : idFac.getNewInstance(parent),
				(children.equals(GroupParameters.defaultValue)) ? null : Utils.parseListCmp(children, idFac)
			);
	}
	
	public AllReduceOp(Reduce.Sender<T> reduceSender,
			Reduce.Receiver<T> reduceReceiver,
			Broadcast.Sender<T> broadcastSender,
			Broadcast.Receiver<T> broadcastReceiver, Identifier self,
			Identifier parent, List<ComparableIdentifier> children) {
		super(self,parent,children);
		this.reduceReceiver = reduceReceiver;
		this.reduceSender = reduceSender;
		this.broadcastSender = broadcastSender;
		this.broadcastReceiver = broadcastReceiver;
	}

	@Override
	public T apply(T element) throws InterruptedException, NetworkException {
		return apply(element,getChildren());
	}

	@Override
	public ReduceFunction<T> getReduceFunction() {
		return reduceReceiver.getReduceFunction();
	}

	@Override
	public T apply(T element, List<? extends Identifier> order)
			throws InterruptedException, NetworkException {
		T result = null;
		if (getParent() == null) {
			result = reduceReceiver.reduce(order);
			broadcastSender.send(result);
		} else {
			reduceSender.send(element);
			result = broadcastReceiver.receive();
		}
		return result;
	}

}
