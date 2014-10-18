/**
 * Copyright (C) 2014 Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.reef.examples.nggroup.tron;

import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.examples.nggroup.tron.utils.Timer;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.tang.Configuration;

/**
 * Runs BGD on the local runtime.
 */
public class TRONLocal {
  private static final String NUM_LOCAL_THREADS = "20";
  private static final int TIMEOUT = 10 * Timer.MINUTES;

  public static void main(final String[] args) throws Exception {

    final TRONClient bgdClient = TRONClient.fromCommandLine(args);

    final Configuration runtimeConfiguration = LocalRuntimeConfiguration.CONF
        .set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, NUM_LOCAL_THREADS)
        .build();

    final String jobName = System.getProperty("user.name") + "-" + "ResourceAwareTRONLocal";

    final LauncherStatus status = bgdClient.run(runtimeConfiguration, jobName, TIMEOUT);

    System.out.println("Result: " + status);
  }
}