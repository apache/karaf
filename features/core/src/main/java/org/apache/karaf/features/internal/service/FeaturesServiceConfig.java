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

import org.apache.karaf.features.FeaturesService;

public class FeaturesServiceConfig {

    /**
     * Range to use when a version is specified on a feature dependency.
     * The default is {@link org.apache.karaf.features.FeaturesService#DEFAULT_FEATURE_RESOLUTION_RANGE}
     */
    public final String featureResolutionRange;
    
    /**
     * Range to use when verifying if a bundle should be updated or
     * new bundle installed.
     * The default is {@link org.apache.karaf.features.FeaturesService#DEFAULT_BUNDLE_UPDATE_RANGE}
     */
    public final String bundleUpdateRange;
    
    /**
     * Use CRC to check snapshot bundles and update them if changed.
     * Either:
     * - none : never update snapshots
     * - always : always update snapshots
     * - crc : use CRC to detect changes
     */
    public final String updateSnapshots;
    
    public final int downloadThreads;
    
    public final long scheduleDelay;
    
    public final int scheduleMaxRun;
    
    /**
     * Service requirements enforcement
     */
    public final String serviceRequirements;

    /**
     * Location of <code>etc/blacklisted.properties</code>
     */
    @Deprecated
    public final String blacklisted;

    /**
     * Location of <code>etc/org.apache.karaf.features.xml</code>
     */
    public final String featureModifications;

    /**
     * Location of <code>etc/overrides.properties</code>
     */
    @Deprecated
    public final String overrides;

    public FeaturesServiceConfig() {
        this(null, null, null);
    }

    public FeaturesServiceConfig(String featureModifications) {
        this(null, FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE, FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE, null, 1, 0, 0, null, featureModifications, null);
    }

    @Deprecated
    public FeaturesServiceConfig(String overrides, String blacklisted, String featureModifications) {
        this(overrides, FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE, FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE, null, 1, 0, 0, blacklisted, featureModifications, null);
    }

    public FeaturesServiceConfig(String featureResolutionRange, String bundleUpdateRange, String updateSnapshots, int downloadThreads, long scheduleDelay, int scheduleMaxRun, String featureModifications, String serviceRequirements) {
        this.overrides = null;
        this.featureResolutionRange = featureResolutionRange;
        this.bundleUpdateRange = bundleUpdateRange;
        this.updateSnapshots = updateSnapshots;
        this.downloadThreads = downloadThreads;
        this.scheduleDelay = scheduleDelay;
        this.scheduleMaxRun = scheduleMaxRun;
        this.blacklisted = null;
        this.featureModifications = featureModifications;
        this.serviceRequirements = serviceRequirements;
    }

    @Deprecated
    public FeaturesServiceConfig(String overrides, String featureResolutionRange, String bundleUpdateRange, String updateSnapshots, int downloadThreads, long scheduleDelay, int scheduleMaxRun, String blacklisted, String featureModifications, String serviceRequirements) {
        this.overrides = overrides;
        this.featureResolutionRange = featureResolutionRange;
        this.bundleUpdateRange = bundleUpdateRange;
        this.updateSnapshots = updateSnapshots;
        this.downloadThreads = downloadThreads;
        this.scheduleDelay = scheduleDelay;
        this.scheduleMaxRun = scheduleMaxRun;
        this.blacklisted = blacklisted;
        this.featureModifications = featureModifications;
        this.serviceRequirements = serviceRequirements;
    }

}
