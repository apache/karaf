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
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayDeque;
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
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.DeploymentListener;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import java.io.InputStream;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.DownloadManagers;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JacksonUtil;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.util.ThreadUtils;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION;
import static org.apache.karaf.features.internal.model.Feature.VERSION_SEPARATOR;
import static org.apache.karaf.features.internal.service.StateStorage.toStringStringSetMap;
import static org.apache.karaf.features.internal.util.MapUtils.*;

/**
 * A simplified features service implementation that uses a simple feature resolver.
 * <p>
 * Unlike the standard {@link FeaturesServiceImpl}, this implementation does not use
 * OSGi capabilities/requirements resolution (Felix resolver). Instead, it installs
 * features and their bundles in the order they are defined in the feature descriptor,
 * resolving feature dependencies recursively.
 * <p>
 * This is useful for scenarios where:
 * <ul>
 *   <li>OSGi resolver overhead is not needed</li>
 *   <li>Deterministic installation order is desired</li>
 *   <li>Features are self-contained and don't rely on package wiring for resolution</li>
 * </ul>
 */
public class SimpleFeaturesServiceImpl implements FeaturesService, BootManaged, SimpleDeployer.SimpleDeployCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleFeaturesServiceImpl.class);

    private final StateStorage storage;
    private final FeatureRepoFinder featureFinder;
    private final ConfigurationAdmin configurationAdmin;
    private final BundleInstallSupport installSupport;
    private final FeaturesServiceConfig cfg;
    private RepositoryCache repositories;
    private FeaturesProcessor featuresProcessor;

    private final List<FeaturesListener> listeners = new CopyOnWriteArrayIdentityList<>();
    private final List<DeploymentListener> deploymentListeners = new CopyOnWriteArrayIdentityList<>();
    private DeploymentEvent lastDeploymentEvent = DeploymentEvent.DEPLOYMENT_FINISHED;

    private final Object lock = new Object();
    private final State state = new State();
    private final ExecutorService executor;

    private Map<String, Map<String, Feature>> featureCache;

    public SimpleFeaturesServiceImpl(StateStorage storage,
                                     FeatureRepoFinder featureFinder,
                                     ConfigurationAdmin configurationAdmin,
                                     BundleInstallSupport installSupport,
                                     FeaturesServiceConfig cfg) {
        this.storage = storage;
        this.featureFinder = featureFinder;
        this.configurationAdmin = configurationAdmin;
        this.installSupport = installSupport;
        this.featuresProcessor = new FeaturesProcessorImpl(cfg);
        this.repositories = new RepositoryCacheImpl(featuresProcessor);
        this.cfg = cfg;
        this.executor = Executors.newSingleThreadExecutor(ThreadUtils.namedThreadFactory("simple-features"));
        loadState();
    }

    public void stop() {
        this.executor.shutdown();
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
            LOGGER.warn("Error loading SimpleFeaturesService state", e);
        }
    }

    protected void saveState() {
        try {
            synchronized (lock) {
                storage.save(state);
                installSupport.saveDigraph();
            }
        } catch (IOException e) {
            LOGGER.warn("Error saving SimpleFeaturesService state", e);
        }
    }

    @Override
    public boolean isBootDone() {
        synchronized (lock) {
            return state.bootDone.get();
        }
    }

    @Override
    public void bootDone() {
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
            Set<String> repositoriesList;
            Map<String, Set<String>> installedFeatures;
            synchronized (lock) {
                repositoriesList = new TreeSet<>(state.repositories);
                installedFeatures = new TreeMap<>(copy(state.installedFeatures));
            }
            for (String uri : repositoriesList) {
                Repository repository = repositories.create(URI.create(uri), false);
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
    public Feature[] repositoryProvidedFeatures(URI uri) throws Exception {
        Features features;
        if (JacksonUtil.isJson(uri.toURL().toExternalForm())) {
            features = JacksonUtil.unmarshal(uri.toURL().toExternalForm());
        } else {
            features = JaxbUtil.unmarshal(uri.toURL().toExternalForm(), true);
        }
        Feature[] array = new Feature[features.getFeature().size()];
        return features.getFeature().toArray(array);
    }

    @Override
    public void validateRepository(URI uri) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRepositoryUriBlacklisted(URI uri) {
        return featuresProcessor.isRepositoryBlacklisted(uri.toString());
    }

    @Override
    public void addRepository(URI uri) throws Exception {
        addRepository(uri, false);
    }

    @Override
    public void addRepository(URI uri, boolean install) throws Exception {
        Repository repository = repositories.create(uri, true);
        synchronized (lock) {
            repositories.addRepository(repository);
            state.repositories.add(uri.toString());
            featureCache = null;
            saveState();
        }
        callListeners(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, false));
        if (install) {
            Feature[] features = repository.getFeatures();
            Set<String> featureNames = new HashSet<>();
            for (Feature f : features) {
                featureNames.add(f.getName());
            }
            installFeatures(featureNames, EnumSet.noneOf(Option.class));
        }
    }

    @Override
    public void removeRepository(URI uri) throws Exception {
        removeRepository(uri, false);
    }

    @Override
    public void removeRepository(URI uri, boolean uninstall) throws Exception {
        Repository repository;
        synchronized (lock) {
            repository = repositories.getRepository(uri.toString());
            if (repository == null) {
                return;
            }
            if (uninstall) {
                Feature[] features = repository.getFeatures();
                Set<String> featureNames = new HashSet<>();
                for (Feature f : features) {
                    featureNames.add(f.getName());
                }
                uninstallFeatures(featureNames, EnumSet.noneOf(Option.class));
            }
            state.repositories.remove(uri.toString());
            repositories.removeRepository(uri);
            featureCache = null;
            saveState();
        }
        callListeners(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryRemoved, false));
    }

    @Override
    public void restoreRepository(URI uri) throws Exception {
        addRepository(uri);
    }

    @Override
    public Repository[] listRequiredRepositories() throws Exception {
        synchronized (lock) {
            return repositories.listMatchingRepositories(state.repositories);
        }
    }

    @Override
    public Repository[] listRepositories() throws Exception {
        return repositories.listRepositories();
    }

    @Override
    public Repository getRepository(String repoName) throws Exception {
        return repositories.getRepositoryByName(repoName);
    }

    @Override
    public Repository getRepository(URI uri) throws Exception {
        return repositories.getRepository(uri.toString());
    }

    @Override
    public String getRepositoryName(URI uri) throws Exception {
        Repository repo = repositories.getRepository(uri.toString());
        return repo != null ? repo.getName() : null;
    }

    @Override
    public void setResolutionOutputFile(String outputFile) {
        // Not used in simple mode
    }

    @Override
    public void refreshRepositories(Set<URI> uris) throws Exception {
        synchronized (lock) {
            for (URI uri : uris) {
                repositories.removeRepository(uri);
                repositories.addRepository(repositories.create(uri, true));
            }
            featureCache = null;
        }
    }

    @Override
    public void refreshRepository(URI uri) throws Exception {
        refreshRepositories(Collections.singleton(uri));
    }

    //
    // Features support
    //

    @Override
    public Feature[] listFeatures() throws Exception {
        List<Feature> features = new ArrayList<>();
        for (Map<String, Feature> versions : getFeatureCache().values()) {
            features.addAll(versions.values());
        }
        return features.toArray(new Feature[0]);
    }

    @Override
    public Feature[] listRequiredFeatures() throws Exception {
        Set<String> installed;
        synchronized (lock) {
            installed = new HashSet<>();
            for (Set<String> reqs : state.requirements.values()) {
                installed.addAll(reqs);
            }
        }
        List<Feature> result = new ArrayList<>();
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        for (String req : installed) {
            FeatureReq featureReq = FeatureReq.parseRequirement(req);
            if (featureReq != null) {
                featureReq.getMatchingFeatures(allFeatures).forEach(result::add);
            }
        }
        return result.toArray(new Feature[0]);
    }

    @Override
    public Feature[] listInstalledFeatures() throws Exception {
        Set<String> installed;
        synchronized (lock) {
            installed = new HashSet<>();
            for (Set<String> ids : state.installedFeatures.values()) {
                installed.addAll(ids);
            }
        }
        Map<String, Feature> featuresById = getFeaturesById();
        List<Feature> result = new ArrayList<>();
        for (String id : installed) {
            Feature f = featuresById.get(id);
            if (f != null) {
                result.add(f);
            }
        }
        return result.toArray(new Feature[0]);
    }

    @Override
    public boolean isInstalled(Feature f) {
        synchronized (lock) {
            Set<String> installed = state.installedFeatures.get(ROOT_REGION);
            return installed != null && installed.contains(f.getId());
        }
    }

    @Override
    public FeatureState getState(String featureId) {
        synchronized (lock) {
            Map<String, String> regionStates = state.stateFeatures.get(ROOT_REGION);
            if (regionStates != null) {
                String st = regionStates.get(featureId);
                if (st != null) {
                    return FeatureState.valueOf(st);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isRequired(Feature f) {
        String id = new FeatureReq(f).toRequirement();
        synchronized (lock) {
            Set<String> features = state.requirements.get(ROOT_REGION);
            return features != null && features.contains(id);
        }
    }

    @Override
    public Feature getFeature(String name) throws Exception {
        return getFeature(name, null);
    }

    @Override
    public Feature getFeature(String name, String version) throws Exception {
        Feature[] features = getFeatures(name, version);
        return features.length > 0 ? features[0] : null;
    }

    @Override
    public Feature[] getFeatures(String name) throws Exception {
        return getFeatures(name, null);
    }

    @Override
    public Feature[] getFeatures(String name, String version) throws Exception {
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        return new FeatureReq(name, version).getMatchingFeatures(allFeatures).toArray(Feature[]::new);
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
    public void installFeatures(Set<String> featuresIn, String region, EnumSet<Option> options) throws Exception {
        Set<FeatureReq> toInstall = map(featuresIn, FeatureReq::parseNameAndRange);
        State state = copyState();
        Map<String, Set<String>> requires = copy(state.requirements);
        if (region == null || region.isEmpty()) {
            region = ROOT_REGION;
        }
        Set<String> requirements = requires.computeIfAbsent(region, k -> new HashSet<>());
        Set<FeatureReq> existingFeatures = map(requirements, FeatureReq::parseRequirement);

        Set<FeatureReq> toAdd = computeFeaturesToAdd(options, toInstall);
        toAdd.forEach(f -> {
            if (f.isBlacklisted()) {
                print("Skipping blacklisted feature: " + f, options.contains(Option.Verbose));
            } else {
                requirements.add(f.toRequirement());
            }
        });
        List<FeatureReq> notBlacklisted = toAdd.stream()
                .filter(fr -> !fr.isBlacklisted()).collect(Collectors.toList());
        if (!notBlacklisted.isEmpty()) {
            print("Adding features: " + join(notBlacklisted), options.contains(Option.Verbose));
        }

        if (options.contains(Option.Upgrade)) {
            Set<FeatureReq> toRemove = computeFeaturesToRemoveOnUpdate(toAdd, existingFeatures);
            toRemove.forEach(f -> requirements.remove(f.toRequirement()));
            if (!toRemove.isEmpty()) {
                print("Removing features: " + join(toRemove), options.contains(Option.Verbose));
            }
        }

        doProvisionInThread(requires, state, options);
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
    public void uninstallFeature(String name, String version) throws Exception {
        uninstallFeature(getId(name, version), EnumSet.noneOf(Option.class));
    }

    @Override
    public void uninstallFeature(String name, String version, EnumSet<Option> options) throws Exception {
        uninstallFeature(getId(name, version), options);
    }

    @Override
    public void uninstallFeatures(Set<String> features, EnumSet<Option> options) throws Exception {
        uninstallFeatures(features, ROOT_REGION, options);
    }

    @Override
    public void uninstallFeatures(Set<String> featuresIn, String region, EnumSet<Option> options) throws Exception {
        Set<FeatureReq> featureReqs = map(featuresIn, FeatureReq::parseNameAndRange);
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        if (region == null || region.isEmpty()) {
            region = ROOT_REGION;
        }
        Set<String> requirements = required.computeIfAbsent(region, k -> new HashSet<>());
        Set<FeatureReq> existingFeatures = map(requirements, FeatureReq::parseRequirement);
        Set<FeatureReq> featuresToRemove = new HashSet<>();
        for (FeatureReq featureReq : featureReqs) {
            Collection<FeatureReq> toRemove = featureReq.getMatchingRequirements(existingFeatures);
            if (toRemove.isEmpty()) {
                throw new IllegalArgumentException("Feature named '" + featureReq + "' is not installed");
            }
            featuresToRemove.addAll(toRemove);
        }
        print("Removing features: " + join(featuresToRemove), options.contains(Option.Verbose));
        featuresToRemove.forEach(f -> requirements.remove(f.toRequirement()));
        if (requirements.isEmpty()) {
            required.remove(region);
        }
        doProvisionInThread(required, state, options);
    }

    @Override
    public void updateFeaturesState(Map<String, Map<String, FeatureState>> stateChanges, EnumSet<Option> options) throws Exception {
        // In simple mode, state changes are not supported via resolver
        // Just update the state directly
        synchronized (lock) {
            for (Map.Entry<String, Map<String, FeatureState>> regionEntry : stateChanges.entrySet()) {
                Map<String, String> regionStates = state.stateFeatures.computeIfAbsent(
                        regionEntry.getKey(), k -> new HashMap<>());
                for (Map.Entry<String, FeatureState> featureEntry : regionEntry.getValue().entrySet()) {
                    regionStates.put(featureEntry.getKey(), featureEntry.getValue().name());
                }
            }
            saveState();
        }
    }

    @Override
    public void addRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        add(required, requirements);
        doProvisionInThread(required, state, options);
    }

    @Override
    public void removeRequirements(Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State state = copyState();
        Map<String, Set<String>> required = copy(state.requirements);
        remove(required, requirements);
        doProvisionInThread(required, state, options);
    }

    @Override
    public void updateReposAndRequirements(Set<URI> repos, Map<String, Set<String>> requirements, EnumSet<Option> options) throws Exception {
        State stateCopy;
        synchronized (lock) {
            Set<String> reps = map(repos, URI::toString);
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
        doProvisionInThread(requirements, stateCopy, options);
    }

    @Override
    public Repository createRepository(URI uri) throws Exception {
        return repositories.create(uri, true);
    }

    @Override
    public Map<String, Set<String>> listRequirements() {
        synchronized (lock) {
            return copy(this.state.requirements);
        }
    }

    @Override
    public String getFeatureXml(Feature feature) {
        try {
            StringWriter sw = new StringWriter();
            Features r = new Features();
            r.getFeature().add((org.apache.karaf.features.internal.model.Feature) feature);
            JaxbUtil.marshal(r, sw);
            String[] strs = sw.toString().split("\n");
            StringJoiner joiner = new StringJoiner("\n");
            for (int i = 2; i < strs.length - 1; i++) {
                joiner.add(strs[i]);
            }
            return joiner.toString();
        } catch (JAXBException e) {
            return null;
        }
    }

    @Override
    public void refreshFeatures(EnumSet<Option> options) throws Exception {
        Set<URI> uris = new LinkedHashSet<>();
        for (Repository r : this.repositories.listRepositories()) {
            uris.add(r.getURI());
        }
        this.refreshRepositories(uris);
        this.featuresProcessor = new FeaturesProcessorImpl(cfg);
        this.repositories = new RepositoryCacheImpl(featuresProcessor);

        State state = copyState();
        doProvisionInThread(state.requirements, state, options);
    }

    //
    // Internal helpers
    //

    private String getId(String name, String version) {
        return version != null ? name + VERSION_SEPARATOR + version : name;
    }

    private State copyState() {
        synchronized (lock) {
            return this.state.copy();
        }
    }

    protected Map<String, Map<String, Feature>> getFeatureCache() throws Exception {
        if (featureCache == null) {
            Map<String, Map<String, Feature>> map = new HashMap<>();
            Set<String> uris;
            synchronized (lock) {
                uris = new TreeSet<>(state.repositories);
            }
            // Load features from all repositories (and their transitive references)
            Queue<String> toProcess = new ArrayDeque<>(uris);
            Set<String> processed = new HashSet<>();
            while (!toProcess.isEmpty()) {
                String uri = toProcess.poll();
                if (processed.contains(uri)) {
                    continue;
                }
                processed.add(uri);
                try {
                    Repository repo = repositories.getRepository(uri);
                    if (repo == null) {
                        repo = repositories.create(URI.create(uri), false);
                        repositories.addRepository(repo);
                    }
                    for (Feature f : repo.getFeatures()) {
                        map.computeIfAbsent(f.getName(), k -> new HashMap<>())
                                .put(f.getVersion(), f);
                    }
                    for (URI repoUri : repo.getRepositories()) {
                        toProcess.add(repoUri.toString());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error loading repository {}", uri, e);
                }
            }
            featureCache = map;
        }
        return featureCache;
    }

    private Map<String, Feature> getFeaturesById() throws Exception {
        Map<String, Feature> result = new HashMap<>();
        for (Map<String, Feature> versions : getFeatureCache().values()) {
            for (Feature f : versions.values()) {
                result.put(f.getId(), f);
            }
        }
        return result;
    }

    private Set<FeatureReq> computeFeaturesToAdd(EnumSet<Option> options,
                                                  Set<FeatureReq> toInstall) throws Exception {
        Map<String, Map<String, Feature>> allFeatures = getFeatureCache();
        Set<FeatureReq> toAdd = new HashSet<>();
        for (FeatureReq featureReq : toInstall) {
            Collection<Feature> matching = featureReq.getMatchingFeatures(allFeatures).collect(toSet());
            for (Feature f : matching) {
                toAdd.add(new FeatureReq(f));
            }
            if (matching.isEmpty() && !options.contains(Option.NoFailOnFeatureNotFound)) {
                throw new IllegalArgumentException("No matching features for " + featureReq);
            }
        }
        return toAdd;
    }

    private Set<FeatureReq> computeFeaturesToRemoveOnUpdate(Set<FeatureReq> featuresToAdd,
                                                             Set<FeatureReq> existingFeatures) {
        Set<String> namedToAdd = map(featuresToAdd, FeatureReq::getName);
        return filter(existingFeatures, f -> namedToAdd.contains(f.getName()) && !featuresToAdd.contains(f));
    }

    private void doProvisionInThread(final Map<String, Set<String>> requirements,
                                     final State state,
                                     final EnumSet<Option> options) throws Exception {
        try {
            Future<Object> future = executor.submit(() -> {
                doProvision(requirements, state, options);
                return null;
            });
            future.get();
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

    private void doProvision(Map<String, Set<String>> requirements,
                             State state,
                             EnumSet<Option> options) throws Exception {
        try (DownloadManager manager = createDownloadManager()) {
            Map<String, Feature> featuresById = getFeaturesById();
            Map<String, Map<String, Feature>> featuresByName = getFeatureCache();
            SimpleDeployer deployer = new SimpleDeployer(manager, this);
            deployer.deploy(featuresById, featuresByName, requirements, state, options);
        }
    }

    protected DownloadManager createDownloadManager() throws IOException {
        Dictionary<String, String> props = getMavenConfig();
        MavenResolver resolver = MavenResolvers.createMavenResolver(props, "org.ops4j.pax.url.mvn");
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(cfg.downloadThreads, ThreadUtils.namedThreadFactory("downloader"));
        executor.setMaximumPoolSize(cfg.downloadThreads);
        return DownloadManagers.createDownloadManager(resolver, executor, cfg.scheduleDelay, cfg.scheduleMaxRun);
    }

    private Dictionary<String, String> getMavenConfig() throws IOException {
        Hashtable<String, String> props = new Hashtable<>();
        if (configurationAdmin != null) {
            Configuration config = configurationAdmin.getConfiguration("org.ops4j.pax.url.mvn", null);
            if (config != null) {
                Dictionary<String, Object> cfg = config.getProcessedProperties(null);
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

    //
    // SimpleDeployCallback implementation
    //

    @Override
    public void print(String message, boolean verbose) {
        LOGGER.info(message);
        if (verbose) {
            System.out.println(message);
        }
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
    public BundleInstallSupport.FrameworkInfo getInfo() {
        return installSupport.getInfo();
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
    public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
        installSupport.refreshPackages(bundles);
    }

    @Override
    public void installConfigs(Feature feature) throws IOException {
        try {
            installSupport.installConfigs(feature);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Error installing configs for " + feature.getId(), e);
        }
    }

    @Override
    public void deleteConfigs(Feature feature) throws IOException {
        try {
            installSupport.deleteConfigs(feature);
        } catch (InvalidSyntaxException e) {
            throw new IOException("Error deleting configs for " + feature.getId(), e);
        }
    }

    @Override
    public void installLibraries(Feature feature) throws IOException {
        installSupport.installLibraries(feature);
    }

    private String join(Collection<FeatureReq> reqs) {
        return reqs.stream().map(FeatureReq::toString).collect(Collectors.joining(","));
    }

}
