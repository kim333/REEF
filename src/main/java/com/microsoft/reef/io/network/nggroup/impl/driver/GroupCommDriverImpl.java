/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.driver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.context.ServiceConfiguration;
import com.microsoft.reef.driver.evaluator.FailedEvaluator;
import com.microsoft.reef.driver.parameters.DriverIdentifier;
import com.microsoft.reef.driver.task.FailedTask;
import com.microsoft.reef.driver.task.RunningTask;
import com.microsoft.reef.io.network.Message;
import com.microsoft.reef.io.network.impl.BindNSToTask;
import com.microsoft.reef.io.network.impl.MessagingTransportFactory;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.impl.NetworkServiceClosingHandler;
import com.microsoft.reef.io.network.impl.NetworkServiceParameters;
import com.microsoft.reef.io.network.impl.UnbindNSFromTask;
import com.microsoft.reef.io.network.naming.NameServer;
import com.microsoft.reef.io.network.naming.NameServerParameters;
import com.microsoft.reef.io.network.nggroup.api.driver.CommunicationGroupDriver;
import com.microsoft.reef.io.network.nggroup.api.driver.GroupCommDriver;
import com.microsoft.reef.io.network.nggroup.api.driver.GroupCommServiceDriver;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessage;
import com.microsoft.reef.io.network.nggroup.impl.GroupCommunicationMessageCodec;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.SerializedGroupConfigs;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.TreeTopologyFanOut;
import com.microsoft.reef.io.network.nggroup.impl.task.GroupCommNetworkHandlerImpl;
import com.microsoft.reef.io.network.nggroup.impl.utils.BroadcastingEventHandler;
import com.microsoft.reef.io.network.nggroup.impl.utils.Utils;
import com.microsoft.reef.io.network.util.StringIdentifierFactory;
import com.microsoft.reef.util.SingletonAsserter;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.formats.ConfigurationSerializer;
import com.microsoft.wake.EStage;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.IdentifierFactory;
import com.microsoft.wake.impl.LoggingEventHandler;
import com.microsoft.wake.impl.SingleThreadStage;
import com.microsoft.wake.impl.SyncStage;
import com.microsoft.wake.impl.ThreadPoolStage;
import com.microsoft.wake.remote.NetUtils;

/**
 * Sets up various stages to handle REEF events and adds the per communication
 * group stages to them whenever a new communication group is created.
 *
 * Also starts the NameService & the NetworkService on the driver
 */
public class GroupCommDriverImpl implements GroupCommServiceDriver {
  private static final Logger LOG = Logger.getLogger(GroupCommDriverImpl.class.getName());
  /**
   * TANG instance
   */
  private static final Tang tang = Tang.Factory.getTang();

  private final AtomicInteger contextIds = new AtomicInteger(0);

  private final IdentifierFactory idFac = new StringIdentifierFactory();

  private final NameServer nameService = new NameServer(0, idFac);

  private final String nameServiceAddr;
  private final int nameServicePort;

  private final Map<Class<? extends Name<String>>, CommunicationGroupDriver> commGroupDrivers = new HashMap<>();

  private final ConfigurationSerializer confSerializer;

  private final NetworkService<GroupCommunicationMessage> netService;

  private final EStage<GroupCommunicationMessage> senderStage;

  private final String driverId;
  private final BroadcastingEventHandler<RunningTask> groupCommRunningTaskHandler;
  private final EStage<RunningTask> groupCommRunningTaskStage;
  private final BroadcastingEventHandler<FailedTask> groupCommFailedTaskHandler;
  private final EStage<FailedTask> groupCommFailedTaskStage;
  private final BroadcastingEventHandler<FailedEvaluator> groupCommFailedEvaluatorHandler;
  private final EStage<FailedEvaluator> groupCommFailedEvaluatorStage;
  private final GroupCommMessageHandler groupCommMessageHandler;
  private final EStage<GroupCommunicationMessage> groupCommMessageStage;
  private final int fanOut;

  @Inject
  public GroupCommDriverImpl (final ConfigurationSerializer confSerializer,
                              @Parameter(DriverIdentifier.class) final String driverId,
                              @Parameter(TreeTopologyFanOut.class) final int fanOut) {
    assert(SingletonAsserter.assertSingleton(getClass()));
    this.driverId = driverId;
    this.fanOut = fanOut;
    this.nameServiceAddr = NetUtils.getLocalAddress();
    this.nameServicePort = nameService.getPort();
    this.confSerializer = confSerializer;
    this.groupCommRunningTaskHandler = new BroadcastingEventHandler<>();
    this.groupCommRunningTaskStage = new SyncStage<>("GroupCommRunningTaskStage", groupCommRunningTaskHandler);
    this.groupCommFailedTaskHandler = new BroadcastingEventHandler<>();
    this.groupCommFailedTaskStage = new SyncStage<>("GroupCommFailedTaskStage", groupCommFailedTaskHandler);
    this.groupCommFailedEvaluatorHandler = new BroadcastingEventHandler<>();
    this.groupCommFailedEvaluatorStage = new SyncStage<>("GroupCommFailedEvaluatorStage",
                                                         groupCommFailedEvaluatorHandler);
    this.groupCommMessageHandler = new GroupCommMessageHandler();
    this.groupCommMessageStage = new SingleThreadStage<>("GroupCommMessageStage", groupCommMessageHandler, 100 * 1000);
    this.netService = new NetworkService<>(idFac, 0, nameServiceAddr, nameServicePort,
                                           new GroupCommunicationMessageCodec(), new MessagingTransportFactory(),
                                           new EventHandler<Message<GroupCommunicationMessage>>() {

                                             @Override
                                             public void onNext (final Message<GroupCommunicationMessage> msg) {
                                               groupCommMessageStage.onNext(Utils.getGCM(msg));
                                             }
                                           }, new LoggingEventHandler<Exception>());
    this.netService.registerId(idFac.getNewInstance(driverId));
    this.senderStage = new ThreadPoolStage<>("SrcCtrlMsgSender", new CtrlMsgSender(idFac, netService), 5);
  }

  @Override
  public CommunicationGroupDriver newCommunicationGroup (final Class<? extends Name<String>> groupName,
                                                         final int numberOfTasks) {
    LOG.entering("GroupCommDriverImpl", "newCommunicationGroup", new Object[] { Utils.simpleName(groupName), numberOfTasks });
    final BroadcastingEventHandler<RunningTask> commGroupRunningTaskHandler = new BroadcastingEventHandler<>();
    final BroadcastingEventHandler<FailedTask> commGroupFailedTaskHandler = new BroadcastingEventHandler<>();
    final BroadcastingEventHandler<FailedEvaluator> commGroupFailedEvaluatorHandler = new BroadcastingEventHandler<>();
    final BroadcastingEventHandler<GroupCommunicationMessage> commGroupMessageHandler = new BroadcastingEventHandler<>();
    final CommunicationGroupDriver commGroupDriver = new CommunicationGroupDriverImpl(groupName, confSerializer,
                                                                                      senderStage,
                                                                                      commGroupRunningTaskHandler,
                                                                                      commGroupFailedTaskHandler,
                                                                                      commGroupFailedEvaluatorHandler,
                                                                                      commGroupMessageHandler,
                                                                                      driverId, numberOfTasks, fanOut);
    commGroupDrivers.put(groupName, commGroupDriver);
    groupCommRunningTaskHandler.addHandler(commGroupRunningTaskHandler);
    groupCommFailedTaskHandler.addHandler(commGroupFailedTaskHandler);
    groupCommMessageHandler.addHandler(groupName, commGroupMessageHandler);
    LOG.exiting("GroupCommDriverImpl", "newCommunicationGroup", "Created communication group: " + Utils.simpleName(groupName));
    return commGroupDriver;
  }

  @Override
  public boolean isConfigured (final ActiveContext activeContext) {
    LOG.entering("GroupCommDriverImpl", "isConfigured", activeContext.getId());
    final boolean retVal = activeContext.getId().startsWith("GroupCommunicationContext-");
    LOG.exiting("GroupCommDriverImpl", "isConfigured", retVal);
    return retVal;
  }

  @Override
  public Configuration getContextConfiguration () {
    LOG.entering("GroupCommDriverImpl", "getContextConf");
    final Configuration retVal = ContextConfiguration.CONF.set(ContextConfiguration.IDENTIFIER,
                                         "GroupCommunicationContext-" + contextIds.getAndIncrement()).build();
    LOG.exiting("GroupCommDriverImpl", "getContextConf", confSerializer.toString(retVal));
    return retVal;
  }

  @Override
  public Configuration getServiceConfiguration () {
    LOG.entering("GroupCommDriverImpl", "getServiceConf");
    final Configuration serviceConfiguration = ServiceConfiguration.CONF.set(ServiceConfiguration.SERVICES,
                                                                             NetworkService.class)
                                                                        .set(ServiceConfiguration.SERVICES,
                                                                             GroupCommNetworkHandlerImpl.class)
                                                                        .set(ServiceConfiguration.ON_CONTEXT_STOP,
                                                                             NetworkServiceClosingHandler.class)
                                                                        .set(ServiceConfiguration.ON_TASK_STARTED,
                                                                             BindNSToTask.class)
                                                                        .set(ServiceConfiguration.ON_TASK_STOP,
                                                                             UnbindNSFromTask.class).build();
    final Configuration retVal = tang.newConfigurationBuilder(serviceConfiguration)
               .bindNamedParameter(NetworkServiceParameters.NetworkServiceCodec.class,
                                   GroupCommunicationMessageCodec.class)
               .bindNamedParameter(NetworkServiceParameters.NetworkServiceHandler.class,
                                   GroupCommNetworkHandlerImpl.class)
               .bindNamedParameter(NetworkServiceParameters.NetworkServiceExceptionHandler.class,
                                   ExceptionHandler.class)
               .bindNamedParameter(NameServerParameters.NameServerAddr.class, nameServiceAddr)
               .bindNamedParameter(NameServerParameters.NameServerPort.class, Integer.toString(nameServicePort))
               .bindNamedParameter(NetworkServiceParameters.NetworkServicePort.class, "0").build();
    LOG.exiting("GroupCommDriverImpl", "getServiceConf", confSerializer.toString(retVal));
    return retVal;
  }

  @Override
  public Configuration getTaskConfiguration (final Configuration partialTaskConf) {
    LOG.entering("GroupCommDriverImpl", "getTaskConfiguration", new Object[] { confSerializer.toString(partialTaskConf) });
    final JavaConfigurationBuilder jcb = Tang.Factory.getTang().newConfigurationBuilder(partialTaskConf);
    for (final CommunicationGroupDriver commGroupDriver : commGroupDrivers.values()) {
      final Configuration commGroupConf = commGroupDriver.getTaskConfiguration(partialTaskConf);
      if(commGroupConf!=null) {
        jcb.bindSetEntry(SerializedGroupConfigs.class, confSerializer.toString(commGroupConf));
      }
    }
    final Configuration retVal = jcb.build();
    LOG.exiting("GroupCommDriverImpl", "getTaskConfiguration", confSerializer.toString(retVal));
    return retVal;
  }

  /**
   * @return the groupCommRunningTaskStage
   */
  @Override
  public EStage<RunningTask> getGroupCommRunningTaskStage () {
    LOG.entering("GroupCommDriverImpl", "getGroupCommRunningTaskStage");
    LOG.exiting("GroupCommDriverImpl", "getGroupCommRunningTaskStage", "Returning GroupCommRunningTaskStage");
    return groupCommRunningTaskStage;
  }

  /**
   * @return the groupCommFailedTaskStage
   */
  @Override
  public EStage<FailedTask> getGroupCommFailedTaskStage () {
    LOG.entering("GroupCommDriverImpl", "getGroupCommFailedTaskStage");
    LOG.exiting("GroupCommDriverImpl", "getGroupCommFailedTaskStage", "Returning GroupCommFailedTaskStage");
    return groupCommFailedTaskStage;
  }

  /**
   * @return the groupCommFailedEvaluatorStage
   */
  @Override
  public EStage<FailedEvaluator> getGroupCommFailedEvaluatorStage () {
    LOG.entering("GroupCommDriverImpl", "getGroupCommFailedEvaluatorStage");
    LOG.exiting("GroupCommDriverImpl", "getGroupCommFailedEvaluatorStage", "Returning GroupCommFaileEvaluatorStage");
    return groupCommFailedEvaluatorStage;
  }

}
