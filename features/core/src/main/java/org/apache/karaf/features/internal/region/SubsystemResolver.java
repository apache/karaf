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

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.resolver.BaseClause;
import org.apache.karaf.features.internal.resolver.CapabilityImpl;
import org.apache.karaf.features.internal.resolver.CapabilitySet;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.apache.karaf.features.internal.resolver.ResourceImpl;
import org.apache.karaf.features.internal.resolver.SimpleFilter;
import org.apache.karaf.features.internal.util.JsonWriter;
import org.eclipse.equinox.internal.region.StandardRegionDigraph;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_FEATURE;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.TYPE_SUBSYSTEM;
import static org.apache.karaf.features.internal.util.MapUtils.invert;
import static org.osgi.framework.Constants.PROVIDE_CAPABILITY;
import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.TYPE_BUNDLE;
import static org.osgi.framework.namespace.IdentityNamespace.TYPE_FRAGMENT;

public class SubsystemResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubsystemResolver.class);

    private DownloadManager manager;
    private Resolver resolver;
    private RegionDigraph digraph;
    private Subsystem root;
    private Map<Resource, List<Wire>> wiring;

    // Cached computed results
    private ResourceImpl environmentResource;
    private Map<String, String> flatSubsystemsMap;
    private Map<String, Set<Resource>> bundlesPerRegions;
    private Map<Resource, String> bundles;
    private Map<String, Set<Resource>> featuresPerRegions;
    private Map<Resource, String> features;
    private RegionDigraph flatDigraph;
    private Map<String, Map<String, BundleInfo>> bundleInfos;

    public SubsystemResolver(Resolver resolver, DownloadManager manager) {
        this.resolver = resolver;
        this.manager = manager;
    }

    public void prepare(
            Collection<Feature> allFeatures,
            Map<String, Set<String>> requirements,
            Map<String, Set<BundleRevision>> system
    ) throws Exception {
        // Build subsystems on the fly
        for (Map.Entry<String, Set<String>> entry : requirements.entrySet()) {
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
            for (String requirement : entry.getValue()) {
                ss.require(requirement);
            }
        }
        if (root == null) {
            return;
        }

        // Pre-resolve
        root.build(allFeatures);

        // Add system resources
        BundleRevision sysBundleRev = null;
        boolean hasEeCap = false;
        for (Map.Entry<String, Set<BundleRevision>> entry : system.entrySet()) {
            Subsystem ss = null;
            String[] parts = entry.getKey().split("/");
            String path = parts[0];
            if (path.equals(root.getName())) {
                ss = root;
            }
            for (int i = 1; ss != null && i < parts.length; i++) {
                path += "/" + parts[i];
                ss = ss.getChild(path);
            }
            if (ss != null) {
                ResourceImpl dummy = new ResourceImpl("dummy", "dummy", Version.emptyVersion);
                for (BundleRevision res : entry.getValue()) {
                    // We need to explicitely provide service capabilities for bundles
                    // We use both actual services and services declared from the headers
                    // TODO: use actual services
                    Map<String, String> headers = new DictionaryAsMap<>(res.getBundle().getHeaders());
                    Resource tmp = ResourceBuilder.build(res.getBundle().getLocation(), headers);
                    for (Capability cap : tmp.getCapabilities(ServiceNamespace.SERVICE_NAMESPACE)) {
                        dummy.addCapability(new CapabilityImpl(dummy, cap.getNamespace(), cap.getDirectives(), cap.getAttributes()));
                    }
                    ss.addSystemResource(res);
                    for (Capability cap : res.getCapabilities(null)) {
                        hasEeCap |= cap.getNamespace().equals(EXECUTION_ENVIRONMENT_NAMESPACE);
                    }
                    if (res.getBundle().getBundleId() == 0) {
                        sysBundleRev = res;
                    }
                }
                ss.addSystemResource(dummy);
            }
        }
        // Under Equinox, the osgi.ee capabilities are not provided by the system bundle
        if (!hasEeCap && sysBundleRev != null) {
            String provideCaps = sysBundleRev.getBundle().getHeaders().get(PROVIDE_CAPABILITY);
            environmentResource = new ResourceImpl("environment", "karaf.environment", Version.emptyVersion);
            environmentResource.addCapabilities(ResourceBuilder.parseCapability(environmentResource, provideCaps));
            root.addSystemResource(environmentResource);
        }
    }

    public Set<String> collectPrerequisites() throws Exception {
        if (root != null) {
            return root.collectPrerequisites();
        }
        return new HashSet<>();
    }

    public Map<Resource, List<Wire>> resolve(
            Set<String> overrides,
            String featureResolutionRange,
            String serviceRequirements,
            final Repository globalRepository,
            String outputFile) throws Exception {

        if (root == null) {
            return Collections.emptyMap();
        }

        // Download bundles
        RepositoryManager repos = new RepositoryManager();
        root.downloadBundles(manager, overrides, featureResolutionRange, serviceRequirements, repos);

        // Populate digraph and resolve
        digraph = new StandardRegionDigraph(null, null);
        populateDigraph(digraph, root);

        Downloader downloader = manager.createDownloader();
        SubsystemResolveContext context = new SubsystemResolveContext(root, digraph, globalRepository, downloader, serviceRequirements);
        if (outputFile != null) {
            Map<String, Object> json = new HashMap<>();
            if (globalRepository != null) {
                json.put("globalRepository", toJson(globalRepository));
            }
            json.put("repository", toJson(context.getRepository()));
            try {
                wiring = resolver.resolve(context);
                json.put("success", "true");
                json.put("wiring", toJson(wiring));
            } catch (Exception e) {
                json.put("success", "false");
                json.put("exception", e.toString());
                throw e;
            } finally {
                try (Writer writer = Files.newBufferedWriter(
                        Paths.get(outputFile),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    JsonWriter.write(writer, json, true);
                }
            }
        } else {
            wiring = resolver.resolve(context);
        }
        downloader.await();

        // Remove wiring to the fake environment resource
        if (environmentResource != null) {
            for (List<Wire> wires : wiring.values()) {
                for (Iterator<Wire> iterator = wires.iterator(); iterator.hasNext();) {
                    Wire wire = iterator.next();
                    if (wire.getProvider() == environmentResource) {
                        iterator.remove();
                    }
                }
            }
        }
        // Fragments are always wired to their host only, so create fake wiring to
        // the subsystem the host is wired to
        associateFragments();

        return wiring;
    }

    private Object toJson(Map<Resource, List<Wire>> wiring) {
        Map<String, List<Map<String, Object>>> wires = new HashMap<>();
        for (Map.Entry<Resource, List<Wire>> reswiring : wiring.entrySet()) {
            Resource resource = reswiring.getKey();
            String id = toString(resource);
            List<Map<String, Object>> reswires = new ArrayList<>();
            for (Wire w : reswiring.getValue()) {
                Map<String, Object> rw = new LinkedHashMap<>();
                rw.put("requirement", toString(w.getRequirement()));
                rw.put("capability", toString(w.getCapability()));
                rw.put("requirer", toString(w.getRequirer()));
                rw.put("provider", toString(w.getProvider()));
                reswires.add(rw);
            }
            wires.put(id, reswires);
        }
        return wires;
    }

    private String toString(Resource r) {
        return toString(r.getCapabilities(IDENTITY_NAMESPACE).get(0));
    }

    private String toString(Requirement r) {
        return BaseClause.toString(null, r.getNamespace(), r.getAttributes(), r.getDirectives());
    }

    private String toString(Capability c) {
        return BaseClause.toString(null, c.getNamespace(), c.getAttributes(), c.getDirectives());
    }

    private Object toJson(Repository repository) {
        Requirement req = new RequirementImpl(
                null,
                IDENTITY_NAMESPACE,
                Collections.emptyMap(),
                Collections.emptyMap(),
                new SimpleFilter(null, null, SimpleFilter.MATCH_ALL));
        Collection<Capability> identities = repository.findProviders(Collections.singleton(req)).get(req);
        List<Object> resources = new ArrayList<>();
        for (Capability identity : identities) {
            String id = BaseClause.toString(null, identity.getNamespace(), identity.getAttributes(), identity.getDirectives());
            resources.add(toJson(identity.getResource()));
        }
        return resources;
    }

    private Object toJson(Resource resource) {
        Map<String, Object> obj = new HashMap<>();
        List<Object> caps = new ArrayList<>();
        List<Object> reqs = new ArrayList<>();
        for (Capability cap : resource.getCapabilities(null)) {
            caps.add(BaseClause.toString(null, cap.getNamespace(), cap.getAttributes(), cap.getDirectives()));
        }
        for (Requirement req : resource.getRequirements(null)) {
            reqs.add(BaseClause.toString(null, req.getNamespace(), req.getAttributes(), req.getDirectives()));
        }
        obj.put("capabilities", caps);
        obj.put("requirements", reqs);
        return obj;
    }

    public Map<String, Map<String, BundleInfo>> getBundleInfos() {
        if (bundleInfos == null) {
            bundleInfos = new HashMap<>();
            addBundleInfos(root);
        }
        return bundleInfos;
    }

    private void addBundleInfos(Subsystem subsystem) {
        if (subsystem != null) {
            String region = getFlatSubsystemsMap().get(subsystem.getName());
            bundleInfos.computeIfAbsent(region, k -> new HashMap<>()).putAll(subsystem.getBundleInfos());
            for (Subsystem child : subsystem.getChildren()) {
                addBundleInfos(child);
            }
        }
    }

    public Map<String, StreamProvider> getProviders() {
        return manager.getProviders();
    }

    public Map<Resource, List<Wire>> getWiring() {
        return wiring;
    }

    public RegionDigraph getFlatDigraph() throws BundleException, InvalidSyntaxException {
        if (flatDigraph == null) {
            flatDigraph = new StandardRegionDigraph(null, null);
            Map<String, String> flats = getFlatSubsystemsMap();
            if (digraph != null) {
                for (Region r : digraph.getRegions()) {
                    if (r.getName().equals(flats.get(r.getName()))) {
                        flatDigraph.createRegion(r.getName());
                    }
                }
                for (Region r : digraph.getRegions()) {
                    for (RegionDigraph.FilteredRegion fr : digraph.getEdges(r)) {
                        String rt = flats.get(r.getName());
                        String rh = flats.get(fr.getRegion().getName());
                        if (!rh.equals(rt)) {
                            Region tail = flatDigraph.getRegion(rt);
                            Region head = flatDigraph.getRegion(rh);
                            RegionFilterBuilder rfb = flatDigraph.createRegionFilterBuilder();
                            for (Map.Entry<String, Collection<String>> entry : fr.getFilter().getSharingPolicy().entrySet()) {
                                // Discard osgi.identity namespace
                                if (!IDENTITY_NAMESPACE.equals(entry.getKey())) {
                                    for (String f : entry.getValue()) {
                                        rfb.allow(entry.getKey(), f);
                                    }
                                }
                            }
                            flatDigraph.connect(tail, rfb.build(), head);
                        }
                    }
                }
            }
        }
        return flatDigraph;
    }

    public Map<String, String> getFlatSubsystemsMap() {
        if (flatSubsystemsMap == null) {
            flatSubsystemsMap = new HashMap<>();
            findSubsystemsToFlatten(root, flatSubsystemsMap);
        }
        return flatSubsystemsMap;
    }

    public Map<String, Set<Resource>> getBundlesPerRegions() {
        if (bundlesPerRegions == null) {
            bundlesPerRegions = invert(getBundles());
        }
        return bundlesPerRegions;
    }

    /**
     * 
     * @return map of bundles and the region they are deployed in
     */
    public Map<Resource, String> getBundles() {
        if (bundles == null) {
            String filter = String.format("(&(%s=*)(|(%s=%s)(%s=%s)))",
                    IDENTITY_NAMESPACE,
                    CAPABILITY_TYPE_ATTRIBUTE, TYPE_BUNDLE,
                    CAPABILITY_TYPE_ATTRIBUTE, TYPE_FRAGMENT);
            SimpleFilter sf = SimpleFilter.parse(filter);
            bundles = getResourceMapping(sf);
        }
        return bundles;
    }

    public Map<String, Set<Resource>> getFeaturesPerRegions() {
        if (featuresPerRegions == null) {
            featuresPerRegions = invert(getFeatures());
        }
        return featuresPerRegions;
    }

    public Map<Resource, String> getFeatures() {
        if (features == null) {
            SimpleFilter sf = createFilter(IDENTITY_NAMESPACE, "*",
                    CAPABILITY_TYPE_ATTRIBUTE, TYPE_FEATURE);
            features = getResourceMapping(sf);
        }
        return features;
    }

    /**
     * 
     * @param resourceFilter
     * @return map from resource to region name
     */
    private Map<Resource, String> getResourceMapping(SimpleFilter resourceFilter) {
        Map<String, String> flats = getFlatSubsystemsMap();
        Map<Resource, List<Wire>> wiring = getWiring();
        Map<Resource, String> resources = new HashMap<>();
        SimpleFilter sf = createFilter(IDENTITY_NAMESPACE, "*",
                CAPABILITY_TYPE_ATTRIBUTE, TYPE_SUBSYSTEM);
        if (wiring != null) {
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
        }
        return resources;
    }

    private void associateFragments() {
        SimpleFilter sf = createFilter(IDENTITY_NAMESPACE, "*", CAPABILITY_TYPE_ATTRIBUTE, TYPE_SUBSYSTEM);
        for (Map.Entry<Resource, List<Wire>> entry : wiring.entrySet()) {
            final Resource resource = entry.getKey();
            final Requirement requirement = getSubsystemRequirement(resource);
            if (ResolverUtil.isFragment(resource) && requirement != null) {
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
        Map<String, Object> attrs = new HashMap<>();
        for (int i = 0; i < s.length - 1; i += 2) {
            attrs.put(s[i], s[i + 1]);
        }
        return SimpleFilter.convert(attrs);

    }

    private void findSubsystemsToFlatten(Subsystem subsystem, Map<String, String> toFlatten) {
        Subsystem nonFlat = subsystem;
        while (isFlat(nonFlat)) {
            nonFlat = nonFlat.getParent();
        }
        if (subsystem != null) {
            toFlatten.put(subsystem.getName(), nonFlat.getName());
            for (Subsystem child : subsystem.getChildren()) {
                findSubsystemsToFlatten(child, toFlatten);
            }
        }
    }

    private boolean isFlat(Subsystem subsystem) {
        if (subsystem == null || subsystem.getFeature() == null)
            return false;
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
        for (Map.Entry<String, Set<String>> entry : sharingPolicy.entrySet()) {
            for (String filter : entry.getValue()) {
                result.allow(entry.getKey(), filter);
            }
        }
        return result;
    }

}
