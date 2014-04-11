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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.internal.deployment.DeploymentBuilder;
import org.apache.karaf.features.internal.deployment.StreamProvider;
import org.apache.karaf.features.internal.resolver.FeatureNamespace;
import org.apache.karaf.features.internal.resolver.UriNamespace;
import org.apache.karaf.features.internal.util.ChecksumUtils;
import org.apache.karaf.features.internal.util.Macro;
import org.apache.karaf.features.internal.util.MultiException;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.felix.resolver.Util.getSymbolicName;
import static org.apache.felix.resolver.Util.getVersion;

/**
 *
 */
public class FeaturesServiceImpl implements FeaturesService {

    public static final String UPDATE_SNAPSHOTS_NONE = "none";
    public static final String UPDATE_SNAPSHOTS_CRC = "crc";
    public static final String UPDATE_SNAPSHOTS_ALWAYS = "always";
    public static final String DEFAULT_UPDATE_SNAPSHOTS = UPDATE_SNAPSHOTS_CRC;

    public static final String DEFAULT_FEATURE_RESOLUTION_RANGE = "${range;[====,====]}";
    public static final String DEFAULT_BUNDLE_UPDATE_RANGE = "${range;[==,=+)}";

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);
    private static final String SNAPSHOT = "SNAPSHOT";
    private static final String MAVEN = "mvn:";

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
    private final String overrides;
    /**
     * Range to use when a version is specified on a feature dependency.
     * The default is {@link FeaturesServiceImpl#DEFAULT_FEATURE_RESOLUTION_RANGE}
     */
    private final String featureResolutionRange;
    /**
     * Range to use when verifying if a bundle should be updated or
     * new bundle installed.
     * The default is {@link FeaturesServiceImpl#DEFAULT_BUNDLE_UPDATE_RANGE}
     */
    private final String bundleUpdateRange;
    /**
     * Use CRC to check snapshot bundles and update them if changed.
     * Either:
     *   - none : never update snapshots
     *   - always : always update snapshots
     *   - crc : use CRC to detect changes
     */
    private final String updateSnaphots;

    private final List<FeaturesListener> listeners = new CopyOnWriteArrayIdentityList<FeaturesListener>();

    // Synchronized on lock
    private final Object lock = new Object();
    private final State state = new State();
    private final Map<String, Repository> repositoryCache = new HashMap<String, Repository>();
    private Map<String, Map<String, Feature>> featureCache;


    public FeaturesServiceImpl(Bundle bundle,
                               BundleContext systemBundleContext,
                               StateStorage storage,
                               FeatureFinder featureFinder,
                               EventAdminListener eventAdminListener,
                               FeatureConfigInstaller configInstaller,
                               String overrides,
                               String featureResolutionRange,
                               String bundleUpdateRange,
                               String updateSnaphots) {
        this.bundle = bundle;
        this.systemBundleContext = systemBundleContext;
        this.storage = storage;
        this.featureFinder = featureFinder;
        this.eventAdminListener = eventAdminListener;
        this.configInstaller = configInstaller;
        this.overrides = overrides;
        this.featureResolutionRange = featureResolutionRange;
        this.bundleUpdateRange = bundleUpdateRange;
        this.updateSnaphots = updateSnaphots;
        loadState();
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
            Set<String> repositories = new TreeSet<String>();
            Set<String> installedFeatures = new TreeSet<String>();
            synchronized (lock) {
                repositories.addAll(state.repositories);
                installedFeatures.addAll(state.installedFeatures);
            }
            for (String uri : repositories) {
                Repository repository = new RepositoryImpl(URI.create(uri));
                listener.repositoryEvent(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, true));
            }
            for (String id : installedFeatures) {
                Feature feature = org.apache.karaf.features.internal.model.Feature.valueOf(id);
                listener.featureEvent(new FeatureEvent(feature, FeatureEvent.EventType.FeatureInstalled, true));
            }
        } catch (Exception e) {
            LOGGER.error("Error notifying listener about the current state", e);
        }
    }

    public void unregisterListener(FeaturesListener listener) {
        listeners.remove(listener);
    }

    protected void callListeners(FeatureEvent event) {
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
            List<String> toRemove = new ArrayList<String>();
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
            List<Repository> repos = new ArrayList<Repository>();
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
                VersionRange versionRange = version.isEmpty() ?
                        new VersionRange(Version.emptyVersion) :
                        new VersionRange(version, true, true);
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
        Set<Feature> features = new HashSet<Feature>();
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
            uris = new ArrayList<String>(state.repositories);
        }
        //the outer map's key is feature name, the inner map's key is feature version
        Map<String, Map<String, Feature>> map = new HashMap<String, Map<String, Feature>>();
        // Two phase load:
        // * first load dependent repositories
        List<String> toLoad = new ArrayList<String>(uris);
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
            repos = new ArrayList<Repository>(repositoryCache.values());
        }
        // * then load all features
        for (Repository repo : repos) {
            for (Feature f : repo.getFeatures()) {
                if (map.get(f.getName()) == null) {
                    Map<String, Feature> versionMap = new HashMap<String, Feature>();
                    versionMap.put(f.getVersion(), f);
                    map.put(f.getName(), versionMap);
                } else {
                    map.get(f.getName()).put(f.getVersion(), f);
                }
            }
        }
        synchronized (lock) {
            if (uris.size() == state.repositories.size() &&
                    state.repositories.containsAll(uris)) {
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
        Set<Feature> features = new HashSet<Feature>();
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
        Set<Feature> features = new HashSet<Feature>();
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
            return state.installedFeatures.contains(id);
        }
    }

    @Override
    public boolean isRequired(Feature f) {
        String id = normalize(f.getId());
        synchronized (lock) {
            return state.features.contains(id);
        }
    }

    //
    // Installation and uninstallation of features
    //

    public void installFeature(String name) throws Exception {
        installFeature(name, EnumSet.noneOf(Option.class));
    }

    public void installFeature(String name, String version) throws Exception {
        installFeature(version != null ? name + "/" + version : name, EnumSet.noneOf(Option.class));
    }

    public void installFeature(String name, EnumSet<Option> options) throws Exception {
        installFeatures(Collections.singleton(name), options);
    }

    public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        installFeature(version != null ? name + "/" + version : name, options);
    }

    public void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        installFeature(feature.getId());
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


    //
    //
    //
    //   RESOLUTION
    //
    //
    //






    public void installFeatures(Set<String> features, EnumSet<Option> options) throws Exception {
        Set<String> required;
        Set<String> installed;
        Set<Long> managed;
        synchronized (lock) {
            required = new HashSet<String>(state.features);
            installed = new HashSet<String>(state.installedFeatures);
            managed = new HashSet<Long>(state.managedBundles);
        }
        List<String> featuresToAdd = new ArrayList<String>();
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
                featuresToAdd.add(normalize(f.getId()));
            }
        }
        featuresToAdd = new ArrayList<String>(new LinkedHashSet<String>(featuresToAdd));
        StringBuilder sb = new StringBuilder();
        sb.append("Adding features: ");
        for (int i = 0; i < featuresToAdd.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(featuresToAdd.get(i));
        }
        print(sb.toString(), options.contains(Option.Verbose));
        required.addAll(featuresToAdd);
        doInstallFeaturesInThread(required, installed, managed, options);
    }

    public void uninstallFeatures(Set<String> features, EnumSet<Option> options) throws Exception {
        Set<String> required;
        Set<String> installed;
        Set<Long> managed;
        synchronized (lock) {
            required = new HashSet<String>(state.features);
            installed = new HashSet<String>(state.installedFeatures);
            managed = new HashSet<Long>(state.managedBundles);
        }
        List<String> featuresToRemove = new ArrayList<String>();
        for (String feature : new HashSet<String>(features)) {
            List<String> toRemove = new ArrayList<String>();
            feature = normalize(feature);
            if (feature.endsWith("/0.0.0")) {
                String nameSep = feature.substring(0, feature.indexOf("/") + 1);
                for (String f : required) {
                    if (normalize(f).startsWith(nameSep)) {
                        toRemove.add(f);
                    }
                }
            } else {
                toRemove.add(feature);
            }
            toRemove.retainAll(required);
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
                    sb.append(toRemove.get(i));
                }
                sb.append("). Please specify the version to uninstall.");
                throw new IllegalArgumentException(sb.toString());
            }
            featuresToRemove.addAll(toRemove);
        }
        featuresToRemove = new ArrayList<String>(new LinkedHashSet<String>(featuresToRemove));
        StringBuilder sb = new StringBuilder();
        sb.append("Removing features: ");
        for (int i = 0; i < featuresToRemove.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(featuresToRemove.get(i));
        }
        print(sb.toString(), options.contains(Option.Verbose));
        required.removeAll(featuresToRemove);
        doInstallFeaturesInThread(required, installed, managed, options);
    }

    protected String normalize(String feature) {
        if (!feature.contains("/")) {
            feature += "/0.0.0";
        }
        int idx = feature.indexOf("/");
        String name = feature.substring(0, idx);
        String version = feature.substring(idx + 1);
        return name + "/" + VersionTable.getVersion(version).toString();
    }

    /**
     * Actual deployment needs to be done in a separate thread.
     * The reason is that if the console is refreshed, the current thread which is running
     * the command may be interrupted while waiting for the refresh to be done, leading
     * to bundles not being started after the refresh.
     */
    public void doInstallFeaturesInThread(final Set<String> features,
                                          final Set<String> installed,
                                          final Set<Long> managed,
                                          final EnumSet<Option> options) throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    doInstallFeatures(features, installed, managed, options);
                    return null;
                }
            }).get();
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw ((RuntimeException) t);
            } else if (t instanceof Error) {
                throw ((Error) t);
            } else if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw e;
            }
        } finally {
            executor.shutdown();
        }
    }

    public void doInstallFeatures(Set<String> features,    // all request features
                                  Set<String> installed,   // installed features
                                  Set<Long> managed,       // currently managed bundles
                                  EnumSet<Option> options  // installation options
                    ) throws Exception {

        boolean noRefreshUnmanaged = options.contains(Option.NoAutoRefreshUnmanagedBundles);
        boolean noRefreshManaged = options.contains(Option.NoAutoRefreshManagedBundles);
        boolean noRefresh = options.contains(Option.NoAutoRefreshBundles);
        boolean noStart = options.contains(Option.NoAutoStartBundles);
        boolean verbose = options.contains(Option.Verbose);
        boolean simulate = options.contains(Option.Simulate);

        // Get a list of resolved and unmanaged bundles to use as capabilities during resolution
        List<Resource> systemBundles = new ArrayList<Resource>();
        Bundle[] bundles = systemBundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() >= Bundle.RESOLVED && !managed.contains(bundle.getBundleId())) {
                Resource res = bundle.adapt(BundleRevision.class);
                systemBundles.add(res);
            }
        }
        // Resolve
        // TODO: requirements
        // TODO: bundles
        // TODO: regions: on isolated regions, we may need different resolution for each region
        Set<String>  overrides    = Overrides.loadOverrides(this.overrides);
        Repository[] repositories = listRepositories();
        DeploymentBuilder builder = createDeploymentBuilder(repositories);
        builder.setFeatureRange(featureResolutionRange);
        builder.download(features,
                         Collections.<String>emptySet(),
                         Collections.<String>emptySet(),
                         overrides,
                         Collections.<String>emptySet());
        Map<Resource, List<Wire>> resolution = builder.resolve(systemBundles, true);
        Collection<Resource> allResources = resolution.keySet();
        Map<String, StreamProvider> providers = builder.getProviders();

        // Install conditionals
        List<String> installedFeatureIds = getFeatureIds(allResources);
        List<String> newFeatures = new ArrayList<String>(installedFeatureIds);
        newFeatures.removeAll(installed);
        List<String> delFeatures = new ArrayList<String>(installed);
        delFeatures.removeAll(installedFeatureIds);

        //
        // Compute list of installable resources (those with uris)
        //
        List<Resource> resources = getBundles(allResources);

        // Compute information for each bundle
        Map<String, BundleInfo> bundleInfos = new HashMap<String, BundleInfo>();
        for (Feature feature : getFeatures(repositories, getFeatureIds(allResources))) {
            for (BundleInfo bi : feature.getBundles()) {
                BundleInfo oldBi = bundleInfos.get(bi.getLocation());
                if (oldBi != null) {
                    bi = mergeBundleInfo(bi, oldBi);
                }
                bundleInfos.put(bi.getLocation(), bi);
            }
        }

        // TODO: handle bundleInfo.isStart()

        // Get all resources that will be used to satisfy the old features set
        Set<Resource> resourceLinkedToOldFeatures = new HashSet<Resource>();
        if (noStart) {
            for (Resource resource : resolution.keySet()) {
                String name = FeatureNamespace.getName(resource);
                if (name != null) {
                    Version version = FeatureNamespace.getVersion(resource);
                    String id = version != null ? name + "/" + version : name;
                    if (installed.contains(id)) {
                        addTransitive(resource, resourceLinkedToOldFeatures, resolution);
                    }
                }
            }
        }

        //
        // Compute deployment
        //
        Map<String, Long> bundleChecksums = new HashMap<String, Long>();
        synchronized (lock) {
            bundleChecksums.putAll(state.bundleChecksums);
        }
        Deployment deployment = computeDeployment(managed, bundles, providers, resources, bundleChecksums);

        if (deployment.toDelete.isEmpty() &&
                deployment.toUpdate.isEmpty() &&
                deployment.toInstall.isEmpty()) {
            print("No deployment change.", verbose);
            return;
        }
        //
        // Log deployment
        //
        logDeployment(deployment, verbose);

        //
        // Compute the set of bundles to refresh
        //
        Set<Bundle> toRefresh = new HashSet<Bundle>();
        toRefresh.addAll(deployment.toDelete);
        toRefresh.addAll(deployment.toUpdate.keySet());

        if (!noRefreshManaged) {
            int size;
            do {
                size = toRefresh.size();
                for (Bundle bundle : bundles) {
                    // Continue if we already know about this bundle
                    if (toRefresh.contains(bundle)) {
                        continue;
                    }
                    // Ignore non resolved bundle
                    BundleWiring wiring = bundle.adapt(BundleWiring.class);
                    if (wiring == null) {
                        continue;
                    }
                    // Get through the old resolution and flag this bundle
                    // if it was wired to a bundle to be refreshed
                    for (BundleWire wire : wiring.getRequiredWires(null)) {
                        if (toRefresh.contains(wire.getProvider().getBundle())) {
                            toRefresh.add(bundle);
                            break;
                        }
                    }
                    // Get through the new resolution and flag this bundle
                    // if it's wired to any new bundle
                    List<Wire> newWires = resolution.get(wiring.getRevision());
                    if (newWires != null) {
                        for (Wire wire : newWires) {
                            Bundle b = null;
                            if (wire.getProvider() instanceof BundleRevision) {
                                b = ((BundleRevision) wire.getProvider()).getBundle();
                            } else {
                                b = deployment.resToBnd.get(wire.getProvider());
                            }
                            if (b == null || toRefresh.contains(b)) {
                                toRefresh.add(bundle);
                                break;
                            }
                        }
                    }
                }
            } while (toRefresh.size() > size);
        }
        if (noRefreshUnmanaged) {
            Set<Bundle> newSet = new HashSet<Bundle>();
            for (Bundle bundle : toRefresh) {
                if (managed.contains(bundle.getBundleId())) {
                    newSet.add(bundle);
                }
            }
            toRefresh = newSet;
        }


        if (simulate) {
            if (!toRefresh.isEmpty()) {
                print("  Bundles to refresh:", verbose);
                for (Bundle bundle : toRefresh) {
                    print("    " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                }
            }
            return;
        }

        Set<Bundle> toStart = new HashSet<Bundle>();

        //
        // Execute deployment
        //

        // TODO: handle update on the features service itself
        if (deployment.toUpdate.containsKey(bundle) ||
                deployment.toDelete.contains(bundle)) {

            LOGGER.warn("Updating or uninstalling of the FeaturesService is not supported");
            deployment.toUpdate.remove(bundle);
            deployment.toDelete.remove(bundle);

        }

        //
        // Perform bundle operations
        //

        // Stop bundles by chunks
        Set<Bundle> toStop = new HashSet<Bundle>();
        toStop.addAll(deployment.toUpdate.keySet());
        toStop.addAll(deployment.toDelete);
        removeFragmentsAndBundlesInState(toStop, Bundle.UNINSTALLED | Bundle.RESOLVED | Bundle.STOPPING);
        if (!toStop.isEmpty()) {
            print("Stopping bundles:", verbose);
            while (!toStop.isEmpty()) {
                List<Bundle> bs = getBundlesToStop(toStop);
                for (Bundle bundle : bs) {
                    print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                    bundle.stop(Bundle.STOP_TRANSIENT);
                    toStop.remove(bundle);
                }
            }
        }
        if (!deployment.toDelete.isEmpty()) {
            print("Uninstalling bundles:", verbose);
            for (Bundle bundle : deployment.toDelete) {
                print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                bundle.uninstall();
                managed.remove(bundle.getBundleId());
            }
        }
        if (!deployment.toUpdate.isEmpty()) {
            print("Updating bundles:", verbose);
            for (Map.Entry<Bundle, Resource> entry : deployment.toUpdate.entrySet()) {
                Bundle bundle = entry.getKey();
                Resource resource = entry.getValue();
                String uri = UriNamespace.getUri(resource);
                print("  " + uri, verbose);
                InputStream is = getBundleInputStream(resource, providers);
                bundle.update(is);
                toStart.add(bundle);
                BundleInfo bi = bundleInfos.get(uri);
                if (bi != null && bi.getStartLevel() > 0) {
                    bundle.adapt(BundleStartLevel.class).setStartLevel(bi.getStartLevel());
                }
                // TODO: handle region
            }
        }
        if (!deployment.toInstall.isEmpty()) {
            print("Installing bundles:", verbose);
            for (Resource resource : deployment.toInstall) {
                String uri = UriNamespace.getUri(resource);
                print("  " + uri, verbose);
                InputStream is = getBundleInputStream(resource, providers);
                Bundle bundle = systemBundleContext.installBundle(uri, is);
                managed.add(bundle.getBundleId());
                if (!noStart || resourceLinkedToOldFeatures.contains(resource)) {
                    toStart.add(bundle);
                }
                deployment.resToBnd.put(resource, bundle);
                // save a checksum of installed snapshot bundle
                if (UPDATE_SNAPSHOTS_CRC.equals(updateSnaphots)
                        && isUpdateable(resource) && !deployment.newCheckums.containsKey(bundle.getLocation())) {
                    deployment.newCheckums.put(bundle.getLocation(), ChecksumUtils.checksum(getBundleInputStream(resource, providers)));
                }
                BundleInfo bi = bundleInfos.get(uri);
                if (bi != null && bi.getStartLevel() > 0) {
                    bundle.adapt(BundleStartLevel.class).setStartLevel(bi.getStartLevel());
                }
                // TODO: handle region
            }
        }

        //
        // Update and save state
        //
        synchronized (lock) {
            state.bundleChecksums.putAll(deployment.newCheckums);
            state.features.clear();
            state.features.addAll(features);
            state.installedFeatures.clear();
            state.installedFeatures.addAll(installedFeatureIds);
            state.managedBundles.clear();
            state.managedBundles.addAll(managed);
            saveState();
        }

        //
        // Install configurations
        //
        if (configInstaller != null && !newFeatures.isEmpty()) {
            for (Repository repository : repositories) {
                for (Feature feature : repository.getFeatures()) {
                    if (newFeatures.contains(feature.getId())) {
                        configInstaller.installFeatureConfigs(feature);
                    }
                }
            }
        }

        // TODO: remove this hack, but it avoids loading the class after the bundle is refreshed
        new CopyOnWriteArrayIdentityList().iterator();
        new RequirementSort();

        if (!noRefresh) {
            toStop = new HashSet<Bundle>();
            toStop.addAll(toRefresh);
            removeFragmentsAndBundlesInState(toStop, Bundle.UNINSTALLED | Bundle.RESOLVED | Bundle.STOPPING);
            if (!toStop.isEmpty()) {
                print("Stopping bundles:", verbose);
                while (!toStop.isEmpty()) {
                    List<Bundle> bs = getBundlesToStop(toStop);
                    for (Bundle bundle : bs) {
                        print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                        bundle.stop(Bundle.STOP_TRANSIENT);
                        toStop.remove(bundle);
                        toStart.add(bundle);
                    }
                }
            }

            if (!toRefresh.isEmpty()) {
                print("Refreshing bundles:", verbose);
                for (Bundle bundle : toRefresh) {
                    print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                }
                if (!toRefresh.isEmpty()) {
                    refreshPackages(toRefresh);
                }
            }
        }

        // Compute bundles to start
        removeFragmentsAndBundlesInState(toStart, Bundle.UNINSTALLED | Bundle.ACTIVE | Bundle.STARTING);
        if (!toStart.isEmpty()) {
            // Compute correct start order
            List<Exception> exceptions = new ArrayList<Exception>();
            print("Starting bundles:", verbose);
            while (!toStart.isEmpty()) {
                List<Bundle> bs = getBundlesToStart(toStart);
                for (Bundle bundle : bs) {
                    LOGGER.info("  " + bundle.getSymbolicName() + " / " + bundle.getVersion());
                    try {
                        bundle.start();
                    } catch (BundleException e) {
                        exceptions.add(e);
                    }
                    toStart.remove(bundle);
                }
            }
            if (!exceptions.isEmpty()) {
                throw new MultiException("Error restarting bundles", exceptions);
            }
        }

        // Call listeners
        for (Feature feature : getFeatures(repositories, delFeatures)) {
            callListeners(new FeatureEvent(feature, FeatureEvent.EventType.FeatureUninstalled, false));
        }
        for (Feature feature : getFeatures(repositories, newFeatures)) {
            callListeners(new FeatureEvent(feature, FeatureEvent.EventType.FeatureInstalled, false));
        }

        print("Done.", verbose);
    }

    private void addTransitive(Resource resource, Set<Resource> resources, Map<Resource, List<Wire>> resolution) {
        if (resources.add(resource)) {
            for (Wire wire : resolution.get(resource)) {
                addTransitive(wire.getProvider(), resources, resolution);
            }
        }
    }

    protected BundleInfo mergeBundleInfo(BundleInfo bi, BundleInfo oldBi) {
        // TODO: we need a proper merge strategy when a bundle
        // TODO: comes from different features
        return bi;
    }

    private void print(String message, boolean verbose) {
        LOGGER.info(message);
        if (verbose) {
            System.out.println(message);
        }
    }

    private void removeFragmentsAndBundlesInState(Collection<Bundle> bundles, int state) {
        for (Bundle bundle : new ArrayList<Bundle>(bundles)) {
            if ((bundle.getState() & state) != 0
                     || bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                bundles.remove(bundle);
            }
        }
    }

    protected void logDeployment(Deployment deployment, boolean verbose) {
        print("Changes to perform:", verbose);
        if (!deployment.toDelete.isEmpty()) {
            print("  Bundles to uninstall:", verbose);
            for (Bundle bundle : deployment.toDelete) {
                print("    " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
            }
        }
        if (!deployment.toUpdate.isEmpty()) {
            print("  Bundles to update:", verbose);
            for (Map.Entry<Bundle, Resource> entry : deployment.toUpdate.entrySet()) {
                print("    " + entry.getKey().getSymbolicName() + " / " + entry.getKey().getVersion() + " with " + UriNamespace.getUri(entry.getValue()), verbose);
            }
        }
        if (!deployment.toInstall.isEmpty()) {
            print("  Bundles to install:", verbose);
            for (Resource resource : deployment.toInstall) {
                print("    " + UriNamespace.getUri(resource), verbose);
            }
        }
    }

    protected Deployment computeDeployment(
                                Set<Long> managed,
                                Bundle[] bundles,
                                Map<String, StreamProvider> providers,
                                List<Resource> resources,
                                Map<String, Long> bundleChecksums) throws IOException {
        Deployment deployment = new Deployment();

        // TODO: regions
        List<Resource> toDeploy = new ArrayList<Resource>(resources);

        // First pass: go through all installed bundles and mark them
        // as either to ignore or delete
        for (Bundle bundle : bundles) {
            if (bundle.getSymbolicName() != null && bundle.getBundleId() != 0) {
                Resource resource = null;
                for (Resource res : toDeploy) {
                    if (bundle.getSymbolicName().equals(getSymbolicName(res))) {
                        if (bundle.getVersion().equals(getVersion(res))) {
                            resource = res;
                            break;
                        }
                    }
                }
                // We found a matching bundle
                if (resource != null) {
                    // In case of snapshots, check if the snapshot is out of date
                    // and flag it as to update
                    if (managed.contains(bundle.getBundleId()) && isUpdateable(resource)) {
                        // Always update snapshots
                        if (UPDATE_SNAPSHOTS_ALWAYS.equalsIgnoreCase(updateSnaphots)) {
                            LOGGER.debug("Update snapshot for " + bundle.getLocation());
                            deployment.toUpdate.put(bundle, resource);
                        }
                        else if (UPDATE_SNAPSHOTS_CRC.equalsIgnoreCase(updateSnaphots)) {
                            // if the checksum are different
                            InputStream is = null;
                            try {
                                is = getBundleInputStream(resource, providers);
                                long newCrc = ChecksumUtils.checksum(is);
                                long oldCrc = bundleChecksums.containsKey(bundle.getLocation()) ? bundleChecksums.get(bundle.getLocation()) : 0l;
                                if (newCrc != oldCrc) {
                                    LOGGER.debug("New snapshot available for " + bundle.getLocation());
                                    deployment.toUpdate.put(bundle, resource);
                                    deployment.newCheckums.put(bundle.getLocation(), newCrc);
                                }
                            } finally {
                                if (is != null) {
                                    is.close();
                                }
                            }
                        }
                    }
                    // We're done for this resource
                    toDeploy.remove(resource);
                    deployment.resToBnd.put(resource, bundle);
                // There's no matching resource
                // If the bundle is managed, we need to delete it
                } else if (managed.contains(bundle.getBundleId())) {
                    deployment.toDelete.add(bundle);
                }
            }
        }

        // Second pass on remaining resources
        for (Resource resource : toDeploy) {
            TreeMap<Version, Bundle> matching = new TreeMap<Version, Bundle>();
            VersionRange range = new VersionRange(Macro.transform(bundleUpdateRange, getVersion(resource).toString()));
            for (Bundle bundle : deployment.toDelete) {
                if (bundle.getSymbolicName().equals(getSymbolicName(resource)) && range.contains(bundle.getVersion())) {
                    matching.put(bundle.getVersion(), bundle);
                }
            }
            if (!matching.isEmpty()) {
                Bundle bundle = matching.lastEntry().getValue();
                deployment.toUpdate.put(bundle, resource);
                deployment.toDelete.remove(bundle);
                deployment.resToBnd.put(resource, bundle);
            } else {
                deployment.toInstall.add(resource);
            }
        }
        return deployment;
    }

    protected List<Resource> getBundles(Collection<Resource> allResources) {
        Map<String, Resource> deploy = new TreeMap<String, Resource>();
        for (Resource res : allResources) {
            String uri = UriNamespace.getUri(res);
            if (uri != null) {
                deploy.put(uri, res);
            }
        }
        return new ArrayList<Resource>(deploy.values());
    }

    protected List<Feature> getFeatures(Repository[] repositories, List<String> featureIds) throws Exception {
        List<Feature> installedFeatures = new ArrayList<Feature>();
        for (Repository repository : repositories) {
            for (Feature feature : repository.getFeatures()) {
                String id = feature.getName() + "/" + VersionTable.getVersion(feature.getVersion());
                if (featureIds.contains(id)) {
                    installedFeatures.add(feature);
                }
            }
        }
        return installedFeatures;
    }

    protected List<String> getFeatureIds(Collection<Resource> allResources) {
        List<String> installedFeatureIds = new ArrayList<String>();
        for (Resource resource : allResources) {
            String name = FeatureNamespace.getName(resource);
            if (name != null) {
                Version version = FeatureNamespace.getVersion(resource);
                String id = version != null ? name + "/" + version : name;
                installedFeatureIds.add(id);
            }
        }
        return installedFeatureIds;
    }

    protected DeploymentBuilder createDeploymentBuilder(Repository[] repositories) {
        return new DeploymentBuilder(new SimpleDownloader(), Arrays.asList(repositories));
    }


    protected boolean isUpdateable(Resource resource) {
        return (getVersion(resource).getQualifier().endsWith(SNAPSHOT) ||
                UriNamespace.getUri(resource).contains(SNAPSHOT) ||
                !UriNamespace.getUri(resource).contains(MAVEN));
    }

    protected List<Bundle> getBundlesToStart(Collection<Bundle> bundles) {
        // TODO: make this pluggable ?
        // TODO: honor respectStartLvlDuringFeatureStartup

        // We hit FELIX-2949 if we don't use the correct order as Felix resolver isn't greedy.
        // In order to minimize that, we make sure we resolve the bundles in the order they
        // are given back by the resolution, meaning that all root bundles (i.e. those that were
        // not flagged as dependencies in features) are started before the others.   This should
        // make sure those important bundles are started first and minimize the problem.

        // Restart the features service last, regardless of any other consideration
        // so that we don't end up with the service trying to do stuff before we're done
        boolean restart = bundles.remove(bundle);

        List<BundleRevision> revs = new ArrayList<BundleRevision>();
        for (Bundle bundle : bundles) {
            revs.add(bundle.adapt(BundleRevision.class));
        }
        List<Bundle> sorted = new ArrayList<Bundle>();
        for (BundleRevision rev : RequirementSort.sort(revs)) {
            sorted.add(rev.getBundle());
        }
        if (restart) {
            sorted.add(bundle);
        }
        return sorted;
    }

    protected List<Bundle> getBundlesToStop(Collection<Bundle> bundles) {
        // TODO: make this pluggable ?
        // TODO: honor respectStartLvlDuringFeatureUninstall

        List<Bundle> bundlesToDestroy = new ArrayList<Bundle>();
        for (Bundle bundle : bundles) {
            ServiceReference[] references = bundle.getRegisteredServices();
            int usage = 0;
            if (references != null) {
                for (ServiceReference reference : references) {
                    usage += getServiceUsage(reference, bundles);
                }
            }
            LOGGER.debug("Usage for bundle {} is {}", bundle, usage);
            if (usage == 0) {
                bundlesToDestroy.add(bundle);
            }
        }
        if (!bundlesToDestroy.isEmpty()) {
            Collections.sort(bundlesToDestroy, new Comparator<Bundle>() {
                public int compare(Bundle b1, Bundle b2) {
                    return (int) (b2.getLastModified() - b1.getLastModified());
                }
            });
            LOGGER.debug("Selected bundles {} for destroy (no services in use)", bundlesToDestroy);
        } else {
            ServiceReference ref = null;
            for (Bundle bundle : bundles) {
                ServiceReference[] references = bundle.getRegisteredServices();
                for (ServiceReference reference : references) {
                    if (getServiceUsage(reference, bundles) == 0) {
                        continue;
                    }
                    if (ref == null || reference.compareTo(ref) < 0) {
                        LOGGER.debug("Currently selecting bundle {} for destroy (with reference {})", bundle, reference);
                        ref = reference;
                    }
                }
            }
            if (ref != null) {
                bundlesToDestroy.add(ref.getBundle());
            }
            LOGGER.debug("Selected bundle {} for destroy (lowest ranking service)", bundlesToDestroy);
        }
        return bundlesToDestroy;
    }

    private static int getServiceUsage(ServiceReference ref, Collection<Bundle> bundles) {
        Bundle[] usingBundles = ref.getUsingBundles();
        int nb = 0;
        if (usingBundles != null) {
            for (Bundle bundle : usingBundles) {
                if (bundles.contains(bundle)) {
                    nb++;
                }
            }
        }
        return nb;
    }

    protected InputStream getBundleInputStream(Resource resource, Map<String, StreamProvider> providers) throws IOException {
        String uri = UriNamespace.getUri(resource);
        if (uri == null) {
            throw new IllegalStateException("Resource has no uri");
        }
        StreamProvider provider = providers.get(uri);
        if (provider == null) {
            throw new IllegalStateException("Resource " + uri + " has no StreamProvider");
        }
        return provider.open();
    }

    protected void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
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


    static class Deployment {
        Map<String, Long> newCheckums = new HashMap<String, Long>();
        Map<Resource, Bundle> resToBnd = new HashMap<Resource, Bundle>();
        List<Resource> toInstall = new ArrayList<Resource>();
        List<Bundle> toDelete = new ArrayList<Bundle>();
        Map<Bundle, Resource> toUpdate = new HashMap<Bundle, Resource>();
    }

}
