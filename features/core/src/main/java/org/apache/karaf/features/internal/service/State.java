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

/**
 * Representation of the state of features service from the point of view of <em>logical requirements</em>
 * which are translated into bundles and features installed in {@link org.eclipse.equinox.region.Region regions}.
 * It's a collection of:<ul>
 *     <li>used repositories</li>
 *     <li>region -&gt; requirements (logical feature requirements)</li>
 *     <li>region -&gt; installed features (actual features installed - including conditionals and dependant features)</li>
 *     <li>region -&gt; installed features -&gt; state of feature installation</li>
 *     <li>region -&gt; bundle ids (for bundles installed via features service, a.k.a. <em>managed bundles</em>)</li>
 *     <li>bundle id -&gt; checksum</li>
 * </ul>
 * State is replaced (swapped) after uninstalling/updating/installing all the bundles as requested, but
 * before resolving/refreshing them. Before State is set, work is done on the instance of Deployer.DeploymentState.
 */
public class State {

    public final AtomicBoolean bootDone = new AtomicBoolean();
    /**
     * Set while a deployment changes bundles, from before its first bundle operation until all its bundles have been
     * started, otherwise <code>null</code>. If it is still set when the features service starts in a later framework
     * launch, the deployment was interrupted (e.g. by a shutdown) and has to be resumed, as the framework does not
     * start bundles which have only been installed.
     */
    public volatile PendingDeployment pendingDeployment;
    public final Set<String> repositories = new TreeSet<>();
    
    /** Map from region name to Set of feature requirements (<code>feature:name/version-range</code>) */
    public final Map<String, Set<String>> requirements = new HashMap<>();
    /** Map from region name to Set of feature id (<code>name/version</code>) */
    public final Map<String, Set<String>> installedFeatures = new HashMap<>();
    
    /** State of features by region and feature id (<code>name/version</code>) */
    public final Map<String, Map<String, String>> stateFeatures = new HashMap<>();

    /** Map from region name to Set of ids of bundles installed via some features or requirements */
    public final Map<String, Set<Long>> managedBundles = new HashMap<>();
    /** Map from bundle id to bundle's java.util.zip.CRC32 */
    public final Map<Long, Long> bundleChecksums = new HashMap<>();

    public State copy() {
        State state = new State();
        copy(this, state, false);
        return state;
    }

    public void replace(State state) {
        copy(state, this, true);
    }

    private static void copy(State from, State to, boolean clear) {
        if (clear) {
            to.repositories.clear();
            to.requirements.clear();
            to.installedFeatures.clear();
            to.stateFeatures.clear();
            to.managedBundles.clear();
            to.bundleChecksums.clear();
        }
        to.bootDone.set(from.bootDone.get());
        PendingDeployment pending = from.pendingDeployment;
        to.pendingDeployment = pending != null ? pending.copy() : null;
        MapUtils.copy(from.repositories, to.repositories);
        MapUtils.copy(from.requirements, to.requirements);
        MapUtils.copy(from.installedFeatures, to.installedFeatures);
        MapUtils.copy(from.stateFeatures, to.stateFeatures);
        MapUtils.copy(from.managedBundles, to.managedBundles);
        MapUtils.copy(from.bundleChecksums, to.bundleChecksums);
    }

    /**
     * What is needed to resume a deployment that was interrupted, see {@link State#pendingDeployment}. The
     * requirements to resume are the ones of the {@link State}.
     */
    public static class PendingDeployment {
        /**
         * {@link org.osgi.framework.Constants#FRAMEWORK_UUID} of the framework launch that started the deployment.
         * It changes with each framework launch, but not when the features service is restarted, e.g. because the
         * deployment refreshes its bundle: then the deployment is still running.
         */
        public String frameworkUuid;
        /** Names of the {@link org.apache.karaf.features.FeaturesService.Option options} of the deployment */
        public final Set<String> options = new TreeSet<>();
        /**
         * Map from region name to Set of ids of the features whose configurations are to be deleted, because they
         * were uninstalled with {@link org.apache.karaf.features.FeaturesService.Option#DeleteConfigurations}
         */
        public final Map<String, Set<String>> configsToDelete = new HashMap<>();

        public PendingDeployment copy() {
            PendingDeployment copy = new PendingDeployment();
            copy.frameworkUuid = frameworkUuid;
            copy.options.addAll(options);
            MapUtils.copy(configsToDelete, copy.configsToDelete);
            return copy;
        }
    }

}
