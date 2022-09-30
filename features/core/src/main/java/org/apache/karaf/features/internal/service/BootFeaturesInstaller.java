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

import org.apache.karaf.features.BootFinished;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.util.ExitManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.*;

public class BootFeaturesInstaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootFeaturesInstaller.class);
    private static final String REQUIRE_SUCCESSFUL_BOOT = "karaf.require.successful.features.boot";

    private final FeaturesServiceImpl featuresService;
    private final BundleContext bundleContext;
    private final ExitManager exitManager;
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
                                 ExitManager exitManager,
                                 String[] repositories,
                                 String features,
                                 boolean asynchronous) {
        this.bundleContext = bundleContext;
        this.featuresService = featuresService;
        this.exitManager = exitManager;
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

        boolean quitIfUnsuccessful = Boolean.getBoolean(REQUIRE_SUCCESSFUL_BOOT);

        if (asynchronous) {
            new Thread("Initial Features Provisioning") {
                public void run() {
                    installBootFeatures(quitIfUnsuccessful);
                }
            }.start();
        } else {
            installBootFeatures(quitIfUnsuccessful);
        }
    }

    protected void installBootFeatures(boolean quitIfUnsuccessful) {
        try {
            addRepositories(quitIfUnsuccessful);

            List<Set<String>> stagedFeatures = parseBootFeatures(features);
            for (Set<String> features : stagedFeatures) {
                EnumSet<FeaturesService.Option> options;
                if (quitIfUnsuccessful) {
                    options = EnumSet.noneOf(FeaturesService.Option.class);
                } else {
                    options = EnumSet.of(FeaturesService.Option.NoFailOnFeatureNotFound);
                }
                featuresService.installFeatures(features, options);
            }
            featuresService.bootDone();
            publishBootFinished();
        } catch (Throwable e) {
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
            if (quitIfUnsuccessful) {
                exitAfterFailedBoot();
            }
        }
    }

    private void addRepositories(boolean quitIfUnsuccessful) {
        for (String repo : repositories) {
            repo = repo.trim();
            if (!repo.isEmpty()) {
                repo = separatorsToUnix(repo);
                repo = encodePath(repo);
                try {
                    featuresService.addRepository(URI.create(repo));
                } catch (Exception e) {
                    LOGGER.error("Error installing boot feature repository " + repo, e);
                    if (quitIfUnsuccessful) {
                        exitAfterFailedBoot();
                    }
                }
            }
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
    /**
     * Converts all invalid characters in a path to a format supported by {@link URI#create(String)}.
     *
     * @param path the path to encode, null ignored
     * @return the encoded path
     */
    private String encodePath(String path) {
        if (path == null) {
            return null;
        }

        return path.replace(" ", "%20");
    }

    private void exitAfterFailedBoot() {
        LOGGER.error("Exiting Karaf after a failed features boot" +
                " (as configured by {} in system.properties)", REQUIRE_SUCCESSFUL_BOOT);
        exitManager.exit();
    }
}
