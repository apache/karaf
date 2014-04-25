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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
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
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.region.SubsystemResolver;
import org.apache.karaf.features.internal.util.ChecksumUtils;
import org.apache.karaf.features.internal.util.Macro;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.features.internal.util.MultiException;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
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
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getFeatureId;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getUri;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.apache.karaf.features.internal.util.MapUtils.apply;
import static org.apache.karaf.features.internal.util.MapUtils.contains;
import static org.apache.karaf.features.internal.util.MapUtils.copy;
import static org.apache.karaf.features.internal.util.MapUtils.diff;
import static org.apache.karaf.features.internal.util.MapUtils.flatten;
import static org.apache.karaf.features.internal.util.MapUtils.map;
import static org.apache.karaf.features.internal.util.MapUtils.removeFromMapSet;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.RESOLVED;
import static org.osgi.framework.Bundle.STARTING;
import static org.osgi.framework.Bundle.STOPPING;
import static org.osgi.framework.Bundle.STOP_TRANSIENT;
import static org.osgi.framework.Bundle.UNINSTALLED;

/**
 *
 */
public class FeaturesServiceImpl implements FeaturesService {

    public static final String ROOT_REGION = "root";

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
    private final RegionDigraph digraph;
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

    /**
     * Optional global repository
     */
    private final org.osgi.service.repository.Repository globalRepository;

    private final List<FeaturesListener> listeners = new CopyOnWriteArrayIdentityList<FeaturesListener>();

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
            Set<String> installedFeatures = new TreeSet<>();
            synchronized (lock) {
                repositories.addAll(state.repositories);
                installedFeatures.addAll(state.installedFeatures.keySet());
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
        String id = f.getName() + "/" + new VersionRange(f.getVersion(), true);
        synchronized (lock) {
            Set<String> features = state.requestedFeatures.get(ROOT_REGION);
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
        Map<String, Set<String>> required = copy(state.requestedFeatures);
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
            required.put(region,fl);
        }
        fl.addAll(featuresToAdd);
        doInstallFeaturesInThread(required, state, options);
    }

    public void uninstallFeatures(Set<String> features, String region, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requestedFeatures);
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
                String nameSep = feature.substring(0, feature.indexOf("/") + 1);
                for (String f : fl) {
                    if (normalize(f).startsWith(nameSep)) {
                        toRemove.add(f);
                    }
                }
            } else {
                String name = feature.substring(0, feature.indexOf("/"));
                String version = feature.substring(feature.indexOf("/") + 1);
                String req = name + "/" + new VersionRange(version, true);
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
                    sb.append(toRemove.get(i));
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
        doInstallFeaturesInThread(required, state, options);
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
        return name + "/" + VersionTable.getVersion(version).toString();
    }

    /**
     * Actual deployment needs to be done in a separate thread.
     * The reason is that if the console is refreshed, the current thread which is running
     * the command may be interrupted while waiting for the refresh to be done, leading
     * to bundles not being started after the refresh.
     */
    public void doInstallFeaturesInThread(final Map<String, Set<String>> features,
                                          final State state,
                                          final EnumSet<Option> options) throws Exception {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    doInstallFeatures(features, state, options);
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

    static class DeploymentState {
        Map<Long, Bundle> bundles;
        Map<String, Feature> features;
        Map<String, Set<Long>> bundlesPerRegion;
        Map<String, Map<String, Map<String, Set<String>>>> filtersPerRegion;
    }
    
    protected DeploymentState getDeploymentState() throws Exception {
        DeploymentState state = new DeploymentState();
        // Bundles
        state.bundles = new HashMap<>();
        for (Bundle bundle : systemBundleContext.getBundles()) {
            state.bundles.put(bundle.getBundleId(), bundle);
        }
        // Features
        state.features = new HashMap<>();
        for (Map<String, Feature> m : getFeatures().values()) {
            for (Feature feature : m.values()) {
                String id = feature.getName() + "/" + VersionTable.getVersion(feature.getVersion());
                state.features.put(id, feature);
            }
        }
        // Region -> bundles mapping
        // Region -> policy mapping
        state.bundlesPerRegion = new HashMap<>();
        state.filtersPerRegion = new HashMap<>();
        RegionDigraph clone = digraph.copy();
        for (Region region : clone.getRegions()) {
            // Get bundles
            state.bundlesPerRegion.put(region.getName(), new HashSet<>(region.getBundleIds()));
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
            state.filtersPerRegion.put(region.getName(), edges);
        }
        // Return
        return state;
    }

    public void doInstallFeatures(Map<String, Set<String>> requestedFeatures,  // all request features
                                  State                    state,              // current state
                                  EnumSet<Option>          options             // installation options
                    ) throws Exception {

        boolean noRefreshUnmanaged = options.contains(Option.NoAutoRefreshUnmanagedBundles);
        boolean noRefreshManaged = options.contains(Option.NoAutoRefreshManagedBundles);
        boolean noRefresh = options.contains(Option.NoAutoRefreshBundles);
        boolean noStart = options.contains(Option.NoAutoStartBundles);
        boolean verbose = options.contains(Option.Verbose);
        boolean simulate = options.contains(Option.Simulate);
        boolean noManageBundles = options.contains(Option.NoAutoManageBundles);

        DeploymentState dstate = getDeploymentState();
        
        Map<String, Set<Long>> managedBundles = copy(state.managedBundles);

        Map<String, Set<Bundle>> unmanagedBundles = apply(diff(dstate.bundlesPerRegion, state.managedBundles),
                                                          map(dstate.bundles));

        // Resolve
        // TODO: requirements
        // TODO: bundles

        SubsystemResolver resolver = new SubsystemResolver();
        resolver.resolve(
                dstate.features.values(),
                requestedFeatures,
                apply(unmanagedBundles, adapt(BundleRevision.class)),
                Overrides.loadOverrides(this.overrides),
                featureResolutionRange,
                globalRepository);

        Map<String, StreamProvider> providers = resolver.getProviders();
        Map<String, Set<Resource>> featuresPerRegion = resolver.getFeaturesPerRegions();
        Map<String, Set<String>> installedFeatures = apply(featuresPerRegion, featureId());
        Map<String, Set<String>> newFeatures = diff(installedFeatures, state.installedFeatures);
        Map<String, Set<String>> delFeatures = diff(state.installedFeatures, installedFeatures);

        // Compute information for each bundle
        Map<String, Map<String, BundleInfo>> bundleInfos = resolver.getBundleInfos();

        // Get all resources that will be used to satisfy the old features set
        // If noStart is true, we don't want to start the newly installed features
        // but we still want old features to be started.
        Set<Resource> resourceLinkedToOldFeatures = new HashSet<>();
        if (noStart) {
            for (Map.Entry<String, Set<Resource>> entry : featuresPerRegion.entrySet()) {
                String region = entry.getKey();
                for (Resource resource : entry.getValue()) {
                    String id = getFeatureId(resource);
                    if (contains(state.installedFeatures, region, id)) {
                        addTransitive(resource, resourceLinkedToOldFeatures, resolver.getWiring());
                    }
                }
            }
        }

        //
        // Compute deployment
        //
        Deployment deployment = computeDeployment(dstate, resolver, state);

        //
        // Compute the set of bundles to refresh
        //
        Set<Bundle> toRefresh = new TreeSet<>(new BundleComparator()); // sort is only used for display
        for (RegionDeployment regionDeployment : deployment.regions.values()) {
            toRefresh.addAll(regionDeployment.toDelete);
            toRefresh.addAll(regionDeployment.toUpdate.keySet());
        }
        if (!noRefreshManaged) {
            computeBundlesToRefresh(toRefresh, dstate.bundles.values(), deployment.resToBnd, resolver.getWiring());
        }
        if (noRefreshUnmanaged) {
            toRefresh.removeAll(flatten(unmanagedBundles));
        }

        // Automatically turn unmanaged bundles into managed bundles
        // if they are required by a feature and no other unmanaged
        // bundles have a requirement on it
        Set<Bundle> toManage = new TreeSet<>(new BundleComparator()); // sort is only used for display
        if (!noManageBundles) {
            Set<Resource> features = resolver.getFeatures().keySet();
            Set<? extends Resource> unmanaged = apply(flatten(unmanagedBundles), adapt(BundleRevision.class));
            Set<Resource> requested = new HashSet<>();
            // Gather bundles required by a feature
            for (List<Wire> wires : resolver.getWiring().values()) {
                for (Wire wire : wires) {
                    if (features.contains(wire.getRequirer()) && unmanaged.contains(wire.getProvider())) {
                        requested.add(wire.getProvider());
                    }
                }
            }
            // Now, we know which bundles are completely unmanaged
            unmanaged.removeAll(requested);
            // Check if bundles have wires from really unmanaged bundles
            for (List<Wire> wires : resolver.getWiring().values()) {
                for (Wire wire : wires) {
                    if (requested.contains(wire.getProvider()) && unmanaged.contains(wire.getRequirer())) {
                        requested.remove(wire.getProvider());
                    }
                }
            }
            if (!requested.isEmpty()) {
                Map<Long, String> bundleToRegion = new HashMap<>();
                for (Map.Entry<String, Set<Long>> entry : dstate.bundlesPerRegion.entrySet()) {
                    for (long id : entry.getValue()) {
                        bundleToRegion.put(id, entry.getKey());
                    }
                }
                for (Resource rev : requested) {
                    Bundle bundle = ((BundleRevision) rev).getBundle();
                    long id = bundle.getBundleId();
                    addToMapSet(managedBundles, bundleToRegion.get(id), id);
                    toManage.add(bundle);
                }
            }
        }

        //
        // Log deployment
        //
        logDeployment(deployment, verbose);

        if (simulate) {
            if (!noRefresh && !toRefresh.isEmpty()) {
                print("  Bundles to refresh:", verbose);
                for (Bundle bundle : toRefresh) {
                    print("    " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                }
            }
            if (!toManage.isEmpty()) {
                print("  Managing bundle:", verbose);
                for (Bundle bundle : toManage) {
                    print("    " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                }
            }
            return;
        }

        Set<Bundle> toStart = new HashSet<>();
        Set<Bundle> toResolve = new HashSet<>();
        Set<Bundle> toStop = new HashSet<>();

        //
        // Execute deployment
        //
        // #1: stop bundles that needs to be updated or uninstalled in order
        // #2: uninstall needed bundles
        // #3: update regions
        // #4: update bundles
        // #5: install bundles
        // #6: save state
        // #7: install configuration
        // #8: refresh bundles
        // #9: start bundles in order
        // #10: send events
        //

        // TODO: handle update on the features service itself
        RegionDeployment rootRegionDeployment = deployment.regions.get(ROOT_REGION);
        if (rootRegionDeployment != null &&
                (rootRegionDeployment.toUpdate.containsKey(bundle)
                        || rootRegionDeployment.toDelete.contains(bundle))) {

            LOGGER.warn("Updating or uninstalling of the FeaturesService is not supported");
            rootRegionDeployment.toUpdate.remove(bundle);
            rootRegionDeployment.toDelete.remove(bundle);

        }

        //
        // Perform bundle operations
        //

        //
        // Find start levels to update
        //
        Map<Bundle, Integer> toUpdateStartLevel = new HashMap<>();
        {
            FrameworkStartLevel fsl = systemBundleContext.getBundle().adapt(FrameworkStartLevel.class);
            for (Map.Entry<Resource, String> entry : resolver.getBundles().entrySet()) {
                Resource resource = entry.getKey();
                Bundle bundle = deployment.resToBnd.get(resource);
                String region = entry.getValue();
                BundleInfo bi = bundleInfos.get(region).get(getUri(resource));
                if (bundle != null && bi != null) {
                    int sl;
                    if (bi.getStartLevel() > 0) {
                        sl = bi.getStartLevel();
                    } else {
                        sl = fsl.getInitialBundleStartLevel();
                    }
                    BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
                    if (bundleStartLevel.getStartLevel() != sl) {
                        toUpdateStartLevel.put(bundle, sl);
                        if (sl > fsl.getStartLevel()) {
                            toStop.add(bundle);
                        }
                    }
                    if (bi.isStart()) {
                        toStart.add(bundle);
                    } else {
                        toStop.add(bundle);
                    }
                }
            }
        }

        //
        // Stop bundles by chunks
        //
        for (RegionDeployment regionDeployment : deployment.regions.values()) {
            toStop.addAll(regionDeployment.toUpdate.keySet());
            toStop.addAll(regionDeployment.toDelete);
        }
        removeFragmentsAndBundlesInState(toStop, UNINSTALLED | RESOLVED | STOPPING);
        if (!toStop.isEmpty()) {
            print("Stopping bundles:", verbose);
            while (!toStop.isEmpty()) {
                List<Bundle> bs = getBundlesToStop(toStop);
                for (Bundle bundle : bs) {
                    print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                    // If the bundle start level will be changed, stop it persistently to
                    // avoid a restart when the start level is actually changed
                    bundle.stop(toUpdateStartLevel.containsKey(bundle) ? 0 : STOP_TRANSIENT);
                    toStop.remove(bundle);
                }
            }
        }

        //
        // Delete bundles
        //
        boolean hasToDelete = false;
        for (RegionDeployment regionDeployment : deployment.regions.values()) {
            if ((hasToDelete = !regionDeployment.toDelete.isEmpty())) {
                break;
            }
        }
        if (hasToDelete) {
            print("Uninstalling bundles:", verbose);
            for (Map.Entry<String, RegionDeployment> entry : deployment.regions.entrySet()) {
                String name = entry.getKey();
                RegionDeployment regionDeployment = entry.getValue();
                for (Bundle bundle : regionDeployment.toDelete) {
                    print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                    bundle.uninstall();
                    removeFromMapSet(managedBundles, name, bundle.getBundleId());
                }
            }
        }

        //
        // Update regions
        //
        {
            RegionDigraph clone = digraph.copy();
            RegionDigraph computedDigraph = resolver.getFlatDigraph();
            Map<String, Map<String, Map<String, Set<String>>>> policies = copy(dstate.filtersPerRegion);
            // Iterate through previously managed regions and
            // delete those that do not contain any bundles anymore
            for (String name : state.managedBundles.keySet()) {
                if (!managedBundles.containsKey(name) && !unmanagedBundles.containsKey(name)) {
                    dstate.filtersPerRegion.remove(name);
                }
            }
            // Fix broken filters
            for (String name : policies.keySet()) {
                policies.get(name).keySet().retainAll(policies.keySet());
            }
            // Update managed regions
            for (Region computedRegion : computedDigraph.getRegions()) {
                String name = computedRegion.getName();
                Map<String, Map<String, Set<String>>> policy = policies.get(name);
                if (policy == null) {
                    policy = new HashMap<>();
                    policies.put(name, policy);
                }
                for (RegionDigraph.FilteredRegion fr : computedDigraph.getEdges(computedRegion)) {
                    String r2 = fr.getRegion().getName();
                    Map<String, Set<String>> filters = new HashMap<>();
                    Map<String, Collection<String>> current = fr.getFilter().getSharingPolicy();
                    for (String ns : current.keySet()) {
                        for (String f : current.get(ns)) {
                            addToMapSet(filters, ns, f);
                        }
                    }
                    policy.put(r2, filters);
                }
            }
            // Apply all changes
            for (Region region : clone.getRegions()) {
                clone.removeRegion(region);
            }
            for (String name : policies.keySet()) {
                clone.createRegion(name);
            }
            for (String r1Name : policies.keySet()) {
                Region r1 = clone.getRegion(r1Name);
                Map<String, Map<String, Set<String>>> policy = policies.get(r1Name);
                for (String r2Name : policy.keySet()) {
                    Region r2 = clone.getRegion(r2Name);
                    RegionFilterBuilder rfb = clone.createRegionFilterBuilder();
                    for (String ns : policy.get(r2Name).keySet()) {
                        for (String f : policy.get(r2Name).get(ns)) {
                            rfb.allow(ns, f);
                        }
                    }
                    clone.connect(r1, rfb.build(), r2);
                }
                // Dispatch bundles
                if (unmanagedBundles.containsKey(r1Name)) {
                    for (Bundle bundle : unmanagedBundles.get(r1Name)) {
                        r1.addBundle(bundle);
                    }
                }
                if (managedBundles.containsKey(r1Name)) {
                    for (long id : managedBundles.get(r1Name)) {
                        r1.addBundle(id);
                    }
                }
            }
            this.digraph.replace(clone);
        }


        //
        // Update bundles
        //
        boolean hasToUpdate = false;
        for (RegionDeployment regionDeployment : deployment.regions.values()) {
            if ((hasToUpdate = !regionDeployment.toUpdate.isEmpty())) {
                break;
            }
        }
        if (hasToUpdate) {
            print("Updating bundles:", verbose);
            for (Map.Entry<String, RegionDeployment> rde : deployment.regions.entrySet()) {
                for (Map.Entry<Bundle, Resource> entry : rde.getValue().toUpdate.entrySet()) {
                    Bundle bundle = entry.getKey();
                    Resource resource = entry.getValue();
                    String uri = getUri(resource);
                    print("  " + uri, verbose);
                    try (
                        InputStream is = getBundleInputStream(resource, providers)
                    ) {
                        bundle.update(is);
                    }
                    toStart.add(bundle);
                }
            }
        }
        //
        // Update start levels
        //
        for (Map.Entry<Bundle, Integer> entry : toUpdateStartLevel.entrySet()) {
            Bundle bundle = entry.getKey();
            int sl = entry.getValue();
            bundle.adapt(BundleStartLevel.class).setStartLevel(sl);
        }
        //
        // Install bundles
        //
        boolean hasToInstall = false;
        for (RegionDeployment regionDeployment : deployment.regions.values()) {
            if ((hasToInstall = !regionDeployment.toInstall.isEmpty())) {
                break;
            }
        }
        if (hasToInstall) {
            print("Installing bundles:", verbose);
            for (Map.Entry<String, RegionDeployment> entry : deployment.regions.entrySet()) {
                String name = entry.getKey();
                Region region = digraph.getRegion(name);
                RegionDeployment regionDeployment = entry.getValue();
                for (Resource resource : regionDeployment.toInstall) {
                    String uri = getUri(resource);
                    print("  " + uri, verbose);
                    Bundle bundle;
                    long crc;
                    try (
                        ChecksumUtils.CRCInputStream is = new ChecksumUtils.CRCInputStream (getBundleInputStream(resource, providers))
                    ) {
                        if (ROOT_REGION.equals(name)) {
                            bundle = region.installBundleAtLocation(uri, is);
                        } else {
                            bundle = region.installBundle(uri, is);
                        }
                        crc = is.getCRC();
                    }
                    addToMapSet(managedBundles, name, bundle.getBundleId());
                    deployment.resToBnd.put(resource, bundle);
                    // save a checksum of installed snapshot bundle
                    if (UPDATE_SNAPSHOTS_CRC.equals(updateSnaphots)
                            && isUpdateable(resource) && !deployment.bundleChecksums.containsKey(bundle.getBundleId())) {
                        deployment.bundleChecksums.put(bundle.getBundleId(), crc);
                    }
                    BundleInfo bi = bundleInfos.get(entry.getKey()).get(uri);
                    if (bi != null && bi.getStartLevel() > 0) {
                        bundle.adapt(BundleStartLevel.class).setStartLevel(bi.getStartLevel());
                    }
                    toResolve.add(bundle);
                    if (resourceLinkedToOldFeatures.contains(resource)) {
                        toStart.add(bundle);
                    } else if (!noStart) {
                        if (bi == null || bi.isStart()) {
                            toStart.add(bundle);
                        }
                    }
                }
            }
        }

       //
        // Update and save state
        //
        synchronized (lock) {
            this.state.bundleChecksums.clear();
            this.state.bundleChecksums.putAll(deployment.bundleChecksums);
            this.state.requestedFeatures.clear();
            this.state.requestedFeatures.putAll(requestedFeatures);
            this.state.installedFeatures.clear();
            this.state.installedFeatures.putAll(installedFeatures);
            this.state.managedBundles.clear();
            this.state.managedBundles.putAll(managedBundles);
            saveState();
        }

        //
        // Install configurations
        //
        if (configInstaller != null && !newFeatures.isEmpty()) {
            Set<Feature> set = apply(flatten(newFeatures), map(dstate.features));
            for (Feature feature : set) {
                configInstaller.installFeatureConfigs(feature);
            }
        }

        // TODO: remove this hack, but it avoids loading the class after the bundle is refreshed
        new CopyOnWriteArrayIdentityList().iterator();
        RequirementSort.sort(Collections.<Resource>emptyList());

        if (!noRefresh) {
            toStop = new HashSet<>();
            toStop.addAll(toRefresh);
            removeFragmentsAndBundlesInState(toStop, UNINSTALLED | RESOLVED | STOPPING);
            if (!toStop.isEmpty()) {
                print("Stopping bundles:", verbose);
                while (!toStop.isEmpty()) {
                    List<Bundle> bs = getBundlesToStop(toStop);
                    for (Bundle bundle : bs) {
                        print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                        bundle.stop(STOP_TRANSIENT);
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

        // Resolve bundles
        toResolve.addAll(toStart);
        toResolve.addAll(toRefresh);
        removeFragmentsAndBundlesInState(toResolve, UNINSTALLED);
        systemBundleContext.getBundle().adapt(FrameworkWiring.class).resolveBundles(toResolve);

        // Compute bundles to start
        removeFragmentsAndBundlesInState(toStart, UNINSTALLED | ACTIVE | STARTING);
        if (!toStart.isEmpty()) {
            // Compute correct start order
            List<Exception> exceptions = new ArrayList<>();
            print("Starting bundles:", verbose);
            while (!toStart.isEmpty()) {
                List<Bundle> bs = getBundlesToStart(toStart);
                for (Bundle bundle : bs) {
                    print("  " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
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
        // TODO: add region information and avoid flattening
        for (Feature feature : apply(flatten(delFeatures), map(dstate.features))) {
            callListeners(new FeatureEvent(feature, FeatureEvent.EventType.FeatureUninstalled, false));
        }
        for (Feature feature : apply(flatten(newFeatures), map(dstate.features))) {
            callListeners(new FeatureEvent(feature, FeatureEvent.EventType.FeatureInstalled, false));
        }

        print("Done.", verbose);
    }

    private void computeBundlesToRefresh(Set<Bundle> toRefresh, Collection<Bundle> bundles, Map<Resource, Bundle> resources, Map<Resource, List<Wire>> resolution) {
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
                        Bundle b;
                        if (wire.getProvider() instanceof BundleRevision) {
                            b = ((BundleRevision) wire.getProvider()).getBundle();
                        } else {
                            b = resources.get(wire.getProvider());
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

    private void addTransitive(Resource resource, Set<Resource> resources, Map<Resource, List<Wire>> resolution) {
        if (resources.add(resource)) {
            for (Wire wire : resolution.get(resource)) {
                addTransitive(wire.getProvider(), resources, resolution);
            }
        }
    }

    private void print(String message, boolean verbose) {
        LOGGER.info(message);
        if (verbose) {
            System.out.println(message);
        }
    }

    private void removeFragmentsAndBundlesInState(Collection<Bundle> bundles, int state) {
        for (Iterator<Bundle> iterator = bundles.iterator(); iterator.hasNext();) {
            Bundle bundle = iterator.next();
            if ((bundle.getState() & state) != 0
                    || bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                iterator.remove();
            }
        }
    }

    protected void logDeployment(Deployment overallDeployment, boolean verbose) {
        if (overallDeployment.regions.isEmpty()) {
            print("No deployment change.", verbose);
            return;
        }
        print("Changes to perform:", verbose);
        for (Map.Entry<String, RegionDeployment> region : overallDeployment.regions.entrySet()) {
            RegionDeployment deployment = region.getValue();
            print("  Region: " + region.getKey(), verbose);
            if (!deployment.toDelete.isEmpty()) {
                print("    Bundles to uninstall:", verbose);
                for (Bundle bundle : deployment.toDelete) {
                    print("      " + bundle.getSymbolicName() + " / " + bundle.getVersion(), verbose);
                }
            }
            if (!deployment.toUpdate.isEmpty()) {
                print("    Bundles to update:", verbose);
                for (Map.Entry<Bundle, Resource> entry : deployment.toUpdate.entrySet()) {
                    print("      " + entry.getKey().getSymbolicName() + " / " + entry.getKey().getVersion() + " with " + getUri(entry.getValue()), verbose);
                }
            }
            if (!deployment.toInstall.isEmpty()) {
                print("    Bundles to install:", verbose);
                for (Resource resource : deployment.toInstall) {
                    print("      " + getUri(resource), verbose);
                }
            }
        }
    }

    protected Deployment computeDeployment(
                                DeploymentState dstate,
                                SubsystemResolver resolver,
                                State state) throws IOException {

        Deployment result = new Deployment();

        Map<String, Set<Resource>> bundlesPerRegions = resolver.getBundlesPerRegions();

        // Gather all regions, including old ones and new ones
        Set<String> regions = new HashSet<>();
        regions.addAll(state.managedBundles.keySet());
        regions.addAll(bundlesPerRegions.keySet());

        for (String region : regions) {

            RegionDeployment deployment = new RegionDeployment();

            // Get the list of bundles currently assigned in the region
            Set<Long> managed = state.managedBundles.get(region);
            if (managed == null) {
                managed = Collections.emptySet();
            }

            // Compute the list of resources to deploy in the region
            Set<Resource> bundlesInRegion = bundlesPerRegions.get(region);
            List<Resource> toDeploy = bundlesInRegion != null
                            ? new ArrayList<>(bundlesInRegion) : new ArrayList<Resource>();

            // First pass: go through all installed bundles and mark them
            // as either to ignore or delete
            for (long bundleId : managed) {
                // Look for the installed bundle
                Bundle bundle = dstate.bundles.get(bundleId);
                // Bundle has been manually uninstalled ?
                if (bundle != null) {
                    // Look for a matching resource
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
                        if (isUpdateable(resource)) {
                            // Always update snapshots
                            if (UPDATE_SNAPSHOTS_ALWAYS.equalsIgnoreCase(updateSnaphots)) {
                                LOGGER.debug("Update snapshot for " + bundle.getLocation());
                                deployment.toUpdate.put(bundle, resource);
                            } else if (UPDATE_SNAPSHOTS_CRC.equalsIgnoreCase(updateSnaphots)) {
                                // if the checksum are different
                                try (
                                    InputStream is = getBundleInputStream(resource, resolver.getProviders())
                                ) {
                                    long newCrc = ChecksumUtils.checksum(is);
                                    long oldCrc = state.bundleChecksums.containsKey(bundle.getBundleId()) ? state.bundleChecksums.get(bundle.getBundleId()) : 0l;
                                    if (newCrc != oldCrc) {
                                        LOGGER.debug("New snapshot available for " + bundle.getLocation());
                                        deployment.toUpdate.put(bundle, resource);
                                    }
                                    result.bundleChecksums.put(bundle.getBundleId(), newCrc);
                                }
                            }
                        }
                        // We're done for this resource
                        toDeploy.remove(resource);
                        result.resToBnd.put(resource, bundle);
                        // There's no matching resource
                        // If the bundle is managed, we need to delete it
                    } else if (managed.contains(bundle.getBundleId())) {
                        deployment.toDelete.add(bundle);
                    }
                }
            }

            // Second pass on remaining resources
            for (Resource resource : toDeploy) {
                TreeMap<Version, Bundle> matching = new TreeMap<>();
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
                    result.resToBnd.put(resource, bundle);
                } else {
                    deployment.toInstall.add(resource);
                }
            }
            Collections.sort(deployment.toInstall, new ResourceComparator());

            // Add this region if there is something to do
            if (!deployment.toDelete.isEmpty()
                    || !deployment.toUpdate.isEmpty()
                    || !deployment.toInstall.isEmpty()) {
                result.regions.put(region, deployment);
            }
        }

        return result;
    }

    protected <T> MapUtils.Function<Bundle, T> adapt(final Class<T> clazz) {
        return new MapUtils.Function<Bundle, T>() {
            @Override
            public T apply(Bundle bundle) {
                return bundle.adapt(clazz);
            }
        };
    }

    protected MapUtils.Function<Resource, String> featureId() {
        return new MapUtils.Function<Resource, String>() {
            @Override
            public String apply(Resource resource) {
                return getFeatureId(resource);
            }
        };
    }


    protected boolean isUpdateable(Resource resource) {
        String uri = getUri(resource);
        return getVersion(resource).getQualifier().endsWith(SNAPSHOT) 
                || uri.contains(SNAPSHOT) 
                || !uri.contains(MAVEN);
    }

    protected List<Bundle> getBundlesToStart(Collection<Bundle> bundles) {
        // Restart the features service last, regardless of any other consideration
        // so that we don't end up with the service trying to do stuff before we're done
        boolean restart = bundles.remove(bundle);

        SortedMap<Integer, Set<Bundle>> bundlesPerStartLevel = new TreeMap<>();
        for (Bundle bundle : bundles) {
            int sl = bundle.adapt(BundleStartLevel.class).getStartLevel();
            addToMapSet(bundlesPerStartLevel, sl, bundle);
        }
        bundles = bundlesPerStartLevel.remove(bundlesPerStartLevel.firstKey());

        // We hit FELIX-2949 if we don't use the correct order as Felix resolver isn't greedy.
        // In order to minimize that, we make sure we resolve the bundles in the order they
        // are given back by the resolution, meaning that all root bundles (i.e. those that were
        // not flagged as dependencies in features) are started before the others.   This should
        // make sure those important bundles are started first and minimize the problem.
        List<BundleRevision> revs = new ArrayList<>();
        for (Bundle bundle : bundles) {
            revs.add(bundle.adapt(BundleRevision.class));
        }
        List<Bundle> sorted = new ArrayList<>();
        for (BundleRevision rev : RequirementSort.sort(revs)) {
            sorted.add(rev.getBundle());
        }
        if (sorted.isEmpty() && restart) {
            sorted.add(bundle);
        }
        return sorted;
    }

    protected List<Bundle> getBundlesToStop(Collection<Bundle> bundles) {
        SortedMap<Integer, Set<Bundle>> bundlesPerStartLevel = new TreeMap<>();
        for (Bundle bundle : bundles) {
            int sl = bundle.adapt(BundleStartLevel.class).getStartLevel();
            addToMapSet(bundlesPerStartLevel, sl, bundle);
        }
        bundles = bundlesPerStartLevel.get(bundlesPerStartLevel.lastKey());

        List<Bundle> bundlesToDestroy = new ArrayList<>();
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
        String uri = getUri(resource);
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
        Map<Long, Long> bundleChecksums = new HashMap<>();
        Map<Resource, Bundle> resToBnd = new HashMap<>();
        Map<String, RegionDeployment> regions = new HashMap<>();
    }

    static class RegionDeployment {
        List<Resource> toInstall = new ArrayList<>();
        List<Bundle> toDelete = new ArrayList<>();
        Map<Bundle, Resource> toUpdate = new HashMap<>();
    }

}
