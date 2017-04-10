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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootFeaturesInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootFeaturesInstaller.class);

    private final FeaturesServiceImpl featuresService;
    private final BundleContext bundleContext;
    private final String[] repositories;
    private final String features;
    private final boolean asynchronous;
    
    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * The system separator character.
     */
    private static final char SYSTEM_SEPARATOR = File.separatorChar;
    
    public BootFeaturesInstaller(BundleContext bundleContext,
                                 FeaturesServiceImpl featuresService,
                                 String[] repositories,
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
            for (String repo : repositories) {
                repo = repo.trim();
                if (!repo.isEmpty()) {
                    repo = separatorsToUnix(repo);
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
        List<Set<String>> stages = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(bootFeatures, " \t\r\n,()", true);
        int paren = 0;
        Set<String> stage = new LinkedHashSet<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals("(")) {
                if (paren == 0) {
                    if (!stage.isEmpty()) {
                        stages.add(stage);
                        stage = new LinkedHashSet<>();
                    }
                    paren++;
                } else {
                    throw new IllegalArgumentException("Bad syntax in boot features: '" + bootFeatures + "'");
                }
            } else if (token.equals(")")) {
                if (paren == 1) {
                    if (!stage.isEmpty()) {
                        stages.add(stage);
                        stage = new LinkedHashSet<>();
                    }
                    paren--;
                } else {
                    throw new IllegalArgumentException("Bad syntax in boot features: '" + bootFeatures + "'");
                }
            } else if (!token.matches("[ \t\r\n]+|,")) { // ignore spaces and commas
                stage.add(token);
            }
        }
        if (!stage.isEmpty()) {
            stages.add(stage);
        }
        return stages;
    }

    private void publishBootFinished() {
        if (bundleContext != null) {
            BootFinished bootFinished = new BootFinished() {
            };
            bundleContext.registerService(BootFinished.class, bootFinished, new Hashtable<String, String>());
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Converts all separators to the Unix separator of forward slash.
     * 
     * @param path  the path to be changed, null ignored
     * @return the updated path
     */
    private String separatorsToUnix(String path) {
        if (SYSTEM_SEPARATOR == WINDOWS_SEPARATOR) {
            // running under windows
            if (path == null || path.indexOf(WINDOWS_SEPARATOR) == -1) {
                return path;
            }
            
            path = path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
            LOGGER.debug("Converted path to unix separators: {}", path);
        }
        return path;
    }
}
