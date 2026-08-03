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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.util.MultiException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.UNINSTALLED;

/**
 * A simple deployer that installs features without using OSGi capabilities/requirements resolution.
 * Features are installed in the order they are defined: first feature dependencies (recursively),
 * then bundles in declaration order.
 */
public class SimpleDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDeployer.class);

    /**
     * Callback interface for the simple deployer to interact with the features service
     * and OSGi framework.
     */
    public interface SimpleDeployCallback {
        void print(String message, boolean verbose);
        void saveState(State state);
        void callListeners(DeploymentEvent deployEvent);
        void callListeners(FeatureEvent featureEvent);
        Bundle installBundle(String region, String uri, InputStream is) throws BundleException;
        void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException;
        void uninstall(Bundle bundle) throws BundleException;
        void startBundle(Bundle bundle) throws BundleException;
        void stopBundle(Bundle bundle, int options) throws BundleException;
        void setBundleStartLevel(Bundle bundle, int startLevel);
        void refreshPackages(Collection<Bundle> bundles) throws InterruptedException;
        void installConfigs(Feature feature) throws IOException;
        void installLibraries(Feature feature) throws IOException;
        void deleteConfigs(Feature feature) throws IOException;
        BundleInstallSupport.FrameworkInfo getInfo();
    }

    private final DownloadManager downloadManager;
    private final SimpleDeployCallback callback;

    public SimpleDeployer(DownloadManager downloadManager, SimpleDeployCallback callback) {
        this.downloadManager = downloadManager;
        this.callback = callback;
    }

    /**
     * Deploy the given requirements.
     *
     * @param featuresById   all available features indexed by id
     * @param featuresByName all available features indexed by name, then version
     * @param requirements   the required features per region
     * @param state          current state
     * @param options        deployment options
     */
    public void deploy(Map<String, Feature> featuresById,
                       Map<String, Map<String, Feature>> featuresByName,
                       Map<String, Set<String>> requirements,
                       State state,
                       EnumSet<Option> options) throws Exception {
        boolean noStart = options.contains(Option.NoAutoStartBundles);
        boolean verbose = options.contains(Option.Verbose);
        boolean simulate = options.contains(Option.Simulate);
        boolean deleteConfigurations = options.contains(Option.DeleteConfigurations);

        BundleInstallSupport.FrameworkInfo info = callback.getInfo();
        int initialBundleStartLevel = info.initialBundleStartLevel;

        // Resolve features from requirements
        Set<String> requiredFeatureIds = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : requirements.entrySet()) {
            for (String req : entry.getValue()) {
                FeatureReq featureReq = FeatureReq.parseRequirement(req);
                if (featureReq != null) {
                    Feature feature = findFeature(featureReq, featuresByName);
                    if (feature != null) {
                        requiredFeatureIds.add(feature.getId());
                    }
                }
            }
        }

        // Flatten features in install order (dependencies first)
        List<Feature> orderedFeatures = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String featureId : requiredFeatureIds) {
            Feature feature = featuresById.get(featureId);
            if (feature != null) {
                collectFeaturesInOrder(feature, featuresById, featuresByName, orderedFeatures, visited);
            }
        }

        // Determine currently installed features
        Set<String> previouslyInstalledIds = new TreeSet<>();
        for (Set<String> ids : state.installedFeatures.values()) {
            previouslyInstalledIds.addAll(ids);
        }

        // Determine new full set of installed features
        Set<String> newInstalledIds = new LinkedHashSet<>();
        for (Feature f : orderedFeatures) {
            newInstalledIds.add(f.getId());
        }

        // Compute changes
        Set<String> toInstallIds = new LinkedHashSet<>(newInstalledIds);
        toInstallIds.removeAll(previouslyInstalledIds);

        Set<String> toUninstallIds = new LinkedHashSet<>(previouslyInstalledIds);
        toUninstallIds.removeAll(newInstalledIds);

        if (toInstallIds.isEmpty() && toUninstallIds.isEmpty()) {
            print("No deployment change.", verbose);
            return;
        }

        if (simulate) {
            if (!toInstallIds.isEmpty()) {
                print("Simulation - features to install:", verbose);
                for (String id : toInstallIds) {
                    print("  " + id, verbose);
                }
            }
            if (!toUninstallIds.isEmpty()) {
                print("Simulation - features to uninstall:", verbose);
                for (String id : toUninstallIds) {
                    print("  " + id, verbose);
                }
            }
            return;
        }

        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);

        Map<String, Set<Long>> managedBundles = new HashMap<>();
        for (Map.Entry<String, Set<Long>> entry : state.managedBundles.entrySet()) {
            managedBundles.put(entry.getKey(), new TreeSet<>(entry.getValue()));
        }
        Map<Long, Long> bundleChecksums = new HashMap<>(state.bundleChecksums);

        // Uninstall features being removed
        if (!toUninstallIds.isEmpty()) {
            uninstallFeatures(toUninstallIds, featuresById, info, managedBundles, verbose);
        }

        // Install new features in order
        if (!toInstallIds.isEmpty()) {
            installFeatures(toInstallIds, featuresById, info, managedBundles, bundleChecksums,
                    noStart, initialBundleStartLevel, verbose);
        }

        // Delete configurations for removed features
        if (deleteConfigurations && !toUninstallIds.isEmpty()) {
            for (String id : toUninstallIds) {
                Feature feature = featuresById.get(id);
                if (feature != null) {
                    try {
                        callback.deleteConfigs(feature);
                    } catch (Exception e) {
                        LOGGER.warn("Error deleting configurations for feature {}", id, e);
                    }
                }
            }
        }

        // Update and save state
        Map<String, Set<String>> installedFeatures = new HashMap<>();
        installedFeatures.put(FeaturesService.ROOT_REGION, new TreeSet<>(newInstalledIds));
        Map<String, Map<String, String>> stateFeatures = new HashMap<>();
        Map<String, String> featureStates = new HashMap<>();
        for (String id : newInstalledIds) {
            featureStates.put(id, noStart ? FeatureState.Installed.name() : FeatureState.Started.name());
        }
        stateFeatures.put(FeaturesService.ROOT_REGION, featureStates);

        State newState = new State();
        newState.bundleChecksums.putAll(bundleChecksums);
        newState.requirements.putAll(requirements);
        newState.installedFeatures.putAll(installedFeatures);
        newState.stateFeatures.putAll(stateFeatures);
        newState.managedBundles.putAll(managedBundles);
        callback.saveState(newState);

        // Fire events
        for (String id : toUninstallIds) {
            Feature feature = featuresById.get(id);
            if (feature != null) {
                callback.callListeners(new FeatureEvent(FeatureEvent.EventType.FeatureUninstalled,
                        feature, FeaturesService.ROOT_REGION, false));
            }
        }
        for (String id : toInstallIds) {
            Feature feature = featuresById.get(id);
            if (feature != null) {
                callback.callListeners(new FeatureEvent(FeatureEvent.EventType.FeatureInstalled,
                        feature, FeaturesService.ROOT_REGION, false));
            }
        }

        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);
        print("Done.", verbose);
    }

    private void installFeatures(Set<String> featureIds,
                                 Map<String, Feature> featuresById,
                                 BundleInstallSupport.FrameworkInfo info,
                                 Map<String, Set<Long>> managedBundles,
                                 Map<Long, Long> bundleChecksums,
                                 boolean noStart,
                                 int initialBundleStartLevel,
                                 boolean verbose) throws Exception {
        Map<String, StreamProvider> providers = downloadManager.getProviders();
        List<Bundle> bundlesToStart = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();

        for (String featureId : featureIds) {
            Feature feature = featuresById.get(featureId);
            if (feature == null) {
                continue;
            }

            print("Installing feature " + feature.getName() + "/" + feature.getVersion(), verbose);

            // Install bundles in declaration order
            for (BundleInfo bundleInfo : feature.getBundles()) {
                if (bundleInfo.isBlacklisted()) {
                    print("  Skipping blacklisted bundle: " + bundleInfo.getLocation(), verbose);
                    continue;
                }

                String location = bundleInfo.getLocation();
                print("  Installing bundle: " + location, verbose);

                // Check if the bundle is already installed
                Bundle existing = findBundleByLocation(location, info.bundles.values());
                if (existing != null) {
                    print("    Bundle already installed: " + existing.getSymbolicName() + "/"
                            + existing.getVersion(), verbose);
                    if (!noStart && bundleInfo.isStart() && existing.getState() != ACTIVE) {
                        bundlesToStart.add(existing);
                    }
                    Set<Long> regionBundles = managedBundles.computeIfAbsent(
                            FeaturesService.ROOT_REGION, k -> new TreeSet<>());
                    regionBundles.add(existing.getBundleId());
                    continue;
                }

                try {
                    Bundle bundle;
                    StreamProvider provider = providers.get(location);
                    if (provider != null) {
                        try (InputStream is = provider.open()) {
                            bundle = callback.installBundle(FeaturesService.ROOT_REGION, location, is);
                        }
                    } else {
                        try (InputStream is = new URL(location).openStream()) {
                            bundle = callback.installBundle(FeaturesService.ROOT_REGION, location, is);
                        }
                    }

                    Set<Long> regionBundles = managedBundles.computeIfAbsent(
                            FeaturesService.ROOT_REGION, k -> new TreeSet<>());
                    regionBundles.add(bundle.getBundleId());

                    // Set start level
                    int startLevel = bundleInfo.getStartLevel() > 0
                            ? bundleInfo.getStartLevel()
                            : initialBundleStartLevel;
                    if (startLevel != initialBundleStartLevel) {
                        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                    }

                    if (!noStart && bundleInfo.isStart()) {
                        bundlesToStart.add(bundle);
                    }

                    print("    Installed: " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                } catch (Exception e) {
                    LOGGER.warn("Error installing bundle {}", location, e);
                    exceptions.add(e);
                }
            }

            // Install configs and libraries
            try {
                callback.installConfigs(feature);
            } catch (Exception e) {
                LOGGER.warn("Error installing configs for feature {}", featureId, e);
            }
            try {
                callback.installLibraries(feature);
            } catch (Exception e) {
                LOGGER.warn("Error installing libraries for feature {}", featureId, e);
            }
        }

        // Start bundles after all are installed
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);

        if (!bundlesToStart.isEmpty()) {
            print("Starting bundles:", verbose);
            for (Bundle bundle : bundlesToStart) {
                if (bundle.getState() != ACTIVE && bundle.getState() != UNINSTALLED
                        && bundle.getHeaders().get("Fragment-Host") == null) {
                    print("  " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                    try {
                        callback.startBundle(bundle);
                    } catch (BundleException e) {
                        exceptions.add(e);
                    }
                }
            }
        }

        if (!exceptions.isEmpty()) {
            throw new MultiException("Error deploying features", exceptions);
        }
    }

    private void uninstallFeatures(Set<String> featureIds,
                                   Map<String, Feature> featuresById,
                                   BundleInstallSupport.FrameworkInfo info,
                                   Map<String, Set<Long>> managedBundles,
                                   boolean verbose) throws Exception {
        // Collect bundles that belong to features being removed but not to features being kept
        Set<String> bundleLocationsToKeep = new HashSet<>();
        for (Map.Entry<String, Feature> entry : featuresById.entrySet()) {
            if (!featureIds.contains(entry.getKey())) {
                for (BundleInfo bi : entry.getValue().getBundles()) {
                    bundleLocationsToKeep.add(bi.getLocation());
                }
            }
        }

        List<Bundle> bundlesToUninstall = new ArrayList<>();
        for (String featureId : featureIds) {
            Feature feature = featuresById.get(featureId);
            if (feature == null) {
                continue;
            }
            for (BundleInfo bundleInfo : feature.getBundles()) {
                if (bundleLocationsToKeep.contains(bundleInfo.getLocation())) {
                    continue;
                }
                Bundle bundle = findBundleByLocation(bundleInfo.getLocation(), info.bundles.values());
                if (bundle != null && !bundlesToUninstall.contains(bundle)) {
                    bundlesToUninstall.add(bundle);
                }
            }
        }

        if (!bundlesToUninstall.isEmpty()) {
            print("Uninstalling bundles:", verbose);
            for (Bundle bundle : bundlesToUninstall) {
                print("  " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                try {
                    callback.uninstall(bundle);
                    // Remove from managed bundles
                    Set<Long> regionBundles = managedBundles.get(FeaturesService.ROOT_REGION);
                    if (regionBundles != null) {
                        regionBundles.remove(bundle.getBundleId());
                    }
                } catch (BundleException e) {
                    LOGGER.warn("Error uninstalling bundle {}", bundle.getSymbolicName(), e);
                }
            }
        }
    }

    private Bundle findBundleByLocation(String location, Collection<Bundle> bundles) {
        for (Bundle bundle : bundles) {
            if (location.equals(bundle.getLocation())) {
                return bundle;
            }
        }
        return null;
    }

    /**
     * Recursively collect features in install order: dependencies first.
     */
    private void collectFeaturesInOrder(Feature feature,
                                        Map<String, Feature> featuresById,
                                        Map<String, Map<String, Feature>> featuresByName,
                                        List<Feature> result,
                                        Set<String> visited) {
        if (visited.contains(feature.getId())) {
            return;
        }
        visited.add(feature.getId());

        // First, process dependencies
        for (Dependency dep : feature.getDependencies()) {
            Feature depFeature = findDependencyFeature(dep, featuresById, featuresByName);
            if (depFeature != null) {
                collectFeaturesInOrder(depFeature, featuresById, featuresByName, result, visited);
            } else {
                LOGGER.warn("Could not find dependency feature: {} version {}", dep.getName(), dep.getVersion());
            }
        }

        // Then add this feature
        result.add(feature);
    }

    private Feature findFeature(FeatureReq featureReq, Map<String, Map<String, Feature>> featuresByName) {
        return featureReq.getMatchingFeatures(featuresByName).findFirst().orElse(null);
    }

    private Feature findDependencyFeature(Dependency dep,
                                          Map<String, Feature> featuresById,
                                          Map<String, Map<String, Feature>> featuresByName) {
        // Try exact match first
        if (dep.hasVersion()) {
            String id = dep.getName() + "/" + dep.getVersion();
            Feature feature = featuresById.get(id);
            if (feature != null) {
                return feature;
            }
        }
        // Fall back to matching by name/version range
        FeatureReq req = dep.hasVersion()
                ? new FeatureReq(dep.getName(), dep.getVersion())
                : new FeatureReq(dep.getName());
        return findFeature(req, featuresByName);
    }

    private void print(String message, boolean verbose) {
        callback.print(message, verbose);
    }

}
