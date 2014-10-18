/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.group.impl.operators.basic;

import com.microsoft.reef.exception.evaluator.NetworkException;
import com.microsoft.reef.io.network.group.impl.GroupCommNetworkHandler;
import com.microsoft.reef.io.network.group.impl.operators.basic.config.GroupParameters;
import com.microsoft.reef.io.network.group.operators.Reduce;
import com.microsoft.reef.io.network.group.operators.Reduce.ReduceFunction;
import com.microsoft.reef.io.network.group.operators.ReduceScatter;
import com.microsoft.reef.io.network.group.operators.Scatter;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.reef.io.network.util.Utils;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.wake.ComparableIdentifier;
import com.microsoft.wake.Identifier;
import com.microsoft.wake.IdentifierFactory;
import com.microsoft.wake.remote.Codec;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link ReduceScatter}
 * @param <T>
 */
public class ReduceScatterOp<T> extends SenderReceiverBase implements ReduceScatter<T> {
	Reduce.Sender<T> reduceSender;
	Reduce.Receiver<T> reduceReceiver;
	Scatter.Sender<T> scatterSender;
	Scatter.Receiver<T> scatterReceiver;
	
	@Inject
	public ReduceScatterOp(
			NetworkService<GroupCommMessage> netService,
			GroupCommNetworkHandler multiHandler,
			@Parameter(GroupParameters.ReduceScatter.DataCodec.class) Codec<T> codec,
			@Parameter(GroupParameters.ReduceScatter.SelfId.class) String self,
			@Parameter(GroupParameters.ReduceScatter.ParentId.class) String parent, 
			@Parameter(GroupParameters.ReduceScatter.ChildIds.class) String children,
			@Parameter(GroupParameters.IDFactory.class) IdentifierFactory idFac,
			@Parameter(GroupParameters.AllReduce.ReduceFunction.class) ReduceFunction<T> redFunc){
		this(
				new ReduceOp.Sender<>(netService, multiHandler, codec, self, parent, children, idFac, redFunc),
				new ReduceOp.Receiver<>(netService, multiHandler, codec, self, parent, children, idFac, redFunc),
				new ScatterOp.Sender<>(netService, multiHandler, codec, self, parent, children, idFac),
				new ScatterOp.Receiver<>(netService, multiHandler, codec, self, parent, children, idFac),
				idFac.getNewInstance(self),
				(parent.equals(GroupParameters.defaultValue)) ? null : idFac.getNewInstance(parent),
				(children.equals(GroupParameters.defaultValue)) ? null : Utils.parseListCmp(children, idFac)
			);
	}

	public ReduceScatterOp(
			Reduce.Sender<T> reduceSender,
			Reduce.Receiver<T> reduceReceiver, 
			Scatter.Sender<T> scatterSender,
			Scatter.Receiver<T> scatterReceiver, Identifier self,
			Identifier parent, List<ComparableIdentifier> children) {
		super(self,parent,children);
		this.reduceSender = reduceSender;
		this.reduceReceiver = reduceReceiver;
		this.scatterSender = scatterSender;
		this.scatterReceiver = scatterReceiver;
	}

	@Override
	public List<T> apply(List<T> elements, List<Integer> counts)
			throws InterruptedException, NetworkException {
		return apply(elements,counts,getChildren());
	}

	@Override
	public ReduceFunction<T> getReduceFunction() {
		return reduceReceiver.getReduceFunction();
	}

	@Override
	public List<T> apply(List<T> elements, List<Integer> counts,
			List<? extends Identifier> order) throws InterruptedException,
			NetworkException {
		List<T> result = new ArrayList<>(elements.size());
		for (T element : elements) {
			if (getParent() == null)
				result.add(reduceReceiver.reduce(order));
			else
				reduceSender.send(element);
		}
		if (getParent() == null) {
			List<T> retVal = result.subList(0, counts.get(0));
			List<T> remainingElems = result.subList(counts.get(0),
					result.size());
			List<Integer> remainingCounts = counts.subList(1, counts.size());
			scatterSender.send(remainingElems, remainingCounts, order);
			return retVal;
		} else {
			return scatterReceiver.receive();
		}
	}
}
