/*
 * Copyright 2013 Microsoft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.reef.examples.nggroup.tron;

import com.microsoft.reef.io.network.nggroup.api.GroupChanges;
import com.microsoft.reef.io.network.nggroup.api.task.CommunicationGroupClient;

/**
 *
 */
public class SynchronizationUtils {
  public static  boolean chkAndUpdate(final CommunicationGroupClient communicationGroupClient) {
    long t1 = System.currentTimeMillis();
    final GroupChanges changes = communicationGroupClient.getTopologyChanges();
    long t2 = System.currentTimeMillis();
    System.out.println("Time to get TopologyChanges = " + (t2 - t1) / 1000.0 + " sec");
    if (changes.exist()) {
      System.out.println("There exist topology changes. Asking to update Topology");
      t1 = System.currentTimeMillis();
      communicationGroupClient.updateTopology();
      t2 = System.currentTimeMillis();
      System.out.println("Time to get TopologyUpdated = " + (t2 - t1) / 1000.0 + " sec");
      return true;
    } else {
      System.out.println("No changes in topology exist. So not updating topology");
      return false;
    }
  }
}
