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
package org.apache.karaf.features.internal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages installation of the boot features in the background
 */
public class BootFeaturesInstaller {
	private static final Logger LOGGER = LoggerFactory.getLogger(BootFeaturesInstaller.class);

    public static String VERSION_PREFIX = "version=";

    private final FeaturesService featuresService;
    private final String boot;

    /**
     * 
     * @param featuresService
     * @param boot list of boot features separated by comma. Optionally contains ;version=x.x.x to specify a specific feature version
     */
    public BootFeaturesInstaller(FeaturesService featuresService, String boot) {
		this.featuresService = featuresService;
		this.boot = boot;
	}
    
    /**
     * Install boot features
     * @throws Exception
     */
    public void start() throws Exception {
        if (boot != null) {
            new Thread() {
                public void run() {
                    installBootFeatures();
                }
            }.start();
        }
    }
    
	void installBootFeatures() {
		Set<Feature> features = determineFeaturesToInstall();
        try {
            featuresService.installFeatures(features, EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));
        } catch (Exception e) {
            LOGGER.error("Error installing boot features", e);
        }
	}
	
    private Set<Feature> determineFeaturesToInstall() {
    	List<Feature> installedFeatures = Arrays.asList(featuresService.listInstalledFeatures());
        String[] list = boot.split(",");
        Set<Feature> features = new LinkedHashSet<Feature>();
        for (String f : list) {
            f = f.trim();
            if (f.length() > 0) {
                String featureVersion = null;

                // first we split the parts of the feature string to gain access to the version info
                // if specified
                String[] parts = f.split(";");
                String featureName = parts[0];
                for (String part : parts) {
                    // if the part starts with "version=" it contains the version info
                    if (part.startsWith(VERSION_PREFIX)) {
                        featureVersion = part.substring(VERSION_PREFIX.length());
                    }
                }

                if (featureVersion == null) {
                    // no version specified - use default version
                    featureVersion = org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION;
                }

                try {
                    // try to grab specific feature version
                    Feature feature = featuresService.getFeature(featureName, featureVersion);
                    if (feature != null && !installedFeatures.contains(feature)) {
                        features.add(feature);
                    } else {
                        LOGGER.error("Error installing boot feature " + f + ": feature not found");
                    }
                } catch (Exception e) {
                    LOGGER.error("Error installing boot feature " + f, e);
                }
            }
        }
		return features;
	}
}
