/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.io.network.nggroup.impl.task;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.io.network.impl.NetworkService;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupServiceClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommClient;
import com.microsoft.reef.io.network.nggroup.api.task.GroupCommNetworkHandler;
import com.microsoft.reef.io.network.nggroup.impl.config.parameters.SerializedGroupConfigs;
import com.microsoft.reef.io.network.proto.ReefNetworkGroupCommProtos.GroupCommMessage;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.Parameter;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.ConfigurationSerializer;

/**
 *
 */
public class GroupCommClientImpl implements GroupCommClient {
  private static final Logger LOG = Logger.getLogger(GroupCommClientImpl.class.getName());

  private final Map<Class<? extends Name<String>>, CommunicationGroupServiceClient> communicationGroups;

  @Inject
  public GroupCommClientImpl (@Parameter(SerializedGroupConfigs.class) final Set<String> groupConfigs,
                              @Parameter(TaskConfigurationOptions.Identifier.class) final String taskId,
                              final GroupCommNetworkHandler groupCommNetworkHandler,
                              final NetworkService<GroupCommMessage> netService,
                              final ConfigurationSerializer configSerializer) {
    this.communicationGroups = new HashMap<>();
    LOG.finest("GroupCommHandler-" + groupCommNetworkHandler.toString());
    for (final String groupConfigStr : groupConfigs) {
      try {
        final Configuration groupConfig = configSerializer.fromString(groupConfigStr);

        final Injector injector = Tang.Factory.getTang().newInjector(groupConfig);
        injector.bindVolatileParameter(TaskConfigurationOptions.Identifier.class, taskId);
        injector.bindVolatileInstance(GroupCommNetworkHandler.class, groupCommNetworkHandler);
        injector.bindVolatileInstance(NetworkService.class, netService);

        final CommunicationGroupServiceClient commGroupClient = injector.getInstance(CommunicationGroupServiceClient.class);

        this.communicationGroups.put(commGroupClient.getName(), commGroupClient);

      } catch (BindException | IOException e) {
        throw new RuntimeException("Unable to deserialize operator config", e);
      } catch (final InjectionException e) {
        throw new RuntimeException("Unable to deserialize operator config", e);
      }
    }

  }

  @Override
  public CommunicationGroupClient getCommunicationGroup (final Class<? extends Name<String>> groupName) {
    return communicationGroups.get(groupName);
  }
}
