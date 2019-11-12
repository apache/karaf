/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.http.core.internal.proxy;

import org.apache.karaf.http.core.BalancingPolicy;

/**
 * Select sequentially (round robin) the host in the proxy provided hosts.
 *
 * WARNING: this policy is global and can be shared across several proxy servlets.
 */
public class RoundRobinBalancingPolicy implements BalancingPolicy {

    private int index = 0;

    @Override
    public String selectHost(String[] targets) {
        if (targets.length == 0) {
            return null;
        } else if (targets.length == 1) {
            return targets[0];
        } else {
            String host = targets[index];
            // moving index
            index++;
            if (index >= targets.length) {
                index = 0;
            }
            return host;
        }
    }

}
