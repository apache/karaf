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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.DeploymentListener;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.DownloadManagers;
import org.apache.karaf.features.internal.region.DigraphHelper;
import org.apache.karaf.features.internal.service.BundleInstallSupport.FrameworkInfo;
import org.apache.karaf.util.ThreadUtils;
import org.apache.karaf.util.json.JsonReader;
import org.apache.karaf.util.json.JsonWriter;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.eclipse.equinox.region.RegionDigraph;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.service.StateStorage.toStringStringSetMap;
import static org.apache.karaf.features.internal.util.MapUtils.add;
import static org.apache.karaf.features.internal.util.MapUtils.copy;
import static org.apache.karaf.features.internal.util.MapUtils.remove;

/**
 *
 */
public class FeaturesServiceImpl implements FeaturesService, Deployer.DeployCallback {

    private static final String RESOLVE_FILE = "resolve";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);
    private static final String FEATURE_OSGI_REQUIREMENT_PREFIX = "feature:";
    private static final String VERSION_SEPARATOR = "/";

    /**
     * Used to load and save the {@link State} of this service.
     */
    private final StateStorage storage;
    private final FeatureRepoFinder featureFinder;
    private final ConfigurationAdmin configurationAdmin;
    private final Resolver resolver;
    private final BundleInstallSupport installSupport;
    private final FeaturesServiceConfig cfg;
    private final RepositoryCache repositories;

    private final ThreadLocal<String> outputFile = new ThreadLocal<>();

    /**
     * Optional global repository
     */
    private final org.osgi.service.repository.Repository globalRepository;

    private final List<FeaturesListener> listeners = new CopyOnWriteArrayIdentityList<>();
    private final List<DeploymentListener> deploymentListeners = new CopyOnWriteArrayIdentityList<>();
    private DeploymentEvent lastDeploymentEvent = DeploymentEvent.DEPLOYMENT_FINISHED;

    // Synchronized on lock
    private final Object lock = new Object();
    private final State state = new State();

    private final ExecutorService executor;

    //the outer map's key is feature name, the inner map's key is feature version
    private Map<String, Map<String, Feature>> featureCache;


    public FeaturesServiceImpl(StateStorage storage,
                               FeatureRepoFinder featureFinder,
                               ConfigurationAdmin configurationAdmin,
                               Resolver resolver,
                               BundleInstallSupport installSupport,
                               org.osgi.service.repository.Repository globalRepository,
                               FeaturesServiceConfig cfg) {
        this.storage = storage;
        this.featureFinder = featureFinder;
        this.configurationAdmin = configurationAdmin;
        this.resolver = resolver;
        this.installSupport = installSupport;
        this.globalRepository = globalRepository;
        this.repositories = new RepositoryCache(cfg.blacklisted);
        this.cfg = cfg;
        this.executor = Executors.newSingleThreadExecutor(ThreadUtils.namedThreadFactory("features"));
        loadState();
        checkResolve();
    }

    public void stop() {
      this.executor.shutdown();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void checkResolve() {
        File resolveFile = installSupport.getDataFile(RESOLVE_FILE);
        if (resolveFile == null || !resolveFile.exists()) {
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
            Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
            doProvisionInThread(requestedFeatures, stateChanges, copyState(), getFeaturesById(), options);
        } catch (Exception e) {
            LOGGER.warn("Error updating state", e);
        }
    }

    private void writeResolve(Map<String, Set<String>> requestedFeatures, EnumSet<Option> options) throws IOException {
        File resolveFile = installSupport.getDataFile(RESOLVE_FILE);
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
                if (!UPDATE_SNAPSHOTS_CRC.equalsIgnoreCase(cfg.updateSnapshots)) {
                    state.bundleChecksums.clear();
                }
                storage.save(state);
                installSupport.saveDigraph();
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

    @Override
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
                Repository repository = new RepositoryImpl(URI.create(uri), cfg.blacklisted);
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

    @Override
    public void unregisterListener(FeaturesListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void registerListener(DeploymentListener listener) {
        deploymentListeners.add(listener);
        listener.deploymentEvent(lastDeploymentEvent);
    }

    @Override
    public void unregisterListener(DeploymentListener listener) {
        deploymentListeners.remove(listener);
    }

    /*
     * Should never be called while holding a lock as we're calling outside our bundle.
     */
    @Override
    public void callListeners(FeatureEvent event) {
        for (FeaturesListener listener : listeners) {
            listener.featureEvent(event);
        }
    }

    /*
     * Should never be called while holding a lock as we're calling outside our bundle.
     */
    @Override
    public void callListeners(DeploymentEvent event) {
        lastDeploymentEvent = event;
        for (DeploymentListener listener : deploymentListeners) {
            try {
                listener.deploymentEvent(event);
            } catch (Exception e) {
                LOGGER.warn("DeploymentListener {} failed to process event {}", listener, event, e);
            }
        }
    }

    /*
     * Should never be called while holding a lock as we're calling outside our bundle.
     */
    protected void callListeners(RepositoryEvent event) {
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
        Repository repository = repositories.create(uri, true, true);
        synchronized (lock) {
            repositories.addRepository(repository);
            featureCache = null;
            // Add repo
            if (!state.repositories.add(uri.toString())) {
                return;
            }
            saveState();
        }
        callListeners(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, false));
        // install the features in the repo
        if (install) {
            HashSet<String> features = new HashSet<>();
            for (Feature feature : repository.getFeatures()) {
                features.add(feature.getId());
            }
            installFeatures(features, EnumSet.noneOf(FeaturesService.Option.class));
        }
    }

    @Override
    public void removeRepository(URI uri) throws Exception {
        removeRepository(uri, true);
    }

    @Override
    public void removeRepository(URI uri, boolean uninstall) throws Exception {
        // This will also ensure the cache is loaded
        Repository repo = getRepository(uri);
        if (repo == null) {
            return;
        }

        Set<Repository> repos;
        Set<String> features;
        synchronized (lock) {
            repos = repositories.getRepositoryClosure(repo);
            List<Repository> required = new ArrayList<>(Arrays.asList(repositories.listMatchingRepositories(state.repositories)));
            required.remove(repo);
            for (Repository rep : required) {
                repos.removeAll(repositories.getRepositoryClosure(rep));
            }
            features = new HashSet<>();
            for (Repository tranRepo : repos) {
                features.addAll(getRequiredFeatureIds(tranRepo));
            }
        }

        if (!features.isEmpty()) {
            if (uninstall) {
                uninstallFeatures(features, EnumSet.noneOf(Option.class));
            } else {
                throw new IllegalStateException("The following features are required from the repository: " + String.join(", ", features));
            }
        }

        synchronized (lock) {
            // Remove repo
            if (!state.repositories.remove(uri.toString())) {
                return;
            }
            // Clean cache
            featureCache = null;
            repositories.removeRepository(uri);
            saveState();
        }
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryRemoved, false));
    }

    private Set<String> getRequiredFeatureIds(Repository repo) throws Exception {
        synchronized (lock) {
            return Stream.of(repo.getFeatures())
                    .filter(this::isRequired)
                    .map(Feature::getId)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public void restoreRepository(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshRepository(URI uri) throws Exception {
        refreshRepositories(Collections.singleton(uri));
    }

    @Override
    public void refreshRepositories(Set<URI> uris) throws Exception {
        synchronized (lock) {
            for (URI uri : uris) {
                repositories.removeRepository(uri);
            }
            featureCache = null;
        }
    }

    @Override
    public Repository[] listRepositories() throws Exception {
        ensureCacheLoaded();
        synchronized (lock) {
            return repositories.listRepositories();
        }
    }

    @Override
    public Repository[] listRequiredRepositories() throws Exception {
        ensureCacheLoaded();
        synchronized (lock) {
            return repositories.listMatchingRepositories(state.repositories);
        }
    }

    @Override
    public Repository getRepository(String name) throws Exception {
        ensureCacheLoaded();
        synchronized (lock) {
            return repositories.getRepositoryByName(name);
        }
    }

    @Override
    public Repository getRepository(URI uri) throws Exception {
        ensureCacheLoaded();
        synchronized (lock) {
            return repositories.getRepository(uri.toString());
        }
    }

    @Override
    public String getRepositoryName(URI uri) throws Exception {
        Repository repo = getRepository(uri);
        return (repo != null) ? repo.getName() : null;
    }

    //
    // Features support
    //

    @Override
    public Feature getFeature(String name) throws Exception {
        Feature[] features = getFeatures(name);
        if (features.length < 1) {
            return null;
        } else {
            return features[0];
        }
    }

    @Override
    public Feature getFeature(String name, String version) throws Exception {
        Feature[] features = getFeatures(name, version);
        if (features.length < 1) {
            return null;
        } else {
            return features[0];
        }
    }

    @Override
    public Feature[] getFeatures(String nameOrId) throws Exception {
        String[] parts = nameOrId.split(VERSION_SEPARATOR);
        String name = parts.length > 0 ? parts[0] : nameOrId;
        String version = parts.length > 1 ? parts[1] : null;
        return getFeatures(name, version);
    }

    @Override
    public Feature[] getFeatures(String name, String version) throws Exception {
        List<Feature> features = new ArrayList<>();
        Pattern pattern = Pattern.compile(name);
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        for (String featureName : allFeatures.keySet()) {
            Matcher matcher = pattern.matcher(featureName);
            if (matcher.matches()) {
                Map<String, Feature> versions = allFeatures.get(featureName);
                Feature matchingFeature = getFeatureMatching(versions, version);
                if (matchingFeature != null) {
                    features.add(matchingFeature);
                }
            }
        }
        return features.toArray(new Feature[features.size()]);
    }

    private Feature getFeatureMatching(Map<String, Feature> versions, String version) {
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

    @Override
    public Feature[] listFeatures() throws Exception {
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        return flattenFeatures(allFeatures);
    }
    
    private void ensureCacheLoaded() throws Exception {
        getFeatureCache();
    }

    /**
     * Should not be called while holding a lock.
     */
    protected Map<String, Map<String, Feature>> getFeatureCache() throws Exception {
        Set<String> uris;
        synchronized (lock) {
            if (featureCache != null) {
                return featureCache;
            }
            uris = new TreeSet<>(state.repositories);
        }
        //the outer map's key is feature name, the inner map's key is feature version
        Map<String, Map<String, Feature>> map = new HashMap<>();
        // Two phase load:
        // * first load dependent repositories
        Set<String> loaded = new HashSet<>();
        List<String> toLoad = new ArrayList<>(uris);
        while (!toLoad.isEmpty()) {
            String uri = toLoad.remove(0);
            Repository repo;
            synchronized (lock) {
                repo = repositories.getRepository(uri);
            }
            try {
                if (repo == null) {
                    repo = repositories.create(URI.create(uri), true, false);
                    synchronized (lock) {
                        repositories.addRepository(repo);
                    }
                }
                if (loaded.add(uri)) {
                    for (URI u : repo.getRepositories()) {
                        toLoad.add(u.toString());
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Can't load features repository {}", uri, e);
            }
        }
        List<Repository> repos;
        synchronized (lock) {
            repos = Arrays.asList(repositories.listRepositories());
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
            if (uris.equals(state.repositories)) {
                featureCache = map;
            }
        }
        return map;
    }

    protected Map<String, Feature> getFeaturesById() throws Exception {
        return getFeatureCache().values().stream().flatMap(m -> m.values().stream())
                .collect(Collectors.toMap(Feature::getId, Function.identity()));
    }

   //
    // Installed features
    //

    @Override
    public Feature[] listInstalledFeatures() throws Exception {
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        synchronized (lock) {
            return flattenFeatures(allFeatures, this::isInstalled);
        }
    }

    @Override
    public Feature[] listRequiredFeatures() throws Exception {
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        synchronized (lock) {
            return flattenFeatures(allFeatures, this::isRequired);
        }
    }

    private Feature[] flattenFeatures(Map<String, Map<String, Feature>> features) {
        return flattenFeatures(features, f -> true /* include all */);
    }

    private Feature[] flattenFeatures(Map<String, Map<String, Feature>> features, Predicate<Feature> pred) {
        return features.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .filter(pred)
                .toArray(Feature[]::new);
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
    public FeatureState getState(String featureId) {
        String id = normalize(featureId);
        synchronized (lock) {
            Set<String> installed = state.installedFeatures.get(ROOT_REGION);
            if (!installed.contains(id)) {
                return FeatureState.Uninstalled;
            } else {
                String stateSt = state.stateFeatures.get(ROOT_REGION).get(id);
                return FeatureState.valueOf(stateSt);
            }
        }
    }

    @Override
    public boolean isRequired(Feature f) {
        String id = FEATURE_OSGI_REQUIREMENT_PREFIX + getFeatureRequirement(f);
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
        installFeature(getId(name, version), EnumSet.noneOf(Option.class));
    }

    @Override
    public void installFeature(String name, EnumSet<Option> options) throws Exception {
        installFeatures(Collections.singleton(name), options);
    }

    @Override
    public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        installFeature(getId(name, version), options);
    }

    @Override
    public void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        installFeature(feature.getId(), options);
    }

    @Override
    public void installFeatures(Set<String> features, EnumSet<Option> options) throws Exception {
        installFeatures(features, ROOT_REGION, options);
    }

    @Override
    public void uninstallFeature(String name, String version) throws Exception {
        uninstallFeature(getId(name, version));
    }

    @Override
    public void uninstallFeature(String name, String version, EnumSet<Option> options) throws Exception {
        uninstallFeature(getId(name, version), options);
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

    private String getId(String name, String version) {
        return version != null ? name + VERSION_SEPARATOR + version : name;
    }


    //
    //
    //
    //   RESOLUTION
    //
    //
    //


    @Override
    public void setResolutionOutputFile(String outputFile) {
        this.outputFile.set(outputFile);
    }

    @Override
    public void installFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        if (region == null || region.isEmpty()) {
            region = ROOT_REGION;
        }
        Set<String> fl = required.computeIfAbsent(region, k -> new HashSet<>());
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        List<String> featuresToAdd = new ArrayList<>();
        List<String> featuresToRemove = new ArrayList<>();
        for (String feature : features) {
            if (!feature.contains(VERSION_SEPARATOR)) {
                feature += "/0.0.0";
            }
            String name = feature.substring(0, feature.indexOf(VERSION_SEPARATOR));
            String version = feature.substring(feature.indexOf(VERSION_SEPARATOR) + 1);
            Pattern pattern = Pattern.compile(name);
            boolean matched = false;
            for (String fKey : allFeatures.keySet()) {
                Matcher matcher = pattern.matcher(fKey);
                if (matcher.matches()) {
                    Feature f = getFeatureMatching(allFeatures.get(fKey), version);
                    if (f != null) {
                        String req = getFeatureRequirement(f);
                        featuresToAdd.add(req);
                        Feature[] installedFeatures = listInstalledFeatures();
                        for (Feature installedFeature : installedFeatures) {
                            if (installedFeature.getName().equals(f.getName()) && installedFeature.getVersion().equals(f.getVersion())) {
                                LOGGER.info("The specified feature: '{}' version '{}' {}",f.getName(),f.getVersion(),f.getVersion().endsWith("SNAPSHOT") ? "has been upgraded": "is already installed");
                            }
                        }
                        matched = true;
                    }
                }
            }
            if (!matched && !options.contains(Option.NoFailOnFeatureNotFound)) {
                throw new IllegalArgumentException("No matching features for " + feature);
            }
            if (options.contains(Option.Upgrade)) {
                for (String existentFeatureReq : fl) {
                    //remove requirement prefix feature:
                    String existentFeature = existentFeatureReq.substring(FEATURE_OSGI_REQUIREMENT_PREFIX.length());
                    if (existentFeature.startsWith(name + VERSION_SEPARATOR)
                            && !featuresToAdd.contains(existentFeature)) {
                        featuresToRemove.add(existentFeature);
                        //do not break cycle to remove all old versions of feature
                    }
                }
            }
        }
        if (!featuresToRemove.isEmpty()) {
            print("Removing features: " + join(featuresToRemove), options.contains(Option.Verbose));
            for (String featureReq : featuresToRemove) {
                fl.remove(FEATURE_OSGI_REQUIREMENT_PREFIX + featureReq);
            }
        }
        featuresToAdd = new ArrayList<>(new LinkedHashSet<>(featuresToAdd));
        List<String> featuresToDisplay = new ArrayList<>();
        for (String feature : featuresToAdd) {
            fl.add(FEATURE_OSGI_REQUIREMENT_PREFIX + feature);
            String v = feature.substring(feature.indexOf(VERSION_SEPARATOR) + VERSION_SEPARATOR.length());
            VersionRange vr = new VersionRange(v, true);
            if (vr.isPointVersion()) {
                v = feature.substring(0, feature.indexOf(VERSION_SEPARATOR) + VERSION_SEPARATOR.length())
                        + vr.getCeiling().toString();
            }
            featuresToDisplay.add(v);
        }
        print("Adding features: " + join(featuresToDisplay), options.contains(Option.Verbose));
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, getFeaturesById(), options);
    }

    @Override
    public void uninstallFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        if (region == null || region.isEmpty()) {
            region = ROOT_REGION;
        }
        Set<String> fl = required.computeIfAbsent(region, k -> new HashSet<>());
        List<String> featuresToRemove = new ArrayList<>();
        for (String feature : new HashSet<>(features)) {
            List<String> toRemove = new ArrayList<>();
            feature = normalize(feature);
            if (feature.endsWith("/0.0.0")) {
                // Match only on name
                String nameSep = FEATURE_OSGI_REQUIREMENT_PREFIX + feature.substring(0, feature.indexOf(VERSION_SEPARATOR) + 1);
                for (String f : fl) {
                    Pattern pattern = Pattern.compile(nameSep.substring(0, nameSep.length() - 1));
                    Matcher matcher = pattern.matcher(f);
                    if (matcher.matches() || normalize(f).startsWith(nameSep)) {
                        toRemove.add(f);
                    }
                }
            } else {
                // Match on name and version
                String name = feature.substring(0, feature.indexOf(VERSION_SEPARATOR));
                String version = feature.substring(feature.indexOf(VERSION_SEPARATOR) + 1);
                Pattern pattern = getFeaturePattern(name, version);
                for (String f : fl) {
                    Matcher matcher = pattern.matcher(f);
                    if (matcher.matches()) {
                        toRemove.add(f);
                    }
                }
            }
            toRemove.retainAll(fl);

            if (toRemove.isEmpty()) {
                throw new IllegalArgumentException("Feature named '" + feature + "' is not installed");
            }
            featuresToRemove.addAll(toRemove);
        }
        featuresToRemove = new ArrayList<>(new LinkedHashSet<>(featuresToRemove));
        print("Removing features: " + join(featuresToRemove), options.contains(Option.Verbose));
        fl.removeAll(featuresToRemove);
        if (fl.isEmpty()) {
            required.remove(region);
        }
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, getFeaturesById(), options);
    }

    @Override
    public void updateFeaturesState(Map<String, Map<String, FeatureState>> stateChanges, EnumSet<Option> options) throws Exception {
        State state = copyState();
        doProvisionInThread(copy(state.requirements), stateChanges, state, getFeaturesById(), options);
    }

    @Override
    public void addRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        add(required, requirements);
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, getFeaturesById(), options);
    }

    @Override
    public void removeRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        remove(required, requirements);
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, getFeaturesById(), options);
    }

    @Override
    public void updateReposAndRequirements(Set<URI> repos, Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State stateCopy;
        synchronized (lock) {
            // Remove repo
            Set<String> reps = repos.stream().map(URI::toString).collect(Collectors.toSet());
            Set<String> toRemove = diff(state.repositories, reps);
            Set<String> toAdd = diff(reps, state.repositories);
            state.repositories.removeAll(toRemove);
            state.repositories.addAll(toAdd);
            featureCache = null;
            for (String uri : toRemove) {
                repositories.removeRepository(URI.create(uri));
            }
            for (String uri : toAdd) {
                repositories.addRepository(createRepository(URI.create(uri)));
            }
            saveState();
            stateCopy = state.copy();
        }
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(requirements, stateChanges, stateCopy, getFeaturesById(), options);
    }

    private <T> Set<T> diff(Set<T> s1, Set<T> s2) {
        Set<T> s = new HashSet<>(s1);
        s.removeAll(s2);
        return s;
    }

    @Override
    public Repository createRepository(URI uri) throws Exception {
        return repositories.create(uri, true, true);
    }

    private Map<String, Feature> loadAllFeatures(Set<URI> uris) throws Exception {
        //the outer map's key is feature name, the inner map's key is feature version
        Map<String, Feature> map = new HashMap<>();
        // Two phase load:
        // * first load dependent repositories
        Set<URI> loaded = new HashSet<>();
        List<URI> toLoad = new ArrayList<>(uris);
        Clause[] blacklisted = repositories.getBlacklisted();
        while (!toLoad.isEmpty()) {
            URI uri = toLoad.remove(0);
            if (loaded.add(uri)) {
                Repository repo = new RepositoryImpl(uri, blacklisted);
                Collections.addAll(toLoad, repo.getRepositories());
                for (Feature f : repo.getFeatures()) {
                    map.put(f.getId(), f);
                }
            }
        }
        return map;
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

    private String normalize(String feature) {
        if (!feature.contains(VERSION_SEPARATOR)) {
            feature += "/0.0.0";
        }
        int idx = feature.indexOf(VERSION_SEPARATOR);
        String name = feature.substring(0, idx);
        String version = feature.substring(idx + 1);
        return name + VERSION_SEPARATOR + VersionCleaner.clean(version);
    }

    /**
     * Actual deployment needs to be done in a separate thread.
     * The reason is that if the console is refreshed, the current thread which is running
     * the command may be interrupted while waiting for the refresh to be done, leading
     * to bundles not being started after the refresh.
     *
     * @param requirements the provided requirements to match.
     * @param stateChanges the current features state.
     * @param state the current provisioning state.
     * @param options the provisioning options.
     * @throws Exception in case of provisioning failure.
     */
    private void doProvisionInThread(final Map<String, Set<String>> requirements,
                                    final Map<String, Map<String, FeatureState>> stateChanges,
                                    final State state,
                                    final Map<String, Feature> featureById,
                                    final EnumSet<Option> options) throws Exception {
        try {
            final String outputFile = this.outputFile.get();
            this.outputFile.set(null);
            executor.submit(() -> {
                doProvision(requirements, stateChanges, state, featureById, options, outputFile);
                return null;
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
        }
    }

    private Deployer.DeploymentState getDeploymentState(State state, Map<String, Feature> featuresById) throws Exception {
        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = state;
        FrameworkInfo info = installSupport.getInfo();
        dstate.serviceBundle = info.ourBundle;
        dstate.initialBundleStartLevel = info.initialBundleStartLevel;
        dstate.currentStartLevel = info.currentStartLevel;
        dstate.bundles = info.bundles;
        // Features
        dstate.features = featuresById;
        RegionDigraph regionDigraph = installSupport.getDiGraphCopy();
        dstate.bundlesPerRegion = DigraphHelper.getBundlesPerRegion(regionDigraph);
        dstate.filtersPerRegion = DigraphHelper.getPolicies(regionDigraph);
        return dstate;
    }

    private Deployer.DeploymentRequest getDeploymentRequest(Map<String, Set<String>> requirements, Map<String, Map<String, FeatureState>> stateChanges, EnumSet<Option> options, String outputFile) {
        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = cfg.bundleUpdateRange;
        request.featureResolutionRange = cfg.featureResolutionRange;
        request.serviceRequirements = cfg.serviceRequirements;
        request.updateSnaphots = cfg.updateSnapshots;
        request.globalRepository = globalRepository;
        request.overrides = Overrides.loadOverrides(cfg.overrides);
        request.requirements = requirements;
        request.stateChanges = stateChanges;
        request.options = options;
        request.outputFile = outputFile;
        return request;
    }

    private void doProvision(Map<String, Set<String>> requirements,                // all requirements
                             Map<String, Map<String, FeatureState>> stateChanges,  // features state changes
                             State state,                                          // current state
                             Map<String, Feature> featuresById,                    // features by id
                             EnumSet<Option> options,                              // installation options
                             String outputFile                                     // file to store the resolution or null
    ) throws Exception {

        Dictionary<String, String> props = getMavenConfig();
        MavenResolver resolver = MavenResolvers.createMavenResolver(props, "org.ops4j.pax.url.mvn");
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(cfg.downloadThreads);
        executor.setMaximumPoolSize(cfg.downloadThreads);
        DownloadManager manager = DownloadManagers.createDownloadManager(resolver, executor, cfg.scheduleDelay, cfg.scheduleMaxRun);
        try {
            Set<String> prereqs = new HashSet<>();
            while (true) {
                try {
                    Deployer.DeploymentState dstate = getDeploymentState(state, featuresById);
                    Deployer.DeploymentRequest request = getDeploymentRequest(requirements, stateChanges, options, outputFile);
                    new Deployer(manager, this.resolver, this).deploy(dstate, request);
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
        } finally {
            executor.shutdown();
        }
    }

    private Dictionary<String, String> getMavenConfig() throws IOException {
        Hashtable<String, String> props = new Hashtable<>();
        if (configurationAdmin != null) {
            Configuration config = configurationAdmin.getConfiguration("org.ops4j.pax.url.mvn", null);
            if (config != null) {
                Dictionary<String, Object> cfg = config.getProperties();
                if (cfg != null) {
                    for (Enumeration<String> e = cfg.keys(); e.hasMoreElements(); ) {
                        String key = e.nextElement();
                        Object val = cfg.get(key);
                        if (key != null) {
                            props.put(key, val.toString());
                        }
                    }
                }
            }
        }
        return props;
    }

    @Override
    public void print(String message, boolean verbose) {
        LOGGER.info(message);
        if (verbose) {
            System.out.println(message);
        }
    }

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
    public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
        installSupport.refreshPackages(bundles);
    }

    @Override
    public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
        return installSupport.installBundle(region, uri, is);
    }

    @Override
    public void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException {
        installSupport.updateBundle(bundle, uri, is);
    }

    @Override
    public void uninstall(Bundle bundle) throws BundleException {
        installSupport.uninstall(bundle);
    }

    @Override
    public void startBundle(Bundle bundle) throws BundleException {
        installSupport.startBundle(bundle);
    }

    @Override
    public void stopBundle(Bundle bundle, int options) throws BundleException {
        installSupport.stopBundle(bundle, options);
    }

    @Override
    public void setBundleStartLevel(Bundle bundle, int startLevel) {
        installSupport.setBundleStartLevel(bundle, startLevel);
    }

    @Override
    public void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring, Map<Resource, Bundle> resToBnd) {
        installSupport.resolveBundles(bundles, wiring, resToBnd);
    }

    @Override
    public void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies, Map<String, Set<Long>> bundles) throws BundleException, InvalidSyntaxException {
        installSupport.replaceDigraph(policies, bundles);
    }

    @Override
    public void installConfigs(Feature feature) throws IOException, InvalidSyntaxException {
        installSupport.installConfigs(feature);
    }

    @Override
    public void installLibraries(Feature feature) throws IOException {
        installSupport.installLibraries(feature);
    }

    private Pattern getFeaturePattern(String name, String version) {
        String req = FEATURE_OSGI_REQUIREMENT_PREFIX + getFeatureRequirement(name, version);
        req = req.replace("[", "\\[");
        req = req.replace("(", "\\(");
        req = req.replace("]", "\\]");
        req = req.replace(")", "\\)");
        return Pattern.compile(req);
    }

    private String getFeatureRequirement(Feature feature) {
        return getFeatureRequirement(feature.getName(), feature.getVersion());
    }

    private String getFeatureRequirement(String name, String version) {
        return name + VERSION_SEPARATOR + new VersionRange(version, true);
    }

    private String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
