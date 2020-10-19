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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.felix.utils.repository.BaseRepository;
import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.felix.utils.resource.ResourceImpl;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.util.promise.Promise;

import static org.apache.karaf.features.internal.resolver.ResourceUtils.addIdentityRequirement;
import static org.apache.karaf.features.internal.resolver.ResourceUtils.getUri;
import static org.eclipse.equinox.region.RegionFilter.VISIBLE_BUNDLE_NAMESPACE;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE;

public class SubsystemResolveContext extends ResolveContext {

    private final Subsystem root;
    private final Map<String, Region> regions;
    private final Map<Resource, Integer> distance;
    private final CandidateComparator candidateComparator = new CandidateComparator(this::getResourceCost);

    private final Map<Resource, Subsystem> resToSub = new HashMap<>();
    private final Repository repository;
    private final Repository globalRepository;
    private final Downloader downloader;
    private final FeaturesService.ServiceRequirementsBehavior serviceRequirements;

    public SubsystemResolveContext(Subsystem root, RegionDigraph digraph, Repository globalRepository, Downloader downloader, FeaturesService.ServiceRequirementsBehavior serviceRequirements) {
        this.root = root;
        this.globalRepository = globalRepository != null ? new SubsystemRepository(globalRepository) : null;
        this.downloader = downloader;
        this.serviceRequirements = serviceRequirements;

        prepare(root);
        repository = new BaseRepository(resToSub.keySet());

        regions = new HashMap<>();
        for (Region region : digraph) {
            regions.put(region.getName(), region);
        }
        // Add a heuristic to sort capabilities :
        //  if a capability comes from a resource which needs to be installed,
        //  prefer that one over any capabilities from other resources
        distance = computeDistances(root);
    }
    
    public Repository getRepository() {
        return repository;
    }

    public Repository getGlobalRepository() {
        return globalRepository;
    }

    private Map<Resource, Integer> computeDistances(Resource root) {
        Map<Resource, Integer> distance = new HashMap<>();
        Set<Resource> settledNodes = new HashSet<>();
        distance.put(root, 0);
        List<Resource> unSettledNodes = new ArrayList<>();
        unSettledNodes.add(root);

        while (!unSettledNodes.isEmpty()) {
            unSettledNodes.sort(Comparator.comparingInt(r -> distance.getOrDefault(r, Integer.MAX_VALUE)));
            Resource node = unSettledNodes.remove(0);
            if (settledNodes.add(node)) {
                Map<Resource, Integer> edge = computeEdges(node);
                for (Resource target : edge.keySet()) {
                    int d = distance.getOrDefault(node, Integer.MAX_VALUE) + edge.get(target);
                    distance.merge(target, d, Math::min);
                    if (!settledNodes.contains(target)) {
                        unSettledNodes.add(target);
                    }
                }
            }
        }
        return distance;
    }

    private Map<Resource, Integer> computeEdges(Resource resource) {
        Map<Resource, Integer> edges = new HashMap<>();
        String owner = ResolverUtil.getOwnerName(resource);
        for (Requirement req : resource.getRequirements(null)) {
            if (isOptional(req) || isDynamic(req)) {
                continue;
            }
            List<Capability> caps = findProviders(req);
            for (Capability cap : caps) {
                Resource r = cap.getResource();
                // If there's a single provider for any kind of mandatory requirement,
                // this means the resource is also mandatory.
                // Else prefer resources from the same subsystem (with the same owner).
                int v = (caps.size() == 1) ? 0 : (Objects.equals(ResolverUtil.getOwnerName(r), owner)) ? 1 : 10;
                edges.merge(r, v, Math::min);
            }
        }
        return edges;
    }

    private int getResourceCost(Resource resource) {
        return distance.getOrDefault(resource, Integer.MAX_VALUE);
    }

    static boolean isOptional(Requirement req) {
        String resolution = req.getDirectives().get(REQUIREMENT_RESOLUTION_DIRECTIVE);
        return RESOLUTION_OPTIONAL.equals(resolution);
    }

    static boolean isDynamic(Requirement req) {
        String resolution = req.getDirectives().get(REQUIREMENT_RESOLUTION_DIRECTIVE);
        return PackageNamespace.RESOLUTION_DYNAMIC.equals(resolution);
    }

    /**
     * {@link #resToSub} will quickly map all {@link Subsystem#getInstallable() installable resources} to their
     * {@link Subsystem}
     * @param subsystem
     */
    void prepare(Subsystem subsystem) {
        resToSub.put(subsystem, subsystem);
        for (Resource res : subsystem.getInstallable()) {
            resToSub.put(res, subsystem);
        }
        for (Subsystem child : subsystem.getChildren()) {
            prepare(child);
        }
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        return Collections.singleton(root);
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        List<Capability> caps = new ArrayList<>();
        Region requirerRegion = getRegion(requirement.getResource());
        if (requirerRegion != null) {
            Map<Requirement, Collection<Capability>> resMap =
                    repository.findProviders(Collections.singleton(requirement));
            Collection<Capability> res = resMap != null ? resMap.get(requirement) : null;
            if (res != null && !res.isEmpty()) {
                caps.addAll(res);
            } else if (globalRepository != null) {
                // Only bring in external resources for non optional requirements
                if (!RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(RESOLUTION_DIRECTIVE))) {
                    resMap = globalRepository.findProviders(Collections.singleton(requirement));
                    res = resMap != null ? resMap.get(requirement) : null;
                    if (res != null && !res.isEmpty()) {
                        caps.addAll(res);
                    }
                }
            }

            // Use the digraph to prune non visible capabilities
            Visitor visitor = new Visitor(caps);
            requirerRegion.visitSubgraph(visitor);
            Collection<Capability> allowed = visitor.getAllowed();
            caps.retainAll(allowed);
            // Handle cases where the same bundle is requested from both
            // a subsystem and one of its ascendant.  In such cases, we
            // need to remove the one from the child if it can view
            // the parent one
            if (caps.size() > 1) {
                Set<Resource> providers = new HashSet<>();
                for (Capability cap : caps) {
                    Resource resource = cap.getResource();
                    String id = ResolverUtil.getSymbolicName(resource) + "|" + ResolverUtil.getVersion(resource);
                    if (!providers.contains(resource)) {
                        Set<Resource> oldRes = new HashSet<>(providers);
                        providers.clear();
                        String r1 = getRegion(resource).getName();
                        boolean superceded = false;
                        for (Resource r : oldRes) {
                            String id2 = ResolverUtil.getSymbolicName(r) + "|" + ResolverUtil.getVersion(r);
                            if (id.equals(id2)) {
                                String r2 = getRegion(r).getName();
                                if (r1.equals(r2)) {
                                    if (r instanceof BundleRevision) {
                                        providers.add(r);
                                        superceded = true;
                                    } else if (resource instanceof BundleRevision) {
                                        providers.add(resource);
                                    } else {
                                        throw new InternalError();
                                    }
                                } else if (r1.startsWith(r2)) {
                                    providers.add(r);
                                    superceded = true;
                                } else if (r2.startsWith(r1)) {
                                    providers.add(resource);
                                } else {
                                    providers.add(r);
                                }
                            } else {
                                providers.add(r);
                            }
                        }
                        if (!superceded) {
                            providers.add(resource);
                        }
                    }
                }
                caps.removeIf(cap -> !providers.contains(cap.getResource()));
            }
            // Sort caps
            if (distance != null && caps.size() > 1) {
                caps.sort(candidateComparator);
            }
        }
        return caps;
    }

    private Subsystem getSubsystem(Resource resource) {
        return resToSub.get(resource);
    }

    private Region getRegion(Resource resource) {
        if (getSubsystem(resource) != null) {
            return regions.get(getSubsystem(resource).getName());
        }
        return regions.get(root.getName());
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
        int idx = Collections.binarySearch(capabilities, hostedCapability, candidateComparator);
        if (idx < 0) {
            idx = Math.abs(idx + 1);
        }
        capabilities.add(idx, hostedCapability);
        return idx;
    }

    @Override
    public boolean isEffective(Requirement requirement) {
        boolean isServiceReq = ServiceNamespace.SERVICE_NAMESPACE.equals(requirement.getNamespace());
        return !(isServiceReq && FeaturesService.ServiceRequirementsBehavior.Disable == serviceRequirements);
    }

    @Override
    public Map<Resource, Wiring> getWirings() {
        return Collections.emptyMap();
    }

    class Visitor extends AbstractRegionDigraphVisitor<Capability> {

        Visitor(Collection<Capability> candidates) {
            super(candidates);
        }

        @Override
        protected boolean contains(Region region, Capability candidate) {
            return region.equals(getRegion(candidate.getResource()));
        }

        @Override
        protected boolean isAllowed(Capability candidate, RegionFilter filter) {
            if (filter.isAllowed(candidate.getNamespace(), candidate.getAttributes())) {
                return true;
            }
            Resource resource = candidate.getResource();
            List<Capability> identities = resource.getCapabilities(IDENTITY_NAMESPACE);
            if (identities != null && !identities.isEmpty()) {
                Capability identity = identities.iterator().next();
                Map<String, Object> attrs = new HashMap<>();
                attrs.put(BUNDLE_SYMBOLICNAME_ATTRIBUTE, identity.getAttributes().get(IDENTITY_NAMESPACE));
                attrs.put(BUNDLE_VERSION_ATTRIBUTE, identity.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE));
                return filter.isAllowed(VISIBLE_BUNDLE_NAMESPACE, attrs);
            }
            return false;
        }

    }

    class SubsystemRepository implements Repository {

        private final Repository repository;
        private final Map<Subsystem, Map<Capability, Capability>> mapping = new HashMap<>();

        public SubsystemRepository(Repository repository) {
            this.repository = repository;
        }

        @Override
        public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
            Map<Requirement, Collection<Capability>> base = repository.findProviders(requirements);
            Map<Requirement, Collection<Capability>> result = new HashMap<>();
            for (Map.Entry<Requirement, Collection<Capability>> entry : base.entrySet()) {
                List<Capability> caps = new ArrayList<>();
                Subsystem ss = getSubsystem(entry.getKey().getResource());
                while (!ss.isAcceptDependencies()) {
                    ss = ss.getParent();
                }
                Map<Capability, Capability> map = mapping.computeIfAbsent(ss, k -> new HashMap<>());
                for (Capability cap : entry.getValue()) {
                    Capability wrapped = map.get(cap);
                    if (wrapped == null) {
                        wrap(map, ss, cap.getResource());
                        wrapped = map.get(cap);
                    }
                    caps.add(wrapped);
                }
                result.put(entry.getKey(), caps);
            }
            return result;
        }

        @Override
        public Promise<Collection<Resource>> findProviders(RequirementExpression expression) {
            // TODO
            return null;
        }

        @Override
        public ExpressionCombiner getExpressionCombiner() {
            // TODO
            return null;
        }

        @Override
        public RequirementBuilder newRequirementBuilder(String namespace) {
            // TODO
            return null;
        }

        private void wrap(Map<Capability, Capability> map, Subsystem subsystem, Resource resource) {
            ResourceImpl wrapped = new ResourceImpl();
            for (Capability cap : resource.getCapabilities(null)) {
                CapabilityImpl wCap = new CapabilityImpl(wrapped, cap.getNamespace(), cap.getDirectives(), cap.getAttributes());
                map.put(cap, wCap);
                wrapped.addCapability(wCap);
            }
            for (Requirement req : resource.getRequirements(null)) {
                RequirementImpl wReq = new RequirementImpl(wrapped, req.getNamespace(), req.getDirectives(), req.getAttributes());
                wrapped.addRequirement(wReq);
            }
            addIdentityRequirement(wrapped, subsystem, false);
            resToSub.put(wrapped, subsystem);
            try {
                downloader.download(getUri(wrapped), null);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Unable to download resource: " + getUri(wrapped));
            }
        }
    }

}
