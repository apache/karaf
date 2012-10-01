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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	    List<Feature> installedFeatures = Arrays.asList(featuresService.listInstalledFeatures());
		List<Set<String>> stagedFeatureNames = parseBootFeatures(boot);
        List<Set<Feature>> stagedFeatures = toFeatureSetList(stagedFeatureNames);

        try {
            for (Set<Feature> features : stagedFeatures) {
                features.removeAll(installedFeatures);
                featuresService.installFeatures(features, EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));                
            }
        } catch (Exception e) {
            LOGGER.error("Error installing boot features", e);
        }
	}
	
	private List<Set<Feature>> toFeatureSetList(List<Set<String>> stagedFeatures) {
	    ArrayList<Set<Feature>> result = new ArrayList<Set<Feature>>();
	    for (Set<String> features : stagedFeatures) {
	        HashSet<Feature> featureSet = new HashSet<Feature>();
            for (String featureName : features) {
                try {
                    Feature feature = getFeature(featureName);
                    if (feature == null) {
                        LOGGER.error("Error Boot feature " + featureName + " not found");
                    } else {
                        featureSet.add(feature);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error getting feature for feature string " + featureName, e);
                }
            }
            result.add(featureSet);
        }
        return result;
	}
	
	/**
	 * 
	 * @param featureSt either feature name or <featurename>;version=<version>
	 * @return feature matching the feature string
	 * @throws Exception
	 */
    private Feature getFeature(String featureSt) throws Exception {
        String[] parts = featureSt.trim().split(";");
        String featureName = parts[0];
        String featureVersion = null;
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
        return featuresService.getFeature(featureName, featureVersion);
    }
    
    protected List<Set<String>> parseBootFeatures(String bootFeatures) {
        Pattern pattern = Pattern.compile("(\\((.+))\\),|.+");
        Matcher matcher = pattern.matcher(bootFeatures);
        List<Set<String>> result = new ArrayList<Set<String>>();
        while (matcher.find()) {
            String group = matcher.group(2) != null ? matcher.group(2) : matcher.group();
            result.add(parseFeatureList(group));
        }
        return result;
    }

    private Set<String> parseFeatureList(String group) {
        HashSet<String> features = new HashSet<String>(Arrays.asList(group.trim().split("\\s*,\\s*")));
        return features;
    }

}
