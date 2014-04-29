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
package org.apache.karaf.features.internal.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.karaf.features.internal.util.MapUtils;

public class State {

    public final AtomicBoolean bootDone = new AtomicBoolean();
    public final Set<String> repositories = new TreeSet<>();
    public final Map<String, Set<String>> requestedFeatures = new HashMap<>();
    public final Map<String, Set<String>> installedFeatures = new HashMap<>();
    public final Map<String, Map<String, String>> stateFeatures = new HashMap<>();
    public final Map<String, Set<Long>> managedBundles = new HashMap<>();
    public final Map<Long, Long> bundleChecksums = new HashMap<>();

    public State copy() {
        State state = new State();
        state.bootDone.set(bootDone.get());
        MapUtils.copy(repositories, state.repositories);
        MapUtils.copy(requestedFeatures, state.requestedFeatures);
        MapUtils.copy(installedFeatures, state.installedFeatures);
        MapUtils.copy(stateFeatures, state.stateFeatures);
        MapUtils.copy(managedBundles, state.managedBundles);
        MapUtils.copy(bundleChecksums, state.bundleChecksums);
        return state;
    }

}
