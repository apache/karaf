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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.resolver.Util;
import org.apache.karaf.features.internal.repository.StaticRepository;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.osgi.framework.BundleException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import static org.eclipse.equinox.region.RegionFilter.VISIBLE_BUNDLE_NAMESPACE;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE;
import static org.osgi.framework.Constants.BUNDLE_VERSION_ATTRIBUTE;
import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;
import static org.osgi.framework.namespace.IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;

public class SubsystemResolveContext extends ResolveContext {

    private final Subsystem root;
    private final RegionDigraph digraph;
    private final CandidateComparator candidateComparator = new CandidateComparator();

    private final Map<Resource, Subsystem> resToSub = new HashMap<Resource, Subsystem>();
    private final Repository repository;


    public SubsystemResolveContext(Subsystem root, RegionDigraph digraph) throws BundleException {
        this.root = root;
        this.digraph = digraph;

        prepare(root);
        repository = new StaticRepository(resToSub.keySet());
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
        List<Capability> caps = new ArrayList<Capability>();
        Region requirerRegion = getRegion(requirement.getResource());
        if (requirerRegion != null) {
            Map<Requirement, Collection<Capability>> resMap =
                    repository.findProviders(Collections.singleton(requirement));
            Collection<Capability> res = resMap != null ? resMap.get(requirement) : null;
            if (res != null) {
                caps.addAll(res);
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
                Map<String, Resource> providers = new HashMap<String, Resource>();
                for (Capability cap : caps) {
                    Resource resource = cap.getResource();
                    String id = Util.getSymbolicName(resource) + "|" + Util.getVersion(resource);
                    Resource prev = providers.get(id);
                    if (prev != null && prev != resource) {
                        String r1 = getRegion(prev).getName();
                        String r2 = getRegion(resource).getName();
                        int c = r1.compareTo(r2);
                        if (c == 0) {
                            // This should never happen because resource have been
                            // de-duplicated during the pre-resolution phase.
                            throw new IllegalStateException();
                        }
                        resource = c < 0 ? prev : resource;
                    }
                    providers.put(id, resource);
                }
                for (Iterator<Capability> it = caps.iterator(); it.hasNext();) {
                    Capability cap = it.next();
                    if (!providers.values().contains(cap.getResource())) {
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
        return digraph.getRegion(getSubsystem(resource).getName());
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
        String resolution = requirement.getDirectives().get(RESOLUTION_DIRECTIVE);
        return requirement.getNamespace().equals(IDENTITY_NAMESPACE)
                || !RESOLUTION_OPTIONAL.equals(resolution);
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

}
