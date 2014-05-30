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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.internal.download.simple.SimpleDownloader;
import org.apache.karaf.features.internal.util.JsonReader;
import org.apache.karaf.features.internal.util.JsonWriter;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.service.StateStorage.toStringStringSetMap;
import static org.apache.karaf.features.internal.util.MapUtils.add;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.apache.karaf.features.internal.util.MapUtils.copy;
import static org.apache.karaf.features.internal.util.MapUtils.remove;

/**
 *
 */
public class FeaturesServiceImpl implements FeaturesService, Deployer.DeployCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);

    /**
     * Our bundle.
     * We use it to check bundle operations affecting our own bundle.
     */
    private final Bundle bundle;

    /**
     * The system bundle context.
     * For all bundles related operations, we use the system bundle context
     * to allow this bundle to be stopped and still allow the deployment to
     * take place.
     */
    private final BundleContext systemBundleContext;
    /**
     * Used to load and save the {@link State} of this service.
     */
    private final StateStorage storage;
    private final FeatureFinder featureFinder;
    private final EventAdminListener eventAdminListener;
    private final FeatureConfigInstaller configInstaller;
    private final RegionDigraph digraph;
    private final String overrides;
    /**
     * Range to use when a version is specified on a feature dependency.
     * The default is {@link org.apache.karaf.features.FeaturesService#DEFAULT_FEATURE_RESOLUTION_RANGE}
     */
    private final String featureResolutionRange;
    /**
     * Range to use when verifying if a bundle should be updated or
     * new bundle installed.
     * The default is {@link org.apache.karaf.features.FeaturesService#DEFAULT_BUNDLE_UPDATE_RANGE}
     */
    private final String bundleUpdateRange;
    /**
     * Use CRC to check snapshot bundles and update them if changed.
     * Either:
     * - none : never update snapshots
     * - always : always update snapshots
     * - crc : use CRC to detect changes
     */
    private final String updateSnaphots;

    /**
     * Optional global repository
     */
    private final org.osgi.service.repository.Repository globalRepository;

    private final List<FeaturesListener> listeners = new CopyOnWriteArrayIdentityList<>();

    // Synchronized on lock
    private final Object lock = new Object();
    private final State state = new State();
    private final Map<String, Repository> repositoryCache = new HashMap<>();
    private Map<String, Map<String, Feature>> featureCache;


    public FeaturesServiceImpl(Bundle bundle,
                               BundleContext systemBundleContext,
                               StateStorage storage,
                               FeatureFinder featureFinder,
                               EventAdminListener eventAdminListener,
                               FeatureConfigInstaller configInstaller,
                               RegionDigraph digraph,
                               String overrides,
                               String featureResolutionRange,
                               String bundleUpdateRange,
                               String updateSnaphots,
                               org.osgi.service.repository.Repository globalRepository) {
        this.bundle = bundle;
        this.systemBundleContext = systemBundleContext;
        this.storage = storage;
        this.featureFinder = featureFinder;
        this.eventAdminListener = eventAdminListener;
        this.configInstaller = configInstaller;
        this.digraph = digraph;
        this.overrides = overrides;
        this.featureResolutionRange = featureResolutionRange;
        this.bundleUpdateRange = bundleUpdateRange;
        this.updateSnaphots = updateSnaphots;
        this.globalRepository = globalRepository;
        loadState();
        checkResolve();

    }

    @SuppressWarnings("unchecked")
    private void checkResolve() {
        if (bundle == null) {
            return; // Most certainly in unit tests
        }
        File resolveFile = bundle.getBundleContext().getDataFile("resolve");
        if (!resolveFile.exists()) {
            return;
        }
        Map<String, Object> request;
        try (
                FileInputStream fis = new FileInputStream(resolveFile)
        ) {
            request = (Map<String, Object>) JsonReader.read(fis);
        } catch (IOException e) {
            LOGGER.warn("Error reading resolution request", e);
            return;
        }
        Map<String, Set<String>> requestedFeatures = toStringStringSetMap((Map) request.get("features"));
        Collection<String> opts = (Collection<String>) request.get("options");
        EnumSet<Option> options = EnumSet.noneOf(Option.class);
        for (String opt : opts) {
            options.add(Option.valueOf(opt));
        }
        // Resolve
        try {
            Map<String, Map<String, RequestedState>> stateChanges = Collections.emptyMap();
            doProvisionInThread(requestedFeatures, stateChanges, copyState(), options);
        } catch (Exception e) {
            LOGGER.warn("Error updating state", e);
        }
    }

    private void writeResolve(Map<String, Set<String>> requestedFeatures, EnumSet<Option> options) throws IOException {
        File resolveFile = bundle.getBundleContext().getDataFile("resolve");
        Map<String, Object> request = new HashMap<>();
        List<String> opts = new ArrayList<>();
        for (Option opt : options) {
            opts.add(opt.toString());
        }
        request.put("features", requestedFeatures);
        request.put("options", opts);
        try (
                FileOutputStream fos = new FileOutputStream(resolveFile)
        ) {
            JsonWriter.write(fos, request);
        }
    }

    //
    // State support
    //

    protected void loadState() {
        try {
            synchronized (lock) {
                storage.load(state);
            }
        } catch (IOException e) {
            LOGGER.warn("Error loading FeaturesService state", e);
        }
    }

    protected void saveState() {
        try {
            synchronized (lock) {
                // Make sure we don't store bundle checksums if
                // it has been disabled through configadmin
                // so that we don't keep out-of-date checksums.
                if (!UPDATE_SNAPSHOTS_CRC.equalsIgnoreCase(updateSnaphots)) {
                    state.bundleChecksums.clear();
                }
                storage.save(state);
            }
        } catch (IOException e) {
            LOGGER.warn("Error saving FeaturesService state", e);
        }
    }

    boolean isBootDone() {
        synchronized (lock) {
            return state.bootDone.get();
        }
    }

    void bootDone() {
        synchronized (lock) {
            state.bootDone.set(true);
            saveState();
        }
    }

    //
    // Listeners support
    //

    public void registerListener(FeaturesListener listener) {
        listeners.add(listener);
        try {
            Set<String> repositories = new TreeSet<>();
            Map<String, Set<String>> installedFeatures = new TreeMap<>();
            synchronized (lock) {
                repositories.addAll(state.repositories);
                installedFeatures.putAll(copy(state.installedFeatures));
            }
            for (String uri : repositories) {
                Repository repository = new RepositoryImpl(URI.create(uri));
                listener.repositoryEvent(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, true));
            }
            for (Map.Entry<String, Set<String>> entry : installedFeatures.entrySet()) {
                for (String id : entry.getValue()) {
                    Feature feature = org.apache.karaf.features.internal.model.Feature.valueOf(id);
                    listener.featureEvent(new FeatureEvent(FeatureEvent.EventType.FeatureInstalled, feature, entry.getKey(), true));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error notifying listener about the current state", e);
        }
    }

    public void unregisterListener(FeaturesListener listener) {
        listeners.remove(listener);
    }

    public void callListeners(FeatureEvent event) {
        if (eventAdminListener != null) {
            eventAdminListener.featureEvent(event);
        }
        for (FeaturesListener listener : listeners) {
            listener.featureEvent(event);
        }
    }

    protected void callListeners(RepositoryEvent event) {
        if (eventAdminListener != null) {
            eventAdminListener.repositoryEvent(event);
        }
        for (FeaturesListener listener : listeners) {
            listener.repositoryEvent(event);
        }
    }

    //
    // Feature Finder support
    //

    @Override
    public URI getRepositoryUriFor(String name, String version) {
        return featureFinder.getUriFor(name, version);
    }

    @Override
    public String[] getRepositoryNames() {
        return featureFinder.getNames();
    }


    //
    // Repositories support
    //

    public Repository loadRepository(URI uri) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(uri);
        repo.load(true);
        return repo;
    }

    @Override
    public void validateRepository(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRepository(URI uri) throws Exception {
        addRepository(uri, false);
    }

    @Override
    public void addRepository(URI uri, boolean install) throws Exception {
        if (install) {
            // TODO: implement
            throw new UnsupportedOperationException();
        }
        Repository repository = loadRepository(uri);
        synchronized (lock) {
            // Clean cache
            repositoryCache.put(uri.toString(), repository);
            featureCache = null;
            // Add repo
            if (!state.repositories.add(uri.toString())) {
                return;
            }
            saveState();
        }
        callListeners(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, false));
    }

    @Override
    public void removeRepository(URI uri) throws Exception {
        removeRepository(uri, true);
    }

    @Override
    public void removeRepository(URI uri, boolean uninstall) throws Exception {
        // TODO: check we don't have any feature installed from this repository
        Repository repo;
        synchronized (lock) {
            // Remove repo
            if (!state.repositories.remove(uri.toString())) {
                return;
            }
            // Clean cache
            featureCache = null;
            repo = repositoryCache.get(uri.toString());
            List<String> toRemove = new ArrayList<>();
            toRemove.add(uri.toString());
            while (!toRemove.isEmpty()) {
                Repository rep = repositoryCache.remove(toRemove.remove(0));
                if (rep != null) {
                    for (URI u : rep.getRepositories()) {
                        toRemove.add(u.toString());
                    }
                }
            }
            saveState();
        }
        if (repo == null) {
            repo = new RepositoryImpl(uri);
        }
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryRemoved, false));
    }

    @Override
    public void restoreRepository(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshRepository(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public Repository[] listRepositories() throws Exception {
        // Make sure the cache is loaded
        getFeatures();
        synchronized (lock) {
            return repositoryCache.values().toArray(new Repository[repositoryCache.size()]);
        }
    }

    @Override
    public Repository[] listRequiredRepositories() throws Exception {
        // Make sure the cache is loaded
        getFeatures();
        synchronized (lock) {
            List<Repository> repos = new ArrayList<>();
            for (Map.Entry<String, Repository> entry : repositoryCache.entrySet()) {
                if (state.repositories.contains(entry.getKey())) {
                    repos.add(entry.getValue());
                }
            }
            return repos.toArray(new Repository[repos.size()]);
        }
    }

    @Override
    public Repository getRepository(String name) throws Exception {
        // Make sure the cache is loaded
        getFeatures();
        synchronized (lock) {
            for (Repository repo : this.repositoryCache.values()) {
                if (name.equals(repo.getName())) {
                    return repo;
                }
            }
            return null;
        }
    }

    //
    // Features support
    //

    public Feature getFeature(String name) throws Exception {
        return getFeature(name, null);
    }

    public Feature getFeature(String name, String version) throws Exception {
        Map<String, Feature> versions = getFeatures().get(name);
        return getFeatureMatching(versions, version);
    }

    protected Feature getFeatureMatching(Map<String, Feature> versions, String version) {
        if (version != null) {
            version = version.trim();
            if (version.equals(org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)) {
                version = "";
            }
        } else {
            version = "";
        }
        if (versions == null || versions.isEmpty()) {
            return null;
        } else {
            Feature feature = version.isEmpty() ? null : versions.get(version);
            if (feature == null) {
                // Compute version range. If an version has been given, assume exact range
                VersionRange versionRange = version.isEmpty()
                        ? new VersionRange(Version.emptyVersion)
                        : new VersionRange(version, true, true);
                Version latest = Version.emptyVersion;
                for (String available : versions.keySet()) {
                    Version availableVersion = VersionTable.getVersion(available);
                    if (availableVersion.compareTo(latest) >= 0 && versionRange.contains(availableVersion)) {
                        feature = versions.get(available);
                        latest = availableVersion;
                    }
                }
            }
            return feature;
        }
    }

    public Feature[] listFeatures() throws Exception {
        Set<Feature> features = new HashSet<>();
        for (Map<String, Feature> featureWithDifferentVersion : getFeatures().values()) {
            for (Feature f : featureWithDifferentVersion.values()) {
                features.add(f);
            }
        }
        return features.toArray(new Feature[features.size()]);
    }

    protected Map<String, Map<String, Feature>> getFeatures() throws Exception {
        List<String> uris;
        synchronized (lock) {
            if (featureCache != null) {
                return featureCache;
            }
            uris = new ArrayList<>(state.repositories);
        }
        //the outer map's key is feature name, the inner map's key is feature version
        Map<String, Map<String, Feature>> map = new HashMap<>();
        // Two phase load:
        // * first load dependent repositories
        List<String> toLoad = new ArrayList<>(uris);
        while (!toLoad.isEmpty()) {
            String uri = toLoad.remove(0);
            Repository repo;
            synchronized (lock) {
                repo = repositoryCache.get(uri);
            }
            if (repo == null) {
                RepositoryImpl rep = new RepositoryImpl(URI.create(uri));
                rep.load();
                repo = rep;
                synchronized (lock) {
                    repositoryCache.put(uri, repo);
                }
            }
            for (URI u : repo.getRepositories()) {
                toLoad.add(u.toString());
            }
        }
        List<Repository> repos;
        synchronized (lock) {
            repos = new ArrayList<>(repositoryCache.values());
        }
        // * then load all features
        for (Repository repo : repos) {
            for (Feature f : repo.getFeatures()) {
                if (map.get(f.getName()) == null) {
                    Map<String, Feature> versionMap = new HashMap<>();
                    versionMap.put(f.getVersion(), f);
                    map.put(f.getName(), versionMap);
                } else {
                    map.get(f.getName()).put(f.getVersion(), f);
                }
            }
        }
        synchronized (lock) {
            if (uris.size() == state.repositories.size()
                    && state.repositories.containsAll(uris)) {
                featureCache = map;
            }
        }
        return map;
    }

    //
    // Installed features
    //

    @Override
    public Feature[] listInstalledFeatures() throws Exception {
        Set<Feature> features = new HashSet<>();
        Map<String, Map<String, Feature>> allFeatures = getFeatures();
        synchronized (lock) {
            for (Map<String, Feature> featureWithDifferentVersion : allFeatures.values()) {
                for (Feature f : featureWithDifferentVersion.values()) {
                    if (isInstalled(f)) {
                        features.add(f);
                    }
                }
            }
        }
        return features.toArray(new Feature[features.size()]);
    }

    @Override
    public Feature[] listRequiredFeatures() throws Exception {
        Set<Feature> features = new HashSet<>();
        Map<String, Map<String, Feature>> allFeatures = getFeatures();
        synchronized (lock) {
            for (Map<String, Feature> featureWithDifferentVersion : allFeatures.values()) {
                for (Feature f : featureWithDifferentVersion.values()) {
                    if (isRequired(f)) {
                        features.add(f);
                    }
                }
            }
        }
        return features.toArray(new Feature[features.size()]);
    }


    @Override
    public boolean isInstalled(Feature f) {
        String id = normalize(f.getId());
        synchronized (lock) {
            Set<String> installed = state.installedFeatures.get(ROOT_REGION);
            return installed != null && installed.contains(id);
        }
    }

    @Override
    public boolean isRequired(Feature f) {
        String id = "feature:" + f.getName() + "/" + new VersionRange(f.getVersion(), true);
        synchronized (lock) {
            Set<String> features = state.requirements.get(ROOT_REGION);
            return features != null && features.contains(id);
        }
    }

    //
    // Installation and uninstallation of features
    //

    @Override
    public void installFeature(String name) throws Exception {
        installFeature(name, EnumSet.noneOf(Option.class));
    }

    @Override
    public void installFeature(String name, String version) throws Exception {
        installFeature(version != null ? name + "/" + version : name, EnumSet.noneOf(Option.class));
    }

    @Override
    public void installFeature(String name, EnumSet<Option> options) throws Exception {
        installFeatures(Collections.singleton(name), options);
    }

    @Override
    public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        installFeature(version != null ? name + "/" + version : name, options);
    }

    @Override
    public void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        installFeature(feature.getId());
    }

    @Override
    public void installFeatures(Set<String> features, EnumSet<Option> options) throws Exception {
        installFeatures(features, ROOT_REGION, options);
    }

    @Override
    public void uninstallFeature(String name, String version) throws Exception {
        uninstallFeature(version != null ? name + "/" + version : name);
    }

    @Override
    public void uninstallFeature(String name, String version, EnumSet<Option> options) throws Exception {
        uninstallFeature(version != null ? name + "/" + version : name, options);
    }

    @Override
    public void uninstallFeature(String name) throws Exception {
        uninstallFeature(name, EnumSet.noneOf(Option.class));
    }

    @Override
    public void uninstallFeature(String name, EnumSet<Option> options) throws Exception {
        uninstallFeatures(Collections.singleton(name), options);
    }

    @Override
    public void uninstallFeatures(Set<String> features, EnumSet<Option> options) throws Exception {
        uninstallFeatures(features, ROOT_REGION, options);
    }


    //
    //
    //
    //   RESOLUTION
    //
    //
    //


    @Override
    public void installFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        if (region == null || region.isEmpty()) {
            region = ROOT_REGION;
        }
        List<String> featuresToAdd = new ArrayList<>();
        Map<String, Map<String, Feature>> featuresMap = getFeatures();
        for (String feature : features) {
            feature = normalize(feature);
            String name = feature.substring(0, feature.indexOf("/"));
            String version = feature.substring(feature.indexOf("/") + 1);
            Feature f = getFeatureMatching(featuresMap.get(name), version);
            if (f == null) {
                if (!options.contains(Option.NoFailOnFeatureNotFound)) {
                    throw new IllegalArgumentException("No matching features for " + feature);
                }
            } else {
                String req = f.getName() + "/" + new VersionRange(f.getVersion(), true);
                featuresToAdd.add(req);
            }
        }
        featuresToAdd = new ArrayList<>(new LinkedHashSet<>(featuresToAdd));
        StringBuilder sb = new StringBuilder();
        sb.append("Adding features: ");
        for (int i = 0; i < featuresToAdd.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(featuresToAdd.get(i));
        }
        print(sb.toString(), options.contains(Option.Verbose));
        Set<String> fl = required.get(region);
        if (fl == null) {
            fl = new HashSet<>();
            required.put(region, fl);
        }
        for (String feature : featuresToAdd) {
            fl.add("feature:" + feature);
        }
        Map<String, Map<String, RequestedState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, options);
    }

    public void uninstallFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        if (region == null || region.isEmpty()) {
            region = ROOT_REGION;
        }
        Set<String> fl = required.get(region);
        if (fl == null) {
            fl = new HashSet<>();
            required.put(region, fl);
        }
        List<String> featuresToRemove = new ArrayList<>();
        for (String feature : new HashSet<>(features)) {
            List<String> toRemove = new ArrayList<>();
            feature = normalize(feature);
            if (feature.endsWith("/0.0.0")) {
                String nameSep = "feature:" + feature.substring(0, feature.indexOf("/") + 1);
                for (String f : fl) {
                    if (normalize(f).startsWith(nameSep)) {
                        toRemove.add(f);
                    }
                }
            } else {
                String name = feature.substring(0, feature.indexOf("/"));
                String version = feature.substring(feature.indexOf("/") + 1);
                String req = "feature:" + name + "/" + new VersionRange(version, true);
                toRemove.add(req);
            }
            toRemove.retainAll(fl);
            if (toRemove.isEmpty()) {
                throw new IllegalArgumentException("Feature named '" + feature + "' is not installed");
            } else if (toRemove.size() > 1) {
                String name = feature.substring(0, feature.indexOf("/"));
                StringBuilder sb = new StringBuilder();
                sb.append("Feature named '").append(name).append("' has multiple versions installed (");
                for (int i = 0; i < toRemove.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    String f = toRemove.get(i);
                    String version = f.substring(f.indexOf("/") + 1);
                    sb.append(version);
                }
                sb.append("). Please specify the version to uninstall.");
                throw new IllegalArgumentException(sb.toString());
            }
            featuresToRemove.addAll(toRemove);
        }
        featuresToRemove = new ArrayList<>(new LinkedHashSet<>(featuresToRemove));
        StringBuilder sb = new StringBuilder();
        sb.append("Removing features: ");
        for (int i = 0; i < featuresToRemove.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(featuresToRemove.get(i));
        }
        print(sb.toString(), options.contains(Option.Verbose));
        fl.removeAll(featuresToRemove);
        if (fl.isEmpty()) {
            required.remove(region);
        }
        Map<String, Map<String, RequestedState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, options);
    }

    @Override
    public void updateFeaturesState(Map<String, Map<String, RequestedState>> stateChanges, EnumSet<Option> options) throws Exception {
        State state = copyState();
        doProvisionInThread(copy(state.requirements), stateChanges, state, options);
    }

    @Override
    public void addRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        add(required, requirements);
        Map<String, Map<String, RequestedState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, options);
    }

    @Override
    public void removeRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        remove(required, requirements);
        Map<String, Map<String, RequestedState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, options);
    }

    @Override
    public Map<String, Set<String>> listRequirements() {
        synchronized (lock) {
            return copy(this.state.requirements);
        }
    }

    private State copyState() {
        synchronized (lock) {
            return this.state.copy();
        }
    }

    protected String normalize(String feature) {
        if (!feature.contains("/")) {
            feature += "/0.0.0";
        }
        int idx = feature.indexOf("/");
        String name = feature.substring(0, idx);
        String version = feature.substring(idx + 1);
        return name + "/" + VersionCleaner.clean(version);
    }

    /**
     * Actual deployment needs to be done in a separate thread.
     * The reason is that if the console is refreshed, the current thread which is running
     * the command may be interrupted while waiting for the refresh to be done, leading
     * to bundles not being started after the refresh.
     */
    public void doProvisionInThread(final Map<String, Set<String>> requirements,
                                    final Map<String, Map<String, RequestedState>> stateChanges,
                                    final State state,
                                    final EnumSet<Option> options) throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    doProvision(requirements, stateChanges, state, options);
                    return null;
                }
            }).get();
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw e;
            }
        } finally {
            executor.shutdown();
        }
    }

    protected Deployer.DeploymentState getDeploymentState(State state) throws Exception {
        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        // State
        dstate.state = state;
        // Service bundle
        dstate.serviceBundle = bundle;
        // Start level
        FrameworkStartLevel fsl = systemBundleContext.getBundle().adapt(FrameworkStartLevel.class);
        dstate.initialBundleStartLevel = fsl.getInitialBundleStartLevel();
        dstate.currentStartLevel = fsl.getStartLevel();
        // Bundles
        dstate.bundles = new HashMap<>();
        for (Bundle bundle : systemBundleContext.getBundles()) {
            dstate.bundles.put(bundle.getBundleId(), bundle);
        }
        // Features
        dstate.features = new HashMap<>();
        for (Map<String, Feature> m : getFeatures().values()) {
            for (Feature feature : m.values()) {
                String id = feature.getId();
                dstate.features.put(id, feature);
            }
        }
        // Region -> bundles mapping
        // Region -> policy mapping
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.filtersPerRegion = new HashMap<>();
        RegionDigraph clone = digraph.copy();
        for (Region region : clone.getRegions()) {
            // Get bundles
            dstate.bundlesPerRegion.put(region.getName(), new HashSet<>(region.getBundleIds()));
            // Get policies
            Map<String, Map<String, Set<String>>> edges = new HashMap<>();
            for (RegionDigraph.FilteredRegion fr : clone.getEdges(region)) {
                Map<String, Set<String>> policy = new HashMap<>();
                Map<String, Collection<String>> current = fr.getFilter().getSharingPolicy();
                for (String ns : current.keySet()) {
                    for (String f : current.get(ns)) {
                        addToMapSet(policy, ns, f);
                    }
                }
                edges.put(fr.getRegion().getName(), policy);
            }
            dstate.filtersPerRegion.put(region.getName(), edges);
        }
        // Return
        return dstate;
    }

    private Deployer.DeploymentRequest getDeploymentRequest(Map<String, Set<String>> requirements, Map<String, Map<String, RequestedState>> stateChanges, EnumSet<Option> options) {
        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = bundleUpdateRange;
        request.featureResolutionRange = featureResolutionRange;
        request.globalRepository = globalRepository;
        request.overrides = Overrides.loadOverrides(overrides);
        request.requirements = requirements;
        request.stateChanges = stateChanges;
        request.options = options;
        return request;
    }



    public void doProvision(Map<String, Set<String>> requirements,                 // all requirements
                            Map<String, Map<String, RequestedState>> stateChanges, // features state changes
                            State state,                                           // current state
                            EnumSet<Option> options                                // installation options
    ) throws Exception {

        Set<String> prereqs = new HashSet<>();
        while (true) {
            try {
                Deployer.DeploymentState dstate = getDeploymentState(state);
                Deployer.DeploymentRequest request = getDeploymentRequest(requirements, stateChanges, options);
                new Deployer(new SimpleDownloader(), this).deploy(dstate, request);
                break;
            } catch (Deployer.PartialDeploymentException e) {
                if (!prereqs.containsAll(e.getMissing())) {
                    prereqs.addAll(e.getMissing());
                    state = copyState();
                } else {
                    throw new Exception("Deployment aborted due to loop in missing prerequisites: " + e.getMissing());
                }
            }
        }
    }

    public void print(String message, boolean verbose) {
        LOGGER.info(message);
        if (verbose) {
            System.out.println(message);
        }
    }

    public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        FrameworkWiring fw = systemBundleContext.getBundle().adapt(FrameworkWiring.class);
        fw.refreshBundles(bundles, new FrameworkListener() {
            @Override
            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.ERROR) {
                    LOGGER.error("Framework error", event.getThrowable());
                }
                latch.countDown();
            }
        });
        latch.await();
    }

    @Override
    public void saveState(State state) {
        synchronized (lock) {
            state.repositories.clear();
            state.repositories.addAll(this.state.repositories);
            state.bootDone.set(this.state.bootDone.get());
            this.state.replace(state);
            saveState();
        }
    }

    @Override
    public void persistResolveRequest(Deployer.DeploymentRequest request) throws IOException {
        writeResolve(request.requirements, request.options);
    }

    @Override
    public void installFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
        if (configInstaller != null) {
            configInstaller.installFeatureConfigs(feature);
        }
    }

    @Override
    public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
        if (ROOT_REGION.equals(region)) {
            return digraph.getRegion(region).installBundleAtLocation(uri, is);
        } else {
            return digraph.getRegion(region).installBundle(uri, is);
        }
    }

    @Override
    public void updateBundle(Bundle bundle, InputStream is) throws BundleException {
        bundle.update(is);
    }

    @Override
    public void uninstall(Bundle bundle) throws BundleException {
        bundle.uninstall();
    }

    @Override
    public void startBundle(Bundle bundle) throws BundleException {
        bundle.start();
    }

    @Override
    public void stopBundle(Bundle bundle, int options) throws BundleException {
        bundle.stop(options);
    }

    @Override
    public void setBundleStartLevel(Bundle bundle, int startLevel) {
        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
    }

    @Override
    public void resolveBundles(Set<Bundle> bundles) {
        systemBundleContext.getBundle().adapt(FrameworkWiring.class).resolveBundles(bundles);
    }

    @Override
    public void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies, Map<String, Set<Long>> bundles) throws BundleException, InvalidSyntaxException {
        RegionDigraph temp = digraph.copy();
        // Remove everything
        for (Region region : temp.getRegions()) {
            temp.removeRegion(region);
        }
        // Re-create regions
        for (String name : policies.keySet()) {
            temp.createRegion(name);
        }
        // Dispatch bundles
        for (Map.Entry<String, Set<Long>> entry : bundles.entrySet()) {
            Region region = temp.getRegion(entry.getKey());
            for (long bundleId : entry.getValue()) {
                region.addBundle(bundleId);
            }
        }
        // Add policies
        for (Map.Entry<String, Map<String, Map<String, Set<String>>>> entry1 : policies.entrySet()) {
            Region region1 = temp.getRegion(entry1.getKey());
            for (Map.Entry<String, Map<String, Set<String>>> entry2 : entry1.getValue().entrySet()) {
                Region region2 = temp.getRegion(entry2.getKey());
                RegionFilterBuilder rfb = temp.createRegionFilterBuilder();
                for (Map.Entry<String, Set<String>> entry3 : entry2.getValue().entrySet()) {
                    for (String flt : entry3.getValue()) {
                        rfb.allow(entry3.getKey(), flt);
                    }
                }
                region1.connectRegion(region2, rfb.build());
            }
        }
        digraph.replace(temp);
    }
}
