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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.ConfigInfo;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.region.SubsystemResolver;
import org.apache.karaf.features.internal.region.SubsystemResolverCallback;
import org.apache.karaf.features.internal.resolver.FeatureResource;
import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.apache.karaf.features.internal.resolver.ResourceUtils;
import org.apache.karaf.features.internal.util.ChecksumUtils;
import org.apache.karaf.features.internal.util.Macro;
import org.apache.karaf.features.internal.util.MapUtils;
import org.apache.karaf.features.internal.util.MultiException;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.FeaturesService.ROOT_REGION;
import static org.apache.karaf.features.FeaturesService.UPDATEABLE_URIS;
import static org.apache.karaf.features.internal.resolver.ResolverUtil.getSymbolicName;
import static org.apache.karaf.features.internal.resolver.ResolverUtil.getVersion;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_SUBSYSTEM;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getFeatureId;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getType;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getUri;
import static org.apache.karaf.features.internal.util.MapUtils.add;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.apache.karaf.features.internal.util.MapUtils.apply;
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
import static org.osgi.framework.namespace.HostNamespace.HOST_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.TYPE_BUNDLE;

public class Deployer {

    /**
     * Interface through which {@link Deployer} interacts with OSGi framework.
     */
    public interface DeployCallback extends SubsystemResolverCallback {
        void print(String message, boolean verbose);
        void saveState(State state);
        void persistResolveRequest(DeploymentRequest request) throws IOException;
        void callListeners(DeploymentEvent deployEvent);
        void callListeners(FeatureEvent featureEvent);

        Bundle installBundle(String region, String uri, InputStream is) throws BundleException;
        void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException;
        void uninstall(Bundle bundle) throws BundleException;
        void startBundle(Bundle bundle) throws BundleException;
        void stopBundle(Bundle bundle, int options) throws BundleException;
        void setBundleStartLevel(Bundle bundle, int startLevel);
        void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring,
                            Map<Resource, Bundle> resToBnd);
        void refreshPackages(Collection<Bundle> bundles) throws InterruptedException;
        void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies,
                            Map<String, Set<Long>> bundles) throws BundleException, InvalidSyntaxException;
        void installConfigs(Feature feature) throws IOException, InvalidSyntaxException;
        void deleteConfigs(Feature feature) throws IOException, InvalidSyntaxException;
        void installLibraries(Feature feature) throws IOException;
    }

    @SuppressWarnings("serial")
    public static class CircularPrerequisiteException extends Exception {
        private final Set<String> prereqs;

        public CircularPrerequisiteException(Set<String> prereqs) {
            super(prereqs.toString());
            this.prereqs = prereqs;
        }

        public Set<String> getPrereqs() {
            return prereqs;
        }
    }

    @SuppressWarnings("serial")
    public static class PartialDeploymentException extends Exception {
        private final Set<String> missing;

        public PartialDeploymentException(Set<String> missing) {
            this.missing = missing;
        }

        public Set<String> getMissing() {
            return missing;
        }
    }

    /**
     * <p>Representation of the state of system from the point of view of <em>installed bundles</em>
     * and <em>available features</em></p>
     */
    public static class DeploymentState {
        // part of the deployment state related to features service

        /** Current {@link State} of features service */
        public State state;

        // part of the deployment state related to low level OSGi framework (bundles, no regions)

        /** A {@link Bundle} providing {@link FeaturesService} */
        public Bundle serviceBundle;
        /** A {@link Bundle} providing {@link org.osgi.service.cm.ConfigurationAdmin} service */
        public Bundle configadminBundle;
        /** {@link org.osgi.framework.startlevel.FrameworkStartLevel#getInitialBundleStartLevel()} */
        public int initialBundleStartLevel;
        /** {@link org.osgi.framework.startlevel.FrameworkStartLevel#getStartLevel()} */
        public int currentStartLevel;
        /** bundle-id -&gt; bundle for all currently installed bundles */
        public Map<Long, Bundle> bundles;

        // part of the deployment state related to all available features

        /** feature-name -&gt; list of features for different versions for all available features (not only installed) */
        private Map<String, List<Feature>> features;
        /** feature-id -&gt; feature (not only installed) */
        private Map<String, Feature> featuresById;

        // part of the deployment state related to regions

        /** region-name -&gt; ids for bundles installed in region (see {@link State#managedBundles}) */
        public Map<String, Set<Long>> bundlesPerRegion;
        /** region-name -&gt; connected, filtered, region-name -&gt; filter-namespace -&gt; filters */
        public Map<String, Map<String, Map<String, Set<String>>>> filtersPerRegion;

        /**
         * Returns all features indexed by their name. For each name we have collection of {@link Feature features}
         * for different versions.
         * @return
         */
        public Map<String, List<Feature>> featuresByName() {
            return features;
        }

        /**
         * Returns all features indexed by their id.
         * @return
         */
        public Map<String, Feature> featuresById() {
            return featuresById;
        }

        /**
         * Sets a list of features and stores it as map of features where the key is <code>name</code> and value is a
         * list of features with different versions.
         * @param featuresList
         */
        public void partitionFeatures(Collection<Feature> featuresList) {
            features = new HashMap<>();
            featuresById = new HashMap<>();
            for (Feature feature : featuresList) {
                features.computeIfAbsent(feature.getName(), name -> new ArrayList<>()).add(feature);
                featuresById.put(feature.getId(), feature);
            }
        }
    }

    /**
     * <p>A request to change current {@link DeploymentState} of system</p>
     * <p>{@link #requirements} specify target set of system requirements. If new features are installed,
     * requirements should include currently installed features and new ones. If features are being uninstalled,
     * requirements should include currently installed features minus the ones that are removed.</p>
     */
    public static class DeploymentRequest {
        /** A bnd macro that changes feature version into a version range. */
        public String featureResolutionRange;
        /** Indication of how to handle requirements from <code>osgi.service</code> namespace */
        public FeaturesService.ServiceRequirementsBehavior serviceRequirements;
        /** A bnd macro to find update'able version range for bundle versions (e.g., to determine whether to install or update a bundle */
        public String bundleUpdateRange;
        /** Indication of when to update bundles (or leave them as they are currently installed) */
        public FeaturesService.SnapshotUpdateBehavior updateSnaphots;
        /** Indication if deployer refresh automatically bundle (true), or not (false) */
        public boolean autoRefresh;

        /**
         * Additional {@link Repository} that'll be used to resolve unresolved, non-optional requirements if
         * they're not resolved against current
         */
        public Repository globalRepository;

        /** Target/desired set of requirements per region */
        public Map<String, Set<String>> requirements;
        /** Target/desired set of features state per region */
        public Map<String, Map<String, FeatureState>> stateChanges;
        /** Deployment options */
        public EnumSet<FeaturesService.Option> options;

        /** File to store result of deployment */
        public String outputFile;

        /**
         * Prepare standard, empty DeploymentRequest, where feature versions are taken literally (no ranges)
         * and bundle updates use <em>natural</em> range to determine between install and update (update on micro
         * digit in version, e.g., <code>2.1.0</code> -&gt; <code>2.1.2</code>, but not <code>2.1.2</code> -&gt;
         * <code>2.2.0</code>).
         * @return
         */
        public static DeploymentRequest defaultDeploymentRequest() {
            DeploymentRequest request = new DeploymentRequest();
            request.bundleUpdateRange = FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE;
            request.featureResolutionRange = FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE;
            request.autoRefresh = FeaturesService.DEFAULT_AUTO_REFRESH;
            request.serviceRequirements = FeaturesService.ServiceRequirementsBehavior.Default;
            request.requirements = new HashMap<>();
            request.stateChanges = new HashMap<>();
            request.options = EnumSet.noneOf(FeaturesService.Option.class);
            return request;
        }
    }

    /**
     * Deployment information for all regions
     */
    static class Deployment {
        Map<Long, Long> bundleChecksums = new HashMap<>();
        Map<Resource, Bundle> resToBnd = new HashMap<>();
        Map<String, RegionDeployment> regions = new HashMap<>();
    }

    /**
     * Deployment information for single region
     */
    static class RegionDeployment {
        /** new {@link Resource resources} to install */
        List<Resource> toInstall = new ArrayList<>();
        /** existing {@link Bundle bundles} to remove */
        List<Bundle> toDelete = new ArrayList<>();
        /** existing {@link Bundle bundles} to update using new {@link Resource resources} */
        Map<Bundle, Resource> toUpdate = new HashMap<>();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Deployer.class);

    private final DownloadManager manager;
    private final Resolver resolver;
    private final DeployCallback callback;

    public Deployer(DownloadManager manager, Resolver resolver, DeployCallback callback) {
        this.manager = manager;
        this.resolver = resolver;
        this.callback = callback;
    }

    /**
     * Performs full deployment - with prerequisites
     *
     * @param dstate  deployment state
     * @param request deployment request
     * @throws Exception in case of deployment failure.
     */
    public void deployFully(DeploymentState dstate, DeploymentRequest request) throws Exception {
        Set<String> prereqs = new HashSet<>();
        while (true) {
            try {
                deploy(dstate, request);
                break;
            } catch (Deployer.PartialDeploymentException e) {
                if (!prereqs.containsAll(e.getMissing())) {
                    prereqs.addAll(e.getMissing());
                } else {
                    throw new Exception("Deployment aborted due to loop in missing prerequisites: " + e.getMissing());
                }
            }
        }
    }

    /**
     * Perform a deployment.
     *
     * @param dstate  deployment state
     * @param request deployment request
     * @throws Exception in case of deployment failure.
     */
    public void deploy(DeploymentState dstate, DeploymentRequest request) throws Exception {
        boolean noRefreshUnmanaged = request.options.contains(FeaturesService.Option.NoAutoRefreshUnmanagedBundles);
        boolean noRefreshManaged = request.options.contains(FeaturesService.Option.NoAutoRefreshManagedBundles);
        boolean noRefresh = request.options.contains(FeaturesService.Option.NoAutoRefreshBundles);
        if (!request.autoRefresh) {
            noRefreshUnmanaged = request.autoRefresh;
            noRefreshManaged = request.autoRefresh;
            noRefresh = request.autoRefresh;
        }
        boolean noStart = request.options.contains(FeaturesService.Option.NoAutoStartBundles);
        boolean verbose = request.options.contains(FeaturesService.Option.Verbose);
        boolean simulate = request.options.contains(FeaturesService.Option.Simulate);
        boolean noManageBundles = request.options.contains(FeaturesService.Option.NoAutoManageBundles);
        boolean showWiring = request.options.contains(FeaturesService.Option.DisplayFeaturesWiring)
                    || request.options.contains(FeaturesService.Option.DisplayAllWiring);
        boolean showFeaturesWiringOnly = request.options.contains(FeaturesService.Option.DisplayFeaturesWiring)
                    && !request.options.contains(FeaturesService.Option.DisplayAllWiring);
        boolean deleteConfigurations = request.options.contains(FeaturesService.Option.DeleteConfigurations);

        // TODO: add an option to unmanage bundles instead of uninstalling those

        // current managed bundles per region, as known by o.a.k.features.internal.service.FeaturesServiceImpl.state
        Map<String, Set<Long>> managedBundles = copy(dstate.state.managedBundles);

        // current not managed (by FeaturesService state) bundles per region, as known by o.a.k.features.internal.service.BundleInstallSupportImpl.digraph
        // "unmanaged" means "not installed via features service"
        Map<String, Set<Long>> diff = diff(dstate.bundlesPerRegion, dstate.state.managedBundles);
        Map<String, Set<Bundle>> unmanagedBundles = apply(diff, map(dstate.bundles));

        // Use Subsystem and Felix resolver
        SubsystemResolver resolver = new SubsystemResolver(this.resolver, manager);
        resolver.setDeployCallback(callback);
        Map<String, Set<BundleRevision>> unmanagedBundleRevisions = apply(unmanagedBundles, adapt(BundleRevision.class));

        // preparation - creating OSGi resources with reqs and caps for regions and features
        resolver.prepare(dstate.featuresByName(), request.requirements, unmanagedBundleRevisions);

        // if some features have prerequisites, we have to deploy them first - this method may throw Exception
        // to start another cycle of deployment
        handlePrerequisites(dstate, request, resolver);

        // when there are no more prerequisites, we can resolve Subsystems and Features using Felix resolver
        // Subsystem resolver will have then full information about new bundles and bundle updates or removals
        // per region
        resolver.resolve(
                request.featureResolutionRange,
                request.serviceRequirements,
                request.globalRepository,
                request.outputFile);

        Map<String, StreamProvider> providers = resolver.getProviders();
        Map<String, Set<Resource>> featuresPerRegion = resolver.getFeaturesPerRegions();
        Map<String, Set<String>> installedFeatures = apply(featuresPerRegion, featureId());
        // changes to current state - added and removed features
        Map<String, Set<String>> newFeatures = diff(installedFeatures, dstate.state.installedFeatures);
        Map<String, Set<String>> delFeatures = diff(dstate.state.installedFeatures, installedFeatures);

        //
        // Compute requested features state
        //
        Map<String, Map<String, String>> stateFeatures = copy(dstate.state.stateFeatures);
        for (Map.Entry<String, Set<String>> entry : delFeatures.entrySet()) {
            Map<String, String> map = stateFeatures.get(entry.getKey());
            if (map != null) {
                map.keySet().removeAll(entry.getValue());
                if (map.isEmpty()) {
                    stateFeatures.remove(entry.getKey());
                }
            }
        }
        for (Map.Entry<String, Map<String, FeatureState>> entry1 : request.stateChanges.entrySet()) {
            String region = entry1.getKey();
            Map<String, String> regionStates = stateFeatures.get(region);
            if (regionStates != null) {
                for (Map.Entry<String, FeatureState> entry2 : entry1.getValue().entrySet()) {
                    String feature = entry2.getKey();
                    if (regionStates.containsKey(feature)) {
                        regionStates.put(feature, entry2.getValue().name());
                    }
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : newFeatures.entrySet()) {
            for (String feature : entry.getValue()) {
                Map<String, String> map = stateFeatures.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
                map.put(feature, noStart ? FeatureState.Installed.name() : FeatureState.Started.name());
            }
        }

        // Compute information for each bundle (region -> location -> BundleInfo)
        Map<String, Map<String, BundleInfo>> bundleInfos = resolver.getBundleInfos();

        //
        // Compute deployment
        //
        Deployer.Deployment deployment = computeDeployment(dstate, request, resolver);

        //
        // Compute the set of bundles to refresh
        //
        Map<Bundle, String> toRefresh = new TreeMap<>(new BundleComparator()); // sort is only used for display
        for (Deployer.RegionDeployment regionDeployment : deployment.regions.values()) {
            for (Bundle b : regionDeployment.toDelete) {
                toRefresh.put(b, "Bundle will be uninstalled");
            }
            for (Bundle b : regionDeployment.toUpdate.keySet()) {
                toRefresh.put(b, "Bundle will be updated");
            }
        }
        if (!noRefreshManaged) {
            computeBundlesToRefresh(toRefresh, dstate.bundles.values(), deployment.resToBnd, resolver.getWiring());
        }
        if (noRefreshUnmanaged) {
            toRefresh.keySet().removeAll(flatten(unmanagedBundles));
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
            if (resolver.getWiring() != null) {
                for (List<Wire> wires : resolver.getWiring().values()) {
                    for (Wire wire : wires) {
                        if (features.contains(wire.getRequirer()) && unmanaged.contains(wire.getProvider())) {
                            requested.add(wire.getProvider());
                        }
                    }
                }
            }
            // Now, we know which bundles are completely unmanaged
            unmanaged.removeAll(requested);
            // Check if bundles have wires from really unmanaged bundles
            if (resolver.getWiring() != null) {
                for (List<Wire> wires : resolver.getWiring().values()) {
                    for (Wire wire : wires) {
                        if (requested.contains(wire.getProvider()) && unmanaged.contains(wire.getRequirer())) {
                            requested.remove(wire.getProvider());
                        }
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

        Set<Bundle> toStart = new HashSet<>();
        Set<Bundle> toResolve = new HashSet<>();
        Set<Bundle> toStop = new HashSet<>();

        //
        // Compute bundle states
        //
        Map<Resource, FeatureState> states = new HashMap<>();
        // Find all features state
        Map<Resource, FeatureState> featuresState = new HashMap<>();
        Map<Resource, Set<Resource>> conditionals = new HashMap<>();
        for (Map.Entry<String, Set<Resource>> entry : resolver.getFeaturesPerRegions().entrySet()) {
            String region = entry.getKey();
            Map<String, String> fss = stateFeatures.get(region);
            for (Resource feature : entry.getValue()) {
                Set<Resource> conditions = new HashSet<>();
                for (Wire wire : resolver.getWiring().get(feature)) {
                    if (IDENTITY_NAMESPACE.equals(wire.getRequirement().getNamespace()) &&
                            FeatureResource.CONDITIONAL_TRUE.equals(wire.getRequirement().getDirectives().get(FeatureResource.REQUIREMENT_CONDITIONAL_DIRECTIVE))) {
                        conditions.add(wire.getProvider());
                    }
                }
                if (conditions.isEmpty()) {
                    String fs = fss.get(getFeatureId(feature));
                    featuresState.put(feature, FeatureState.valueOf(fs));
                } else {
                    conditionals.put(feature, conditions);
                }
            }
        }
        // Compute conditional features state
        for (Resource feature : conditionals.keySet()) {
            FeatureState state = null;
            for (Resource cond : conditionals.get(feature)) {
                FeatureState s = featuresState.get(cond);
                if (state == null) {
                    state = s;
                } else if (state == FeatureState.Started && s == FeatureState.Resolved) {
                    state = FeatureState.Resolved;
                }
            }
            featuresState.put(feature, state);
        }
        // Propagate Resolved state
        for (Resource feature : featuresState.keySet()) {
            if (featuresState.get(feature) == FeatureState.Resolved) {
                propagateState(states, feature, FeatureState.Resolved, resolver);
            }
        }
        // Propagate Started state
        for (Resource feature : featuresState.keySet()) {
            if (featuresState.get(feature) == FeatureState.Started) {
                propagateState(states, feature, FeatureState.Started, resolver);
            }
        }
        // Put default Started state for other bundles if start attribute is true
        for (Resource resource : resolver.getBundles().keySet()) {
            BundleInfo bundleInfo = null;
            for (Map.Entry<String, Map<String, BundleInfo>> bis : resolver.getBundleInfos().entrySet()) {
                bundleInfo = bis.getValue().get(getUri(resource));
            }
            Bundle bundle = deployment.resToBnd.get(resource);
            if (bundle == null) {
                // bundle is not present, it's provided by feature
                // we are using bundleInfo and start flag
                if (bundleInfo != null && bundleInfo.isStart() && !noStart) {
                    states.put(resource, FeatureState.Started);
                } else {
                    states.put(resource, FeatureState.Resolved);
                }
            }
        }
        // Only keep bundles resources
        states.keySet().retainAll(resolver.getBundles().keySet());
        //
        // Compute bundles to start, stop and resolve
        //
        for (Map.Entry<Resource, FeatureState> entry : states.entrySet()) {
            Bundle bundle = deployment.resToBnd.get(entry.getKey());
            if (bundle != null) {
                switch (entry.getValue()) {
                case Started:
                    toResolve.add(bundle);
                    toStart.add(bundle);
                    break;
                case Resolved:
                    toResolve.add(bundle);
                    toStop.add(bundle);
                    break;
                }
            }
        }
        //
        // Compute bundle all start levels and start levels to update
        //
        Map<Resource, Integer> startLevels = new HashMap<>();
        Map<Bundle, Integer> toUpdateStartLevel = new HashMap<>();
        for (Map.Entry<String, Set<Resource>> entry : resolver.getBundlesPerRegions().entrySet()) {
            String region = entry.getKey();
            for (Resource resource : entry.getValue()) {
                BundleInfo bi = bundleInfos.get(region).get(getUri(resource));
                if (bi != null) {
                    int sl = bi.getStartLevel() > 0 ? bi.getStartLevel() : dstate.initialBundleStartLevel;
                    startLevels.put(resource, sl);
                    Bundle bundle = deployment.resToBnd.get(resource);
                    if (bundle != null) {
                        int curSl = bundle.adapt(BundleStartLevel.class).getStartLevel();
                        if (sl != curSl) {
                            toUpdateStartLevel.put(bundle, sl);
                            if (sl > dstate.currentStartLevel) {
                                toStop.add(bundle);
                            }
                        }
                    }
                }
            }
        }

        //
        // Log wiring
        //
        if (showWiring) {
            logWiring(resolver.getWiring(), showFeaturesWiringOnly);
        }

        //
        // Log deployment
        //
        logDeployment(deployment, verbose);

        if (simulate) {
            if (!noRefresh && !toRefresh.isEmpty()) {
                print("  Bundles to refresh:", verbose);
                for (Map.Entry<Bundle, String> entry : toRefresh.entrySet()) {
                    Bundle bundle = entry.getKey();
                    print("    " + bundle.getSymbolicName() + "/" + bundle.getVersion() + " (" + entry.getValue() + ")", verbose);
                }
            }
            if (!toManage.isEmpty()) {
                print("  Managing bundle:", verbose);
                for (Bundle bundle : toManage) {
                    print("    " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                }
            }
            if (deleteConfigurations) {
                print(" Configurations to delete:", verbose);
                for (Map.Entry<String, Set<String>> entry : delFeatures.entrySet()) {
                    for (String name : entry.getValue()) {
                        Feature feature = dstate.featuresById.get(name);
                        if (feature != null) {
                            for (ConfigInfo configInfo : feature.getConfigurations()) {
                                print("    " + configInfo.getName(), verbose);
                            }
                        }
                    }
                }
                print(" Configuration Files to delete:", verbose);
                for (Map.Entry<String, Set<String>> entry : delFeatures.entrySet()) {
                    for (String name : entry.getValue()) {
                        Feature feature = dstate.featuresById.get(name);
                        if (feature != null) {
                            for (ConfigFileInfo configFileInfo : feature.getConfigurationFiles()) {
                                print("    " + configFileInfo.getFinalname(), verbose);
                            }
                        }
                    }
                }
            }
            return;
        }

        //
        // Execute deployment
        //
        // #1: stop bundles that needs to be updated or uninstalled or refreshed in order
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
        Bundle serviceBundle = dstate.serviceBundle;
        Bundle configadminBundle = dstate.configadminBundle;
        //
        // Handle updates on the FeaturesService bundle
        //
        Deployer.RegionDeployment rootRegionDeployment = deployment.regions.get(ROOT_REGION);
        // We don't support uninstalling the bundle
        if (rootRegionDeployment != null && rootRegionDeployment.toDelete.contains(serviceBundle)) {
            throw new UnsupportedOperationException("Uninstalling the FeaturesService bundle is not supported");
        }

        // If the bundle needs to be updated, do the following:
        //  - persist the request to indicate the resolution must be continued after restart
        //  - update the checksum and save the state
        //  - compute bundles wired to the FeaturesService bundle that will be refreshed
        //  - stop the bundle
        //  - update the bundle
        //  - refresh wired bundles
        //  - start the bundle
        //  - exit
        // When restarting, the resolution will be attempted again
        if (rootRegionDeployment != null && rootRegionDeployment.toUpdate.containsKey(serviceBundle)) {
            callback.persistResolveRequest(request);
            // If the bundle is updated because of a different checksum,
            // save the new checksum persistently
            if (deployment.bundleChecksums.containsKey(serviceBundle.getBundleId())) {
                State state = dstate.state.copy();
                state.bundleChecksums.put(serviceBundle.getBundleId(),
                                          deployment.bundleChecksums.get(serviceBundle.getBundleId()));
                callback.saveState(state);
            }
            Resource resource = rootRegionDeployment.toUpdate.get(serviceBundle);
            String uri = getUri(resource);
            print("The FeaturesService bundle needs is being updated with " + uri, verbose);
            toRefresh.clear();
            toRefresh.put(serviceBundle, "FeaturesService bundle is being updated");
            computeBundlesToRefresh(toRefresh,
                    dstate.bundles.values(),
                    Collections.emptyMap(),
                    Collections.emptyMap());
            callback.stopBundle(serviceBundle, STOP_TRANSIENT);
            try (
                    InputStream is = getBundleInputStream(resource, providers)
            ) {
                callback.updateBundle(serviceBundle, uri, is);
            }
            callback.refreshPackages(toRefresh.keySet());
            callback.startBundle(serviceBundle);
            return;
        }

        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);

        //
        // Perform bundle operations
        //

        //
        // Stop bundles by chunks
        //
        for (Deployer.RegionDeployment regionDeployment : deployment.regions.values()) {
            toStop.addAll(regionDeployment.toUpdate.keySet());
            toStop.addAll(regionDeployment.toDelete);
        }
        if (!noRefresh) {
            Set<Bundle> toRefreshToStopEarly = new HashSet<>(toRefresh.keySet());
            toRefreshToStopEarly.remove(dstate.serviceBundle);
            toRefreshToStopEarly.remove(dstate.configadminBundle);
            toRefreshToStopEarly.remove(dstate.bundles.get(0L));
            toStop.addAll(toRefreshToStopEarly);
            toStart.addAll(toRefreshToStopEarly);
        }
        removeFragmentsAndBundlesInState(toStop, UNINSTALLED | RESOLVED | STOPPING | STARTING);
        if (!toStop.isEmpty()) {
            print("Stopping bundles:", verbose);
            while (!toStop.isEmpty()) {
                List<Bundle> bs = getBundlesToStop(toStop);
                for (Bundle bundle : bs) {
                    print("  " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                    // If the bundle start level will be changed, stop it persistently to
                    // avoid a restart when the start level is actually changed
                    callback.stopBundle(bundle, toUpdateStartLevel.containsKey(bundle) ? 0 : STOP_TRANSIENT);
                    toStop.remove(bundle);
                }
            }
        }

        //
        // Delete bundles
        //
        boolean hasToDelete = false;
        for (Deployer.RegionDeployment regionDeployment : deployment.regions.values()) {
            if (hasToDelete = !regionDeployment.toDelete.isEmpty()) {
                break;
            }
        }
        if (hasToDelete) {
            print("Uninstalling bundles:", verbose);
            for (Map.Entry<String, Deployer.RegionDeployment> entry : deployment.regions.entrySet()) {
                String name = entry.getKey();
                Deployer.RegionDeployment regionDeployment = entry.getValue();
                for (Bundle bundle : regionDeployment.toDelete) {
                    print("  " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                    callback.uninstall(bundle);
                    removeFromMapSet(managedBundles, name, bundle.getBundleId());
                }
            }
        }

        //
        // Update regions
        //
        {
            // Add bundles
            Map<String, Set<Long>> bundles = new HashMap<>();
            add(bundles, apply(unmanagedBundles, bundleId()));
            add(bundles, managedBundles);
            // Compute policies
            RegionDigraph computedDigraph = resolver.getFlatDigraph();
            Map<String, Map<String, Map<String, Set<String>>>> policies = copy(dstate.filtersPerRegion);
            // Only keep regions which still have bundles
            policies.keySet().retainAll(bundles.keySet());
            // Fix broken filters
            for (String name : policies.keySet()) {
                policies.get(name).keySet().retainAll(policies.keySet());
            }
            // Update managed regions
            for (Region computedRegion : computedDigraph.getRegions()) {
                String name = computedRegion.getName();
                Map<String, Map<String, Set<String>>> policy = policies.computeIfAbsent(name, k -> new HashMap<>());
                for (RegionDigraph.FilteredRegion fr : computedRegion.getEdges()) {
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
            callback.replaceDigraph(policies, bundles);
        }


        //
        // Update bundles
        //
        boolean hasToUpdate = false;
        for (Deployer.RegionDeployment regionDeployment : deployment.regions.values()) {
            if (hasToUpdate = !regionDeployment.toUpdate.isEmpty()) {
                break;
            }
        }
        if (hasToUpdate) {
            print("Updating bundles:", verbose);
            for (Map.Entry<String, Deployer.RegionDeployment> rde : deployment.regions.entrySet()) {
                for (Map.Entry<Bundle, Resource> entry : rde.getValue().toUpdate.entrySet()) {
                    Bundle bundle = entry.getKey();
                    Resource resource = entry.getValue();
                    String uri = getUri(resource);
                    print("  " + uri, verbose);
                    try (
                            InputStream is = getBundleInputStream(resource, providers)
                    ) {
                        callback.updateBundle(bundle, uri, is);
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
            callback.setBundleStartLevel(bundle, sl);
        }
        //
        // Install bundles
        //
        boolean hasToInstall = false;
        for (Deployer.RegionDeployment regionDeployment : deployment.regions.values()) {
            if (hasToInstall = !regionDeployment.toInstall.isEmpty()) {
                break;
            }
        }
        if (hasToInstall) {
            print("Installing bundles:", verbose);
            Map<Bundle, Integer> customStartLevels = new HashMap<>();
            for (Map.Entry<String, Deployer.RegionDeployment> entry : deployment.regions.entrySet()) {
                String name = entry.getKey();
                Deployer.RegionDeployment regionDeployment = entry.getValue();
                for (Resource resource : regionDeployment.toInstall) {
                    String uri = getUri(resource);
                    print("  " + uri, verbose);
                    Bundle bundle;
                    long crc;
                    try (
                            ChecksumUtils.CRCInputStream is = new ChecksumUtils.CRCInputStream(getBundleInputStream(resource, providers))
                    ) {
                        bundle = callback.installBundle(name, uri, is);
                        crc = is.getCRC();
                    }
                    addToMapSet(managedBundles, name, bundle.getBundleId());
                    deployment.resToBnd.put(resource, bundle);
                    // save a checksum of installed snapshot bundle
                    if (FeaturesService.SnapshotUpdateBehavior.Crc == request.updateSnaphots
                            && isUpdateable(resource) && !deployment.bundleChecksums.containsKey(bundle.getBundleId())) {
                        deployment.bundleChecksums.put(bundle.getBundleId(), crc);
                    }
                    Integer startLevel = startLevels.get(resource);
                    if (startLevel != null && startLevel != dstate.initialBundleStartLevel) {
                        customStartLevels.put(bundle, startLevel);
                    }
                    FeatureState reqState = states.get(resource);
                    if (reqState == null) {
                        reqState = FeatureState.Started;
                    }
                    switch (reqState) {
                    case Started:
                        toResolve.add(bundle);
                        toStart.add(bundle);
                        break;
                    case Resolved:
                        toResolve.add(bundle);
                        break;
                    }
                }
            }

            // Set start levels after install to avoid starting before all bundles are installed
            for (Bundle bundle : customStartLevels.keySet()) {
                int startLevel = customStartLevels.get(bundle);
                bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
            }
        }

        //
        // Update and save state
        //
        State newState = new State();
        newState.bundleChecksums.putAll(deployment.bundleChecksums);
        newState.requirements.putAll(request.requirements);
        newState.installedFeatures.putAll(installedFeatures);
        newState.stateFeatures.putAll(stateFeatures);
        newState.managedBundles.putAll(managedBundles);
        callback.saveState(newState);

        //
        // Install configurations and libraries
        //
        if (!newFeatures.isEmpty()) {
            Set<String> featureIds = flatten(newFeatures);
            for (Feature feature : dstate.featuresById.values()) {
                if (featureIds.contains(feature.getId())) {
                    callback.installConfigs(feature);
                    callback.installLibraries(feature);
                }
                for (Conditional cond : feature.getConditional()) {
                    Feature condFeature = cond.asFeature();
                    if (featureIds.contains(condFeature.getId())) {
                        callback.installConfigs(condFeature);
                        callback.installLibraries(condFeature);
                    }
                }
            }
        }

        // Delete configurations
        if (deleteConfigurations) {
            for (Map.Entry<String, Set<String>> entry : delFeatures.entrySet()) {
                for (String name : entry.getValue()) {
                    Feature feature = dstate.featuresById.get(name);
                    callback.deleteConfigs(feature);
                }
            }
        }

        if (!noRefresh) {
            if (toRefresh.containsKey(dstate.bundles.get(0l))) {
                print("The system bundle needs to be refreshed, restarting Karaf...", verbose);
                System.setProperty("karaf.restart", "true");
                dstate.bundles.get(0l).stop();
                return;
            }

            toStop = new HashSet<>(toRefresh.keySet());
            removeFragmentsAndBundlesInState(toStop, UNINSTALLED | RESOLVED | STOPPING);
            if (!toStop.isEmpty()) {
                print("Stopping bundles:", verbose);
                while (!toStop.isEmpty()) {
                    List<Bundle> bs = getBundlesToStop(toStop);
                    for (Bundle bundle : bs) {
                        print("  " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                        callback.stopBundle(bundle, STOP_TRANSIENT);
                        toStop.remove(bundle);
                        toStart.add(bundle);
                    }
                }
            }

            if (!toRefresh.isEmpty()) {
                print("Refreshing bundles:", verbose);
                for (Map.Entry<Bundle, String> entry : toRefresh.entrySet()) {
                    Bundle bundle = entry.getKey();
                    print("    " + bundle.getSymbolicName() + "/" + bundle.getVersion() + " (" + entry.getValue() + ")", verbose);
                }
                // Ensure all classes are loaded in case the bundle will be refreshed
                if (serviceBundle != null && toRefresh.containsKey(serviceBundle)) {
                    ensureAllClassesLoaded(serviceBundle);
                }
                callback.refreshPackages(toRefresh.keySet());

            }
        }

        // Resolve bundles
        toResolve.addAll(toStart);
        toResolve.addAll(toRefresh.keySet());
        removeBundlesInState(toResolve, UNINSTALLED);
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        callback.resolveBundles(toResolve, resolver.getWiring(), deployment.resToBnd);
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);

        // Compute bundles to start
        removeFragmentsAndBundlesInState(toStart, UNINSTALLED | ACTIVE);
        if (!toStart.isEmpty()) {
            // Compute correct start order
            List<Exception> exceptions = new ArrayList<>();
            print("Starting bundles:", verbose);
            while (!toStart.isEmpty()) {
                List<Bundle> bs = getBundlesToStart(toStart, serviceBundle);
                for (Bundle bundle : bs) {
                    print("  " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                    try {
                        callback.startBundle(bundle);
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

        // If uninstall and delete configurations, actually delete configurations and configuration files

        // Call listeners
        for (Map.Entry<String, Set<String>> entry : delFeatures.entrySet()) {
            for (String name : entry.getValue()) {
                Feature feature = dstate.featuresById.get(name);
                if (feature != null) {
                    callback.callListeners(new FeatureEvent(FeatureEvent.EventType.FeatureUninstalled, feature, entry.getKey(), false));
                }
            }
        }
        for (Map.Entry<String, Set<String>> entry : newFeatures.entrySet()) {
            for (String name : entry.getValue()) {
                Feature feature = dstate.featuresById.get(name);
                if (feature != null) {
                    callback.callListeners(new FeatureEvent(FeatureEvent.EventType.FeatureInstalled, feature, entry.getKey(), false));
                }
            }
        }
        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);

        print("Done.", verbose);
    }

    private void handlePrerequisites(DeploymentState dstate, DeploymentRequest request, SubsystemResolver resolver)
            throws Exception {
        Set<String> prereqs = resolver.collectPrerequisites();
        if (!prereqs.isEmpty()) {
            for (Iterator<String> iterator = prereqs.iterator(); iterator.hasNext(); ) {
                String prereq = iterator.next();
                String[] parts = prereq.split("/");
                String name = parts[0];
                String version = parts[1];
                VersionRange range = getRange(version, request.featureResolutionRange);
                boolean found = false;
                for (Set<String> featureSet : dstate.state.installedFeatures.values()) {
                    for (String feature : featureSet) {
                        String[] p = feature.split("/");
                        found = name.equals(p[0]) && range.contains(VersionTable.getVersion(p[1]));
                        if (found) {
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                if (found) {
                    iterator.remove();
                }
            }
        }
        if (!prereqs.isEmpty()) {
            if (request.requirements.get(ROOT_REGION).containsAll(prereqs)) {
                throw new CircularPrerequisiteException(prereqs);
            }
            DeploymentRequest newRequest = new DeploymentRequest();
            newRequest.bundleUpdateRange = request.bundleUpdateRange;
            newRequest.featureResolutionRange = request.featureResolutionRange;
            newRequest.serviceRequirements = request.serviceRequirements;
            newRequest.globalRepository = request.globalRepository;
            newRequest.options = request.options;
            newRequest.requirements = copy(dstate.state.requirements);
            for (String prereq : prereqs) {
                addToMapSet(newRequest.requirements, ROOT_REGION, new FeatureReq(prereq).toRequirement());
            }
            newRequest.stateChanges = Collections.emptyMap();
            newRequest.updateSnaphots = request.updateSnaphots;
            deploy(dstate, newRequest);
            throw new PartialDeploymentException(prereqs);
        }
    }

    private static VersionRange getRange(String version, String featureResolutionRange) {
        VersionRange range;
        if (version.equals("0.0.0")) {
            range = VersionRange.ANY_VERSION;
        } else if (!version.startsWith("[") && !version.startsWith("(")) {
            range = new VersionRange(Macro.transform(featureResolutionRange, version));
        } else {
            range = new VersionRange(version);
        }
        return range;
    }

    private void propagateState(Map<Resource, FeatureState> states, Resource resource, FeatureState state, SubsystemResolver resolver) {
        if (!isSubsystem(resource)) {
            FeatureState reqState = mergeStates(state, states.get(resource));
            if (reqState != states.get(resource)) {
                states.put(resource, reqState);
                for (Wire wire : resolver.getWiring().get(resource)) {
                    if (IDENTITY_NAMESPACE.equals(wire.getCapability().getNamespace())) {
                        propagateState(states, wire.getProvider(), reqState, resolver);
                    }
                }
            }
        }
    }

    private static boolean isSubsystem(Resource resource) {
        return TYPE_SUBSYSTEM.equals(getType(resource));
    }

    private static boolean isBundle(Resource resource) {
        return TYPE_BUNDLE.equals(getType(resource));
    }

    /**
     * Returns the most active state of the given states
     */
    private static FeatureState mergeStates(FeatureState s1, FeatureState s2) {
        if (s1 == FeatureState.Started || s2 == FeatureState.Started) {
            return FeatureState.Started;
        }
        if (s1 == FeatureState.Resolved || s2 == FeatureState.Resolved) {
            return FeatureState.Resolved;
        }
        return FeatureState.Installed;
    }

    private static void computeBundlesToRefresh(Map<Bundle, String> toRefresh, Collection<Bundle> bundles, Map<Resource, Bundle> resources, Map<Resource, List<Wire>> resolution) {
        // Compute the new list of fragments
        Map<Bundle, Set<Resource>> newFragments = new HashMap<>();
        for (Bundle bundle : bundles) {
            newFragments.put(bundle, new HashSet<>());
        }
        if (resolution != null) {
            for (Resource res : resolution.keySet()) {
                for (Wire wire : resolution.get(res)) {
                    if (HOST_NAMESPACE.equals(wire.getCapability().getNamespace())) {
                        Bundle bundle;
                        if (wire.getProvider() instanceof BundleRevision) {
                            bundle = ((BundleRevision) wire.getProvider()).getBundle();
                        } else {
                            bundle = resources.get(wire.getProvider());
                        }
                        if (bundle != null) {
                            Bundle b = resources.get(wire.getRequirer());
                            Resource r = b != null ? b.adapt(BundleRevision.class) : wire.getRequirer();
                            newFragments.get(bundle).add(r);
                        }
                    }
                }
            }
        }
        // Main loop
        int size;
        Map<Bundle, Resource> bndToRes = new HashMap<>();
        for (Map.Entry<Resource, Bundle> entry : resources.entrySet()) {
            bndToRes.put(entry.getValue(), entry.getKey());
        }
        do {
            size = toRefresh.size();
            main: for (Bundle bundle : bundles) {
                Resource resource = bndToRes.get(bundle);
                // This bundle is not managed
                if (resource == null) {
                    resource = bundle.adapt(BundleRevision.class);
                }
                // Continue if we already know about this bundle
                if (toRefresh.containsKey(bundle)) {
                    continue;
                }
                // Ignore non resolved bundle
                BundleWiring wiring = bundle.adapt(BundleWiring.class);
                if (wiring == null) {
                    continue;
                }
                // Ignore bundles that won't be wired
                List<Wire> newWires = resolution != null ? resolution.get(resource) : null;
                if (newWires == null) {
                    continue;
                }
                // Check if this bundle is a host and its fragments changed
                Set<Resource> oldFragments = new HashSet<>();
                for (BundleWire wire : wiring.getProvidedWires(null)) {
                    if (HOST_NAMESPACE.equals(wire.getCapability().getNamespace())) {
                        oldFragments.add(wire.getRequirer());
                    }
                }
                if (!oldFragments.containsAll(newFragments.get(bundle))) {
                    toRefresh.put(bundle, "Attached fragments changed: " + new ArrayList<>(newFragments.get(bundle)));
                    break;
                }
                // Compare the old and new resolutions
                Set<BundleWrapper> wiredBundles = new HashSet<>();
                wiredBundles.add(new BundleWrapper(bundle));
                for (BundleWire wire : wiring.getRequiredWires(null)) {
                    BundleRevision rev = wire.getProvider();
                    Bundle provider = rev.getBundle();
                    if (toRefresh.containsKey(provider)) {
                        // The bundle is wired to a bundle being refreshed,
                        // so we need to refresh it too
                        toRefresh.put(bundle, "Wired to " + provider.getSymbolicName() + "/" + provider.getVersion() + " which is being refreshed");
                        continue main;
                    }
                    wiredBundles.add(new BundleWrapper(provider));
                }
                Map<BundleWrapper, Requirement> wiredResources = new HashMap<>();
                for (Wire wire : newWires) {
                    // Handle only packages, hosts, and required bundles
                    String namespace = wire.getRequirement().getNamespace();
                    if (!namespace.equals(BundleNamespace.BUNDLE_NAMESPACE)
                            && !namespace.equals(PackageNamespace.PACKAGE_NAMESPACE)
                            && !namespace.equals(HostNamespace.HOST_NAMESPACE)) {
                        continue;
                    }
                    // Ignore non-resolution time requirements
                    String effective = wire.getRequirement().getDirectives().get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
                    if (effective != null && !Namespace.EFFECTIVE_RESOLVE.equals(effective)) {
                        continue;
                    }
                    // Ignore non bundle resources
                    if (!isBundle(wire.getProvider())) {
                        continue;
                    }
                    BundleWrapper bw = new BundleWrapper(wire.getProvider());
                    if (!wiredResources.containsKey(bw)) {
                        wiredResources.put(bw, wire.getRequirement());
                    }
                }
                if (!wiredBundles.containsAll(wiredResources.keySet())) {
                    Map<BundleWrapper, Requirement> newResources = new HashMap<>(wiredResources);
                    newResources.keySet().removeAll(wiredBundles);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Should be wired to: ");
                    boolean first = true;
                    for (Map.Entry<BundleWrapper, Requirement> entry : newResources.entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        } else {
                            first = false;
                        }
                        Requirement req = entry.getValue();
                        sb.append(entry.getKey());
                        sb.append(" (through ");
                        sb.append(req);
                        sb.append(")");
                    }
                    toRefresh.put(bundle, sb.toString());
                }
            }
        } while (toRefresh.size() > size);
    }

    private void print(String message, boolean verbose) {
        callback.print(message, verbose);
    }

    private static void removeFragmentsAndBundlesInState(Collection<Bundle> bundles, int state) {
        bundles.removeIf(bundle -> (bundle.getState() & state) != 0
                || bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null);
    }

    private static void removeBundlesInState(Collection<Bundle> bundles, int state) {
        bundles.removeIf(bundle -> (bundle.getState() & state) != 0);
    }

    protected void logWiring(Map<Resource, List<Wire>> wiring, boolean onlyFeatures) {
        print("Wiring:", true);
        Map<Resource, Set<Resource>> wires = new HashMap<>();
        for (Resource r : wiring.keySet()) {
            if (onlyFeatures && !ResourceUtils.TYPE_FEATURE.equals(ResourceUtils.getType(r))) {
                continue;
            }
            for (Wire w : wiring.get(r)) {
                if (onlyFeatures && !ResourceUtils.TYPE_FEATURE.equals(ResourceUtils.getType(w.getProvider()))) {
                    continue;
                }
                MapUtils.addToMapSet(wires, w.getRequirer(), w.getProvider());
            }
        }
        List<Resource> sorted = new ArrayList<>(wires.keySet());
        sorted.sort(Comparator.comparingInt(r2 -> wires.get(r2).size()));
        for (Resource r : sorted) {
            print("    " + ResourceUtils.getType(r) + ": " + ResolverUtil.getSymbolicName(r) + " / " + ResolverUtil.getVersion(r), true);
            for (Resource w : wires.get(r)) {
                print("        " + ResourceUtils.getType(w) + ": " + ResolverUtil.getSymbolicName(w) + " / " + ResolverUtil.getVersion(w), true);
            }
        }
    }

    protected void logDeployment(Deployer.Deployment overallDeployment, boolean verbose) {
        if (overallDeployment.regions.isEmpty()) {
            print("No deployment change.", verbose);
            return;
        }
        print("Changes to perform:", verbose);
        for (Map.Entry<String, Deployer.RegionDeployment> region : overallDeployment.regions.entrySet()) {
            Deployer.RegionDeployment deployment = region.getValue();
            print("  Region: " + region.getKey(), verbose);
            if (!deployment.toDelete.isEmpty()) {
                print("    Bundles to uninstall:", verbose);
                for (Bundle bundle : deployment.toDelete) {
                    print("      " + bundle.getSymbolicName() + "/" + bundle.getVersion(), verbose);
                }
            }
            if (!deployment.toUpdate.isEmpty()) {
                print("    Bundles to update:", verbose);
                for (Map.Entry<Bundle, Resource> entry : deployment.toUpdate.entrySet()) {
                    print("      " + entry.getKey().getSymbolicName() + "/" + entry.getKey().getVersion() + " with " + getUri(entry.getValue()), verbose);
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
                    DeploymentRequest request,
                    SubsystemResolver resolver) throws IOException {

        Deployment result = new Deployment();

        Map<String, Set<Resource>> bundlesPerRegions = resolver.getBundlesPerRegions();

        // Gather all regions, including old ones and new ones
        Set<String> regions = new HashSet<>();
        regions.addAll(dstate.state.managedBundles.keySet());
        regions.addAll(bundlesPerRegions.keySet());

        for (String region : regions) {

            Deployer.RegionDeployment deployment = new Deployer.RegionDeployment();

            // Get the list of bundles currently assigned in the region
            Set<Long> managed = dstate.state.managedBundles.get(region);
            if (managed == null) {
                managed = Collections.emptySet();
            }

            // Compute the list of resources to deploy in the region
            Set<Resource> bundlesInRegion = bundlesPerRegions.get(region);
            List<Resource> toDeploy = bundlesInRegion != null ? new ArrayList<>(bundlesInRegion) : new ArrayList<>();

            // Remove the system bundle
            Bundle systemBundle = dstate.bundles.get(0l);
            if (systemBundle != null) {
                // It may be null when unit testing, so ignore that
                toDeploy.remove(systemBundle.adapt(BundleRevision.class));
            }

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
                        if (bundle.getSymbolicName().equals(getSymbolicName(res))
                                && bundle.getVersion().equals(getVersion(res))) {
                            resource = res;
                            break;
                        }
                    }
                    // We found a matching bundle
                    if (resource != null) {
                        // In case of snapshots, check if the snapshot is out of date
                        // and flag it as to update
                        if (isUpdateable(resource)) {
                            // Always update snapshots
                            if (FeaturesService.SnapshotUpdateBehavior.Always == request.updateSnaphots) {
                                LOGGER.debug("Update snapshot for " + bundle.getLocation());
                                deployment.toUpdate.put(bundle, resource);
                            } else if (FeaturesService.SnapshotUpdateBehavior.Crc == request.updateSnaphots) {
                                // Retrieve current bundle checksum
                                long oldCrc;
                                if (dstate.state.bundleChecksums.containsKey(bundleId)) {
                                    oldCrc = dstate.state.bundleChecksums.get(bundleId);
                                } else {
                                    // Load bundle checksums if not already done
                                    // This is a bit hacky, but we can't get a hold on the real bundle location
                                    // in a standard way in OSGi.  Therefore, hack into Felix/Equinox to obtain the
                                    // corresponding jar url and use that one to compute the checksum of the bundle.
                                    oldCrc = 0l;
                                    try {
                                        URL url = bundle.getEntry("META-INF/MANIFEST.MF");
                                        URLConnection con = url.openConnection();
                                        Method method = con.getClass().getDeclaredMethod("getLocalURL");
                                        method.setAccessible(true);
                                        String jarUrl = ((URL) method.invoke(con)).toExternalForm();
                                        if (jarUrl.startsWith("jar:")) {
                                            String jar = jarUrl.substring("jar:".length(), jarUrl.indexOf("!/"));
                                            jar = new URL(jar).getFile();
                                            try (InputStream is = new FileInputStream(jar)) {
                                                oldCrc = ChecksumUtils.checksum(is);
                                            }
                                            result.bundleChecksums.put(bundleId, oldCrc);
                                        }
                                    } catch (Throwable t) {
                                        LOGGER.debug("Error calculating checksum for bundle: {}", bundle, t);
                                    }
                                }
                                // Compute new bundle checksum
                                long newCrc;
                                try (
                                        InputStream is = getBundleInputStream(resource, resolver.getProviders())
                                ) {
                                    newCrc = ChecksumUtils.checksum(is);
                                    result.bundleChecksums.put(bundle.getBundleId(), newCrc);
                                }
                                // if the checksum are different
                                if (newCrc != oldCrc) {
                                    LOGGER.debug("New snapshot available for " + bundle.getLocation());
                                    deployment.toUpdate.put(bundle, resource);
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
                VersionRange range = new VersionRange(Macro.transform(request.bundleUpdateRange, getVersion(resource).toString()));
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
            deployment.toInstall.sort(new ResourceComparator());

            // Add this region if there is something to do
            if (!deployment.toDelete.isEmpty()
                    || !deployment.toUpdate.isEmpty()
                    || !deployment.toInstall.isEmpty()) {
                result.regions.put(region, deployment);
            }
        }

        return result;
    }

    protected <T> Function<Bundle, T> adapt(final Class<T> clazz) {
        return bundle -> bundle.adapt(clazz);
    }

    protected Function<Bundle, Long> bundleId() {
        return Bundle::getBundleId;
    }

    protected Function<Resource, String> featureId() {
        return ResourceUtils::getFeatureId;
    }

    protected boolean isUpdateable(Resource resource) {
        String uri = getUri(resource);
        return uri != null && uri.matches(UPDATEABLE_URIS);
    }

    protected List<Bundle> getBundlesToStart(Collection<Bundle> bundles, Bundle serviceBundle) {
        // Restart the features service last, regardless of any other consideration
        // so that we don't end up with the service trying to do stuff before we're done
        boolean restart = false;

        SortedMap<Integer, Set<Bundle>> bundlesPerStartLevel = new TreeMap<>();
        for (Bundle bundle : bundles) {
            if (bundle == serviceBundle) {
                restart = true;
            } else {
                int sl = bundle.adapt(BundleStartLevel.class).getStartLevel();
                addToMapSet(bundlesPerStartLevel, sl, bundle);
            }
        }
        if (bundlesPerStartLevel.isEmpty()) {
            bundles = Collections.emptyList();
        } else {
            bundles = bundlesPerStartLevel.remove(bundlesPerStartLevel.firstKey());
        }

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
            sorted.add(serviceBundle);
        }
        return sorted;
    }

    @SuppressWarnings("rawtypes")
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
            bundlesToDestroy.sort((b1, b2) -> Long.compare(b2.getLastModified(), b1.getLastModified()));
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

    private static int getServiceUsage(ServiceReference<?> ref, Collection<Bundle> bundles) {
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
            return new URL(uri).openStream();
//            throw new IllegalStateException("Resource " + uri + " has no StreamProvider");
        }
        return provider.open();
    }

    public static void ensureAllClassesLoaded(Bundle bundle) throws ClassNotFoundException {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);
        if (wiring != null) {
            for (String path : wiring.listResources("/", "*.class", BundleWiring.LISTRESOURCES_RECURSE)) {
                String className = path.substring(0, path.length() - ".class".length());
                className = className.replace('/', '.');
                bundle.loadClass(className);
            }
        }
    }

    public static class BundleWrapper {
        final String symbolicName;
        final Version version;

        public BundleWrapper(Bundle bundle) {
            this.symbolicName = bundle.getSymbolicName();
            this.version = bundle.getVersion();
        }

        public BundleWrapper(BundleRevision bundleRevision) {
            this.symbolicName = bundleRevision.getSymbolicName();
            this.version = bundleRevision.getVersion();
        }

        public BundleWrapper(Resource resource) {
            this.symbolicName = ResolverUtil.getSymbolicName(resource);
            this.version = ResolverUtil.getVersion(resource);
        }

        public String getSymbolicName() {
            return symbolicName;
        }

        public Version getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BundleWrapper that = (BundleWrapper) o;
            return Objects.equals(symbolicName, that.symbolicName) &&
                   Objects.equals(version, that.version);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(symbolicName, version);
        }

        @Override
        public String toString() {
            return symbolicName + "/" + version;
        }

    }

}
