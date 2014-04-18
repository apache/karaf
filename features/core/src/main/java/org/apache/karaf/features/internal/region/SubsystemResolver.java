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
package org.apache.karaf.features.internal.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.resolver.Util;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.download.simple.SimpleDownloader;
import org.apache.karaf.features.internal.resolver.CapabilitySet;
import org.apache.karaf.features.internal.resolver.SimpleFilter;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.eclipse.equinox.internal.region.StandardRegionDigraph;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_FEATURE;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_SUBSYSTEM;
import static org.apache.karaf.features.internal.util.MapUtils.invert;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.TYPE_BUNDLE;
import static org.osgi.framework.namespace.IdentityNamespace.TYPE_FRAGMENT;

public class SubsystemResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemResolver.class);

    private DownloadManager manager;
    private RegionDigraph digraph;
    private Subsystem root;
    private Map<Resource, List<Wire>> wiring;

    public SubsystemResolver() {
        this(new SimpleDownloader());
    }

    public SubsystemResolver(DownloadManager manager) {
        this.manager = manager;
    }

    public Map<Resource, List<Wire>> resolve(
            List<Repository> repositories,
            Map<String, Set<String>> features,
            Collection<? extends Resource> system,
            String featureResolutionRange
    ) throws Exception {
        // Build subsystems on the fly
        for (Map.Entry<String, Set<String>> entry : features.entrySet()) {
            String[] parts = entry.getKey().split("/");
            if (root == null) {
                root = new Subsystem(parts[0]);
            } else if (!root.getName().equals(parts[0])) {
                throw new IllegalArgumentException("Can not use multiple roots: " + root.getName() + ", " + parts[0]);
            }
            Subsystem ss = root;
            for (int i = 1; i < parts.length; i++) {
                ss = getOrCreateChild(ss, parts[i]);
            }
            for (String feature : entry.getValue()) {
                String name, range;
                int idx = feature.indexOf('/');
                if (idx >= 0) {
                    name = feature.substring(0, idx);
                    range = feature.substring(idx + 1);
                } else {
                    name = feature;
                    range = null;
                }
                ss.requireFeature(name, range);
            }
        }
        if (root == null) {
            return Collections.emptyMap();
        }
        // Pre-resolve
        List<Feature> allFeatures = new ArrayList<Feature>();
        for (Repository repo : repositories) {
            allFeatures.addAll(Arrays.asList(repo.getFeatures()));
        }
        root.preResolve(allFeatures, manager, featureResolutionRange);

        // Add system resources
        for (Resource res : system) {
            root.addSystemResource(res);
        }

        // Populate digraph and resolve
        digraph = new StandardRegionDigraph(null, null);
        populateDigraph(digraph, root);

        Resolver resolver = new ResolverImpl(new Slf4jResolverLog(LOGGER));
        wiring = resolver.resolve(new SubsystemResolveContext(root, digraph));

        // Fragments are always wired to their host only, so create fake wiring to
        // the subsystem the host is wired to
        associateFragments();

        return wiring;
    }

    public Map<String, StreamProvider> getProviders() {
        return manager.getProviders();
    }

    public RegionDigraph getDigraph() {
        return digraph;
    }

    public Map<Resource, List<Wire>> getWiring() {
        return wiring;
    }

    public Map<String, String> getFlatSubsystemsMap() {
        Map<String, String> toFlatten = new HashMap<String, String>();
        findSubsystemsToFlatten(root, toFlatten);
        return toFlatten;
    }

    public Map<String, Set<Resource>> getBundlesPerRegions() {
        return invert(getBundles());
    }

    public Map<Resource, String> getBundles() {
        String filter = String.format("(&(%s=*)(|(%s=%s)(%s=%s)))",
                            IDENTITY_NAMESPACE,
                            CAPABILITY_TYPE_ATTRIBUTE, TYPE_BUNDLE,
                            CAPABILITY_TYPE_ATTRIBUTE, TYPE_FRAGMENT);
        SimpleFilter sf = SimpleFilter.parse(filter);
        return getResourceMapping(sf);
    }

    public Map<String, Set<Resource>> getFeaturesPerRegions() {
        return invert(getFeatures());
    }

    public Map<Resource, String> getFeatures() {
        SimpleFilter sf = createFilter(IDENTITY_NAMESPACE, "*",
                                       CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);
        return getResourceMapping(sf);
    }

    public Map<String, Set<Resource>> getResourcesPerRegion(SimpleFilter resourceFilter) {
        return invert(getResourceMapping(resourceFilter));
    }

    public Map<Resource, String> getResourceMapping(SimpleFilter resourceFilter) {
        Map<String, String> flats = getFlatSubsystemsMap();
        Map<Resource, List<Wire>> wiring = getWiring();
        Map<Resource, String> resources = new HashMap<Resource, String>();
        SimpleFilter sf = createFilter(IDENTITY_NAMESPACE, "*",
                                       CAPABILITY_TYPE_ATTRIBUTE, TYPE_SUBSYSTEM);
        for (Resource resource : wiring.keySet()) {
            if (findMatchingCapability(resourceFilter, resource.getCapabilities(null)) != null) {
                // Find the subsystem where this feature is installed
                Wire wire = findMatchingWire(sf, wiring.get(resource));
                if (wire != null) {
                    String region = (String) wire.getCapability().getAttributes().get(IDENTITY_NAMESPACE);
                    region = flats.get(region);
                    resources.put(resource, region);
                }
            }
        }
        return resources;
    }

    private void associateFragments() {
        SimpleFilter sf = createFilter(IDENTITY_NAMESPACE, "*", CAPABILITY_TYPE_ATTRIBUTE, TYPE_SUBSYSTEM);
        for (Map.Entry<Resource, List<Wire>> entry : wiring.entrySet()) {
            final Resource resource = entry.getKey();
            final Requirement requirement = getSubsystemRequirement(resource);
            if (Util.isFragment(resource)) {
                List<Wire> wires = entry.getValue();
                final Resource host = wires.get(0).getProvider();
                final Wire wire = findMatchingWire(sf, wiring.get(host));
                if (wire != null) {
                    wires.add(new Wire() {
                        @Override
                        public Capability getCapability() {
                            return wire.getCapability();
                        }

                        @Override
                        public Requirement getRequirement() {
                            return requirement;
                        }

                        @Override
                        public Resource getProvider() {
                            return wire.getProvider();
                        }

                        @Override
                        public Resource getRequirer() {
                            return resource;
                        }
                    });
                }
            }
        }
    }

    private Requirement getSubsystemRequirement(Resource resource) {
        for (Requirement requirement : resource.getRequirements(null)) {
            if (IDENTITY_NAMESPACE.equals(requirement.getNamespace())
                    && TYPE_SUBSYSTEM.equals(requirement.getAttributes().get(CAPABILITY_TYPE_ATTRIBUTE))) {
                return requirement;
            }
        }
        return null;
    }

    private Capability findMatchingCapability(SimpleFilter filter, Collection<Capability> caps) {
        for (Capability cap : caps) {
            if (CapabilitySet.matches(cap, filter)) {
                return cap;
            }
        }
        return null;
    }

    private Wire findMatchingWire(SimpleFilter filter, Collection<Wire> wires) {
        for (Wire wire : wires) {
            Capability cap = wire.getCapability();
            if (CapabilitySet.matches(cap, filter)) {
                return wire;
            }
        }
        return null;
    }

    private SimpleFilter createFilter(String... s) {
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (int i = 0; i < s.length - 1; i += 2) {
            attrs.put(s[i], s[i+1]);
        }
        return SimpleFilter.convert(attrs);

    }

    private void findSubsystemsToFlatten(Subsystem subsystem, Map<String, String> toFlatten) {
        Subsystem nonFlat = subsystem;
        while (isFlat(nonFlat)) {
            nonFlat = nonFlat.getParent();
        }
        toFlatten.put(subsystem.getName(), nonFlat.getName());
        for (Subsystem child : subsystem.getChildren()) {
            findSubsystemsToFlatten(child, toFlatten);
        }
    }

    private boolean isFlat(Subsystem subsystem) {
        return subsystem.getFeature() != null && subsystem.getFeature().getScoping() == null;
    }

    private Subsystem getOrCreateChild(Subsystem ss, String name) {
        Subsystem child = ss.getChild(name);
        return child != null ? child : ss.createSubsystem(name, true);
    }

    private void populateDigraph(RegionDigraph digraph, Subsystem subsystem) throws BundleException, InvalidSyntaxException {
        Region region = digraph.createRegion(subsystem.getName());
        if (subsystem.getParent() != null) {
            Region parent = digraph.getRegion(subsystem.getParent().getName());
            digraph.connect(region, createRegionFilterBuilder(digraph, subsystem.getImportPolicy()).build(), parent);
            digraph.connect(parent, createRegionFilterBuilder(digraph, subsystem.getExportPolicy()).build(), region);
        }
        for (Subsystem child : subsystem.getChildren()) {
            populateDigraph(digraph, child);
        }
    }

    private RegionFilterBuilder createRegionFilterBuilder(RegionDigraph digraph, Map<String, Set<String>> sharingPolicy) throws InvalidSyntaxException {
        RegionFilterBuilder result = digraph.createRegionFilterBuilder();
        for (Map.Entry<String, Set<String>> entry : sharingPolicy.entrySet())
            for (String filter : entry.getValue())
                result.allow(entry.getKey(), filter);
        return result;
    }

}
