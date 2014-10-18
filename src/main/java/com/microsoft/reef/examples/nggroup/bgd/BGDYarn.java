/**
 * Copyright (C) 2014 Microsoft Corporation
 */
package com.microsoft.reef.examples.nggroup.bgd;

import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.examples.nggroup.bgd.utils.Timer;
import com.microsoft.reef.runtime.yarn.client.YarnClientConfiguration;
import com.microsoft.tang.Configuration;

/**
 * Runs BGD on the YARN runtime.
 */
public class BGDYarn {

  private static final int TIMEOUT = 4 * Timer.HOURS;

  public static void main(final String[] args) throws Exception {

    final BGDClient bgdClient = BGDClient.fromCommandLine(args);

    final Configuration runtimeConfiguration = YarnClientConfiguration.CONF
        .set(YarnClientConfiguration.JVM_HEAP_SLACK, "0.1")
        .build();

    final String jobName = System.getProperty("user.name") + "-" + "BR-ResourceAwareBGD-YARN";

    final LauncherStatus status = bgdClient.run(runtimeConfiguration, jobName, TIMEOUT);

    System.out.println("Result: " + status);
  }
}
