/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd.utils;

import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.reef.driver.task.TaskConfigurationOptions;
import com.microsoft.reef.examples.nggroup.bgd.MasterTask;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.AvroConfigurationSerializer;
import com.microsoft.tang.formats.ConfigurationSerializer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SubConfiguration {

  private static final Logger LOG = Logger.getLogger(SubConfiguration.class.getName());

  @SafeVarargs
  public static Configuration from(
      final Configuration baseConf, final Class<? extends Name<?>>... classes) {

    final Injector injector = Tang.Factory.getTang().newInjector(baseConf);
    final JavaConfigurationBuilder confBuilder = Tang.Factory.getTang().newConfigurationBuilder();

    for (final Class<? extends Name<?>> clazz : classes) {
      try {
        confBuilder.bindNamedParameter(clazz,
            injector.getNamedInstance((Class<? extends Name<Object>>) clazz).toString());
      } catch (final BindException | InjectionException ex) {
        final String msg = "Exception while creating subconfiguration";
        LOG.log(Level.WARNING, msg, ex);
        throw new RuntimeException(msg, ex);
      }
    }

    return confBuilder.build();
  }

  public static void main(final String[] args) throws BindException, InjectionException {

    final Configuration conf = TaskConfiguration.CONF
        .set(TaskConfiguration.IDENTIFIER, "TASK")
        .set(TaskConfiguration.TASK, MasterTask.class)
        .build();

    final ConfigurationSerializer confSerizalizer = new AvroConfigurationSerializer();
    final Configuration subConf = SubConfiguration.from(conf, TaskConfigurationOptions.Identifier.class);
    System.out.println("Base conf:\n" + confSerizalizer.toString(conf));
    System.out.println("Sub conf:\n" + confSerizalizer.toString(subConf));
  }
}
