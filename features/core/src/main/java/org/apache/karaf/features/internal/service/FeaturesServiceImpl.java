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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.karaf.features.internal.util.JsonReader;
import org.apache.karaf.features.internal.util.JsonWriter;
import org.apache.karaf.util.ThreadUtils;
import org.apache.karaf.util.bundles.BundleUtils;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION;
import static org.apache.karaf.features.internal.model.Feature.VERSION_SEPARATOR;
import static org.apache.karaf.features.internal.service.StateStorage.toStringStringSetMap;
import static org.apache.karaf.features.internal.util.MapUtils.add;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.apache.karaf.features.internal.util.MapUtils.copy;
import static org.apache.karaf.features.internal.util.MapUtils.remove;

/**
 *
 */
public class FeaturesServiceImpl implements FeaturesService, Deployer.DeployCallback {

    static final String FEATURE_OSGI_REQUIREMENT_PREFIX = "feature:";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);

    /**
     * Our bundle and corresponding bundle context.
     * We use it to check bundle operations affecting our own bundle.
     */
    private final Bundle bundle;
    private final BundleContext bundleContext;

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
    private final ConfigurationAdmin configurationAdmin;
    private final Resolver resolver;
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
     * Service requirements enforcement
     */
    private final String serviceRequirements;

    private final int downloadThreads;

    private final long scheduleDelay;

    private final int scheduleMaxRun;

    private final String blacklisted;

    private final boolean configCfgStore;

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
    private final Map<String, Repository> repositoryCache = new HashMap<>();
    private final ExecutorService executor;
    private Map<String, Map<String, Feature>> featureCache;

    private Map<Thread, ResolverHook> hooks = new ConcurrentHashMap<>();
    private ServiceRegistration<ResolverHookFactory> hookRegistration;

    public FeaturesServiceImpl(Bundle bundle,
                               BundleContext bundleContext,
                               BundleContext systemBundleContext,
                               StateStorage storage,
                               FeatureFinder featureFinder,
                               EventAdminListener eventAdminListener,
                               ConfigurationAdmin configurationAdmin,
                               Resolver resolver,
                               RegionDigraph digraph,
                               String overrides,
                               String featureResolutionRange,
                               String bundleUpdateRange,
                               String updateSnaphots,
                               String serviceRequirements,
                               org.osgi.service.repository.Repository globalRepository,
                               int downloadThreads,
                               long scheduleDelay,
                               int scheduleMaxRun,
                               String blacklisted) {
        this(bundle, bundleContext,systemBundleContext, storage, featureFinder, eventAdminListener, configurationAdmin,
                resolver, digraph, overrides, featureResolutionRange, bundleUpdateRange, updateSnaphots,
                serviceRequirements, globalRepository, downloadThreads, scheduleDelay, scheduleMaxRun, blacklisted,
                FeaturesService.DEFAULT_CONFIG_CFG_STORE);
    }

    public FeaturesServiceImpl(Bundle bundle,
                               BundleContext bundleContext,
                               BundleContext systemBundleContext,
                               StateStorage storage,
                               FeatureFinder featureFinder,
                               EventAdminListener eventAdminListener,
                               ConfigurationAdmin configurationAdmin,
                               Resolver resolver,
                               RegionDigraph digraph,
                               String overrides,
                               String featureResolutionRange,
                               String bundleUpdateRange,
                               String updateSnaphots,
                               String serviceRequirements,
                               org.osgi.service.repository.Repository globalRepository,
                               int downloadThreads,
                               long scheduleDelay,
                               int scheduleMaxRun,
                               String blacklisted,
                               boolean configCfgStore) {
        this.bundle = bundle;
        this.bundleContext = bundleContext;
        this.systemBundleContext = systemBundleContext;
        this.storage = storage;
        this.featureFinder = featureFinder;
        this.eventAdminListener = eventAdminListener;
        this.configurationAdmin = configurationAdmin;
        this.resolver = resolver;
        this.configInstaller = configurationAdmin != null ? new FeatureConfigInstaller(configurationAdmin, configCfgStore) : null;
        this.digraph = digraph;
        this.overrides = overrides;
        this.featureResolutionRange = featureResolutionRange;
        this.bundleUpdateRange = bundleUpdateRange;
        this.updateSnaphots = updateSnaphots;
        this.serviceRequirements = serviceRequirements;
        this.globalRepository = globalRepository;
        this.downloadThreads = downloadThreads > 0 ? downloadThreads : 1;
        this.scheduleDelay = scheduleDelay;
        this.scheduleMaxRun = scheduleMaxRun;
        this.blacklisted = blacklisted;
        this.configCfgStore = configCfgStore;
        this.executor = Executors.newSingleThreadExecutor(ThreadUtils.namedThreadFactory("features"));

        if (systemBundleContext != null) {
            hookRegistration = systemBundleContext.registerService(ResolverHookFactory.class, new ResolverHookFactory() {
                @Override
                public ResolverHook begin(Collection<BundleRevision> triggers) {
                    return hooks.get(Thread.currentThread());
                }
            }, null);
        }

        loadState();
        checkResolve();
    }

    public void stop() {
        this.executor.shutdown();
        if (hookRegistration != null) {
            hookRegistration.unregister();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void checkResolve() {
        if (bundleContext == null) {
            return; // Most certainly in unit tests
        }
        File resolveFile = bundleContext.getDataFile("resolve");
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
            Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
            doProvisionInThread(requestedFeatures, stateChanges, copyState(), options);
        } catch (Exception e) {
            LOGGER.warn("Error updating state", e);
        }
    }

    private void writeResolve(Map<String, Set<String>> requestedFeatures, EnumSet<Option> options) throws IOException {
        File resolveFile = bundleContext.getDataFile("resolve");
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
                if (bundleContext != null) { // For tests, this should never happen at runtime
                    DigraphHelper.saveDigraph(bundleContext, digraph);
                }
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
                Repository repository = new RepositoryImpl(URI.create(uri), blacklisted);
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

    @Override
    public void callListeners(FeatureEvent event) {
        if (eventAdminListener != null) {
            eventAdminListener.featureEvent(event);
        }
        for (FeaturesListener listener : listeners) {
            listener.featureEvent(event);
        }
    }

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
        RepositoryImpl repo = new RepositoryImpl(uri, blacklisted);
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
        // install the features in the repo
        if (install) {
            HashSet<String> features = new HashSet<>();
            for (Feature feature : repository.getFeatures()) {
                features.add(feature.getName() + "/" + feature.getVersion());
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
        Repository repo = getRepository(uri);
        if (repo == null) {
            return;
        }

        Set<Repository> repos;
        Set<String> features;
        synchronized (lock) {
            repos = getRepositoryClosure(repo);
            List<Repository> required = new ArrayList<>();
            for (String r : state.repositories) {
                required.add(repositoryCache.get(r));
            }
            required.remove(repo);
            for (Repository rep : required) {
                repos.removeAll(getRepositoryClosure(rep));
            }
            features = new HashSet<>();
            for (Repository tranRepo : repos) {
                for (Feature f : tranRepo.getFeatures()) {
                    if (isRequired(f)) {
                        features.add(f.getId());
                    }
                }
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
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryRemoved, false));
    }

    private Set<Repository> getRepositoryClosure(Repository repo) throws Exception {
        Set<Repository> closure = new HashSet<>();
        Deque<Repository> remaining = new ArrayDeque<>(Collections.singleton(repo));
        while (!remaining.isEmpty()) {
            Repository rep = remaining.removeFirst();
            if (closure.add(rep)) {
                for (URI uri : rep.getRepositories()) {
                    remaining.add(this.repositoryCache.get(uri.toString()));
                }
            }
        }
        return closure;
    }

    @Override
    public void restoreRepository(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshRepository(URI uri) throws Exception {
        removeRepository(uri, false);
        addRepository(uri, false);
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

    @Override
    public Repository getRepository(URI uri) throws Exception {
        // Make sure the cache is loaded
        getFeatures();
        synchronized (lock) {
            for (Repository repo : this.repositoryCache.values()) {
                if (repo.getURI().equals(uri)) {
                    return repo;
                }
            }
            return null;
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
        Feature[] features = this.getFeatures(name);
        if (features.length < 1) {
            return null;
        } else {
            return features[0];
        }
    }

    @Override
    public Feature getFeature(String name, String version) throws Exception {
        Feature[] features = this.getFeatures(name, version);
        if (features.length < 1) {
            return null;
        } else {
            return features[0];
        }
    }

    @Override
    public Feature[] getFeatures(String nameOrId) throws Exception {
        String[] parts = nameOrId.split("/");
        String name = parts.length > 0 ? parts[0] : nameOrId;
        String version = parts.length > 1 ? parts[1] : null;
        return getFeatures(name, version);
    }

    @Override
    public Feature[] getFeatures(String name, String version) throws Exception {
        List<Feature> features = new ArrayList<>();
        Pattern pattern = Pattern.compile(name);
        for (String featureName : getFeatures().keySet()) {
            Matcher matcher = pattern.matcher(featureName);
            if (matcher.matches()) {
                Map<String, Feature> versions = getFeatures().get(featureName);
                Feature matchingFeature = getFeatureMatching(versions, version);
                if (matchingFeature != null) {
                    features.add(matchingFeature);
                }
            }
        }
        return features.toArray(new Feature[features.size()]);
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

    @Override
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
        Set<String> loaded = new HashSet<>();
        Queue<String> toLoad = new ArrayDeque<>(uris);
        while (!toLoad.isEmpty()) {
            String uri = toLoad.remove();
            Repository repo;
            synchronized (lock) {
                repo = repositoryCache.get(uri);
            }
            try {
                if (repo == null) {
                    RepositoryImpl rep = new RepositoryImpl(URI.create(uri), blacklisted);
                    rep.load();
                    repo = rep;
                    synchronized (lock) {
                        repositoryCache.put(uri, repo);
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
            repos = new ArrayList<>(repositoryCache.values());
        }
        // * then load all features
        for (Repository repo : repos) {
            for (Feature f : repo.getFeatures()) {
                Map<String, Feature> versionMap = map.computeIfAbsent(f.getName(), key -> new HashMap<>());
                versionMap.put(f.getVersion(), f);
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
        String id = FEATURE_OSGI_REQUIREMENT_PREFIX + f.getName() + "/" + new VersionRange(f.getVersion(), true);
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
        installFeature(feature.getId(), options);
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
        Set<String> fl = required.get(region);
        if (fl == null) {
            fl = new HashSet<>();
            required.put(region, fl);
        }
        List<String> featuresToAdd = new ArrayList<>();
        List<String> featuresToRemove = new ArrayList<>();
        for (String feature : features) {
            feature = normalize(feature);
            String name = feature.substring(0, feature.indexOf("/"));
            String version = feature.substring(feature.indexOf("/") + 1);
            Pattern pattern = Pattern.compile(name);
            boolean matched = false;
            for (String fKey : getFeatures().keySet()) {
                Matcher matcher = pattern.matcher(fKey);
                if (matcher.matches()) {
                    Feature f = getFeatureMatching(getFeatures().get(fKey), version);
                    if (f != null) {
                        String req = f.getName() + "/" + new VersionRange(f.getVersion(), true);
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
                    if (existentFeature.startsWith(name + "/")
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
        print("Adding features: " + join(featuresToAdd), options.contains(Option.Verbose));
        for (String feature : featuresToAdd) {
            fl.add(FEATURE_OSGI_REQUIREMENT_PREFIX + feature);
        }
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, options);
    }

    @Override
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
                // Match only on name
                String nameSep = FEATURE_OSGI_REQUIREMENT_PREFIX + feature.substring(0, feature.indexOf("/") + 1);
                for (String f : fl) {
                    Pattern pattern = Pattern.compile(nameSep.substring(0, nameSep.length() - 1));
                    Matcher matcher = pattern.matcher(f);
                    if (matcher.matches() || normalize(f).startsWith(nameSep)) {
                        toRemove.add(f);
                    }
                }
            } else {
                // Match on name and version
                String name = feature.substring(0, feature.indexOf("/"));
                String version = feature.substring(feature.indexOf("/") + 1);
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
        doProvisionInThread(required, stateChanges, state, options);
    }

    @Override
    public void updateFeaturesState(Map<String, Map<String, FeatureState>> stateChanges, EnumSet<Option> options) throws Exception {
        State state = copyState();
        doProvisionInThread(copy(state.requirements), stateChanges, state, options);
    }

    @Override
    public void addRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        add(required, requirements);
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
        doProvisionInThread(required, stateChanges, state, options);
    }

    @Override
    public void removeRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        remove(required, requirements);
        Map<String, Map<String, FeatureState>> stateChanges = Collections.emptyMap();
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
        int idx = feature.indexOf(VERSION_SEPARATOR);
        if (idx < 0) {
            return feature + VERSION_SEPARATOR + DEFAULT_VERSION;
        }
        String name = feature.substring(0, idx);
        String version = feature.substring(idx + 1);
        return name + "/" + VersionCleaner.clean(version);
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
    public void doProvisionInThread(final Map<String, Set<String>> requirements,
                                    final Map<String, Map<String, FeatureState>> stateChanges,
                                    final State state,
                                    final EnumSet<Option> options) throws Exception {
        try {
            final String outputFile = this.outputFile.get();
            this.outputFile.set(null);
            executor.submit(() -> {
                doProvision(requirements, stateChanges, state, options, outputFile);
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

    private Deployer.DeploymentRequest getDeploymentRequest(Map<String, Set<String>> requirements, Map<String, Map<String, FeatureState>> stateChanges, EnumSet<Option> options, String outputFile) {
        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = bundleUpdateRange;
        request.featureResolutionRange = featureResolutionRange;
        request.serviceRequirements = serviceRequirements;
        request.updateSnaphots = updateSnaphots;
        request.globalRepository = globalRepository;
        request.overrides = Overrides.loadOverrides(overrides);
        request.requirements = requirements;
        request.stateChanges = stateChanges;
        request.options = options;
        request.outputFile = outputFile;
        return request;
    }



    public void doProvision(Map<String, Set<String>> requirements,                // all requirements
                            Map<String, Map<String, FeatureState>> stateChanges,  // features state changes
                            State state,                                          // current state
                            EnumSet<Option> options,                              // installation options
                            String outputFile                                     // file to store the resolution or null
    ) throws Exception {

        Dictionary<String, String> props = getMavenConfig();
        MavenResolver resolver = MavenResolvers.createMavenResolver(props, "org.ops4j.pax.url.mvn");
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(downloadThreads);
        executor.setMaximumPoolSize(downloadThreads);
        DownloadManager manager = DownloadManagers.createDownloadManager(resolver, executor, scheduleDelay, scheduleMaxRun);
        try {
            Set<String> prereqs = new HashSet<>();
            while (true) {
                try {
                    Deployer.DeploymentState dstate = getDeploymentState(state);
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

    @Override
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
    public void installFeature(Feature feature) throws IOException, InvalidSyntaxException {
        if (configInstaller != null) {
            configInstaller.installFeatureConfigs(feature);
        }
        // TODO: install libraries
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
    public void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException {
        // We need to wrap the bundle to insert a Bundle-UpdateLocation header
        try {
            File file = BundleUtils.fixBundleWithUpdateLocation(is, uri);
            bundle.update(new FileInputStream(file));
            file.delete();
        } catch (IOException e) {
            throw new BundleException("Unable to update bundle", e);
        }
    }

    @Override
    public void uninstall(Bundle bundle) throws BundleException {
        bundle.uninstall();
    }

    @Override
    public void startBundle(Bundle bundle) throws BundleException {
        if (bundle != this.bundle || bundle.getState() != Bundle.STARTING) {
            bundle.start();
        }
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
    public void resolveBundles(Set<Bundle> bundles, final Map<Resource, List<Wire>> wiring, Map<Resource, Bundle> resToBnd) {
        // Make sure it's only used for us
        final Thread thread = Thread.currentThread();
        // Translate wiring
        final Map<Bundle, Resource> bndToRes = new HashMap<>();
        for (Resource res : resToBnd.keySet()) {
            bndToRes.put(resToBnd.get(res), res);
        }
        // Hook
        final ResolverHook hook = new ResolverHook() {
            @Override
            public void filterResolvable(Collection<BundleRevision> candidates) {
            }
            @Override
            public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
            }
            @Override
            public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
                if (Thread.currentThread() == thread) {
                    // osgi.ee capabilities are provided by the system bundle, so just ignore those
                    if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE
                            .equals(requirement.getNamespace())) {
                        return;
                    }
                    Bundle sourceBundle = requirement.getRevision().getBundle();
                    Resource sourceResource = bndToRes.get(sourceBundle);
                    List<Wire> wires = wiring.get(sourceResource);
                    if (sourceBundle == null || wires == null) {
                        // This could be a bundle external to this resolution which
                        // is being resolve at the same time, so do not interfere
                        return;
                    }
                    Set<Resource> wired = new HashSet<>();
                    // Get a list of allowed wired resources
                    wired.add(sourceResource);
                    for (Wire wire : wires) {
                        wired.add(wire.getProvider());
                        if (HostNamespace.HOST_NAMESPACE.equals(wire.getRequirement().getNamespace())) {
                            for (Wire hostWire : wiring.get(wire.getProvider())) {
                                wired.add(hostWire.getProvider());
                            }
                        }
                    }
                    // Remove candidates that are not allowed
                    for (Iterator<BundleCapability> candIter = candidates.iterator(); candIter.hasNext(); ) {
                        BundleCapability cand = candIter.next();
                        BundleRevision br = cand.getRevision();
                        if ((br.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                            br = br.getWiring().getRequiredWires(null).get(0).getProvider();
                        }
                        Resource res = bndToRes.get(br.getBundle());
                        if (!wired.contains(br) && !wired.contains(res)) {
                            candIter.remove();
                        }
                    }
                }
            }
            @Override
            public void end() {
            }
        };

        hooks.put(thread, hook);
        try {
            FrameworkWiring frameworkWiring = systemBundleContext.getBundle().adapt(FrameworkWiring.class);
            frameworkWiring.resolveBundles(bundles);
        } finally {
            hooks.remove(thread);
        }
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
        // Verify that no other bundles have been installed externally in the mean time
        DigraphHelper.verifyUnmanagedBundles(systemBundleContext, temp);
        // Do replace
        digraph.replace(temp);
    }

    private Pattern getFeaturePattern(String name, String version) {
        String req = FEATURE_OSGI_REQUIREMENT_PREFIX + name + "/" + new VersionRange(version, true);
        req = req.replace("[", "\\[");
        req = req.replace("(", "\\(");
        req = req.replace("]", "\\]");
        req = req.replace(")", "\\)");
        Pattern pattern = Pattern.compile(req);
        return pattern;
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
