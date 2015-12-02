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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.download.Downloader;
import org.apache.karaf.features.internal.repository.BaseRepository;
import org.apache.karaf.features.internal.resolver.CapabilityImpl;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.ResolverUtil;
import org.apache.karaf.features.internal.resolver.ResourceImpl;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.BundleException;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

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
    private final Set<Resource> mandatory = new HashSet<>();
    private final CandidateComparator candidateComparator = new CandidateComparator(mandatory);

    private final Map<Resource, Subsystem> resToSub = new HashMap<Resource, Subsystem>();
    private final Repository repository;
    private final Repository globalRepository;
    private final Downloader downloader;
    private final String serviceRequirements;

    public SubsystemResolveContext(Subsystem root, RegionDigraph digraph, Repository globalRepository, Downloader downloader, String serviceRequirements) throws BundleException {
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
        findMandatory();
    }
    
    public Repository getRepository() {
        return repository;
    }

    public Repository getGlobalRepository() {
        return globalRepository;
    }

    void findMandatory() {
        mandatory.add(root);
        int nbMandatory;
        // Iterate while we find more mandatory resources
        do {
            nbMandatory = mandatory.size();
            for (Resource res : new ArrayList<>(mandatory)) {
                // Check mandatory requirements of mandatory resources
                for (Requirement req : res.getRequirements(null)) {
                    if (isOptional(req) || isDynamic(req)) {
                        continue;
                    }
                    List<Capability> caps = findProviders(req);
                    // If there's a single provider for any kind of mandatory requirement,
                    // this means the resource is also mandatory
                    if (caps.size() == 1) {
                        mandatory.add(caps.get(0).getResource());
                    } else {
                        // In case there are multiple providers
                        // check if there is a single provider which has
                        // a mandatory identity requirement on a mandatory
                        // resource, in which case we also assume this one
                        // is mandatory
                        Set<Resource> mand = new HashSet<>();
                        for (Capability cap : caps) {
                            Resource r = cap.getResource();
                            if (mandatory.contains(r)) {
                                mand.add(r);
                            } else {
                                for (Requirement req2 : r.getRequirements(null)) {
                                    if (!IDENTITY_NAMESPACE.equals(req2.getNamespace()) || isOptional(req2) || isDynamic(req2)) {
                                        continue;
                                    }
                                    List<Capability> caps2 = findProviders(req2);
                                    if (caps2.size() == 1) {
                                        Resource r2 =  caps2.get(0).getResource();
                                        if (mandatory.contains(r2)) {
                                            mand.add(r);
                                        }
                                    }
                                }
                            }
                        }
                        if (mand.size() == 1) {
                            mandatory.add(mand.iterator().next());
                        } else {
                            mand.clear();
                        }
                    }
                }
            }
        } while (mandatory.size() != nbMandatory);
    }

    static boolean isOptional(Requirement req) {
        String resolution = req.getDirectives().get(REQUIREMENT_RESOLUTION_DIRECTIVE);
        return RESOLUTION_OPTIONAL.equals(resolution);
    }

    static boolean isDynamic(Requirement req) {
        String resolution = req.getDirectives().get(REQUIREMENT_RESOLUTION_DIRECTIVE);
        return PackageNamespace.RESOLUTION_DYNAMIC.equals(resolution);
    }

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
        return Collections.<Resource>singleton(root);
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
                        Set<Resource> newRes = new HashSet<>();
                        String r1 = getRegion(resource).getName();
                        boolean superceded = false;
                        for (Resource r : providers) {
                            String id2 = ResolverUtil.getSymbolicName(r) + "|" + ResolverUtil.getVersion(r);
                            if (id.equals(id2)) {
                                String r2 = getRegion(r).getName();
                                if (r1.equals(r2)) {
                                    if (r instanceof BundleRevision) {
                                        newRes.add(r);
                                        superceded = true;
                                    } else if (resource instanceof BundleRevision) {
                                        newRes.add(resource);
                                    } else {
                                        throw new InternalError();
                                    }
                                } else if (r1.startsWith(r2)) {
                                    newRes.add(r);
                                    superceded = true;
                                } else if (r2.startsWith(r1)) {
                                    newRes.add(resource);
                                } else {
                                    newRes.add(r);
                                }
                            } else {
                                newRes.add(r);
                            }
                        }
                        if (!superceded) {
                            newRes.add(resource);
                        }
                        providers = newRes;
                    }
                }
                for (Iterator<Capability> it = caps.iterator(); it.hasNext();) {
                    Capability cap = it.next();
                    if (!providers.contains(cap.getResource())) {
                        it.remove();
                    }
                }
            }
            // Sort caps
            Collections.sort(caps, candidateComparator);
        }
        return caps;
    }

    private Subsystem getSubsystem(Resource resource) {
        return resToSub.get(resource);
    }

    private Region getRegion(Resource resource) {
        return regions.get(getSubsystem(resource).getName());
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
        return !(isServiceReq && FeaturesService.SERVICE_REQUIREMENTS_DISABLE.equals(serviceRequirements));
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
                Map<String, Object> attrs = new HashMap<String, Object>();
                attrs.put(BUNDLE_SYMBOLICNAME_ATTRIBUTE, identity.getAttributes().get(IDENTITY_NAMESPACE));
                attrs.put(BUNDLE_VERSION_ATTRIBUTE, identity.getAttributes().get(CAPABILITY_VERSION_ATTRIBUTE));
                return filter.isAllowed(VISIBLE_BUNDLE_NAMESPACE, attrs);
            }
            return false;
        }

    }

    class SubsystemRepository implements Repository {

        private final Repository repository;
        private final Map<Subsystem, Map<Capability, Capability>> mapping = new HashMap<Subsystem, Map<Capability, Capability>>();

        public SubsystemRepository(Repository repository) {
            this.repository = repository;
        }

        @Override
        public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
            Map<Requirement, Collection<Capability>> base = repository.findProviders(requirements);
            Map<Requirement, Collection<Capability>> result = new HashMap<Requirement, Collection<Capability>>();
            for (Map.Entry<Requirement, Collection<Capability>> entry : base.entrySet()) {
                List<Capability> caps = new ArrayList<Capability>();
                Subsystem ss = getSubsystem(entry.getKey().getResource());
                while (!ss.isAcceptDependencies()) {
                    ss = ss.getParent();
                }
                Map<Capability, Capability> map = mapping.get(ss);
                if (map == null) {
                    map = new HashMap<Capability, Capability>();
                    mapping.put(ss, map);
                }
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
