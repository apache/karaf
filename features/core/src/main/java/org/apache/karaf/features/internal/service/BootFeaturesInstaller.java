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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootFeaturesInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootFeaturesInstaller.class);

    private final FeaturesServiceImpl featuresService;
    private final BundleContext bundleContext;
    private final String repositories;
    private final String features;
    private final boolean asynchronous;

    /**
     * @param features list of boot features separated by comma. Optionally contains ;version=x.x.x to specify a specific feature version
     */
    public BootFeaturesInstaller(BundleContext bundleContext,
                                 FeaturesServiceImpl featuresService,
                                 String repositories,
                                 String features,
                                 boolean asynchronous) {
        this.bundleContext = bundleContext;
        this.featuresService = featuresService;
        this.repositories = repositories;
        this.features = features;
        this.asynchronous = asynchronous;
    }

    /**
     * Install boot features
     */
    public void start() {
        if (featuresService.isBootDone()) {
            publishBootFinished();
            return;
        }
        if (asynchronous) {
            new Thread("Initial Features Provisioning") {
                public void run() {
                    installBootFeatures();
                }
            }.start();
        } else {
            installBootFeatures();
        }
    }

    protected void installBootFeatures() {
        try {
            for (String repo : repositories.split(",")) {
                repo = repo.trim();
                if (!repo.isEmpty()) {
                    try {
                        featuresService.addRepository(URI.create(repo));
                    } catch (Exception e) {
                        LOGGER.error("Error installing boot feature repository " + repo, e);
                    }
                }
            }

            List<Set<String>> stagedFeatures = parseBootFeatures(features);
            for (Set<String> features : stagedFeatures) {
                featuresService.installFeatures(features, EnumSet.of(FeaturesService.Option.NoFailOnFeatureNotFound));
            }
            featuresService.bootDone();
            publishBootFinished();
        } catch (Exception e) {
            // Special handling in case the bundle has been refreshed.
            // In such a case, simply exits without logging any exception
            // as the restart should cause the feature service to finish
            // the work.
            if (e instanceof IllegalStateException) {
                try {
                    bundleContext.getBundle();
                } catch (IllegalStateException ies) {
                    return;
                }
            }
            LOGGER.error("Error installing boot features", e);
        }
    }

    protected List<Set<String>> parseBootFeatures(String bootFeatures) {
        Pattern pattern = Pattern.compile("(\\s*\\(([^)]+))\\s*\\)\\s*,\\s*|.+");
        Matcher matcher = pattern.matcher(bootFeatures);
        List<Set<String>> result = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group(2) != null ? matcher.group(2) : matcher.group();
            result.add(parseFeatureList(group));
        }
        return result;
    }

    protected Set<String> parseFeatureList(String group) {
        HashSet<String> features = new HashSet<>();
        for (String feature : Arrays.asList(group.trim().split("\\s*,\\s*"))) {
            if (feature.length() > 0) {
                features.add(feature);
            }
        }
        return features;
    }

    private void publishBootFinished() {
        if (bundleContext != null) {
            BootFinished bootFinished = new BootFinished() {
            };
            bundleContext.registerService(BootFinished.class, bootFinished, new Hashtable<String, String>());
        }
    }

}
