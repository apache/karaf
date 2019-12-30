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
package org.apache.karaf.features.extension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

class StoredWiringResolver implements ResolverHook {
    final Map<Long, BundleWires> wiring = new HashMap<>();
    private Path path;

    StoredWiringResolver(Path path) {
        this.path = path;
        load();
    }

    void load() {
        try {
            Files.createDirectories(path);
            Files.list(path).forEach(p -> {
                String name = p.getFileName().toString();
                if (name.matches("[0-9]+")) {
                    long id = Long.parseLong(name);
                    try (BufferedReader reader = Files.newBufferedReader(p)) {
                        wiring.put(id, new BundleWires(id, reader));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void filterResolvable(Collection<BundleRevision> candidates) {
    }

    @Override
    public void filterSingletonCollisions(BundleCapability singleton,
                                          Collection<BundleCapability> collisionCandidates) {
    }

    @Override
    public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
        long[] requirementSourceBundleIds = getRequirementBundleIds(requirement);

        Set<BundleCapability> goodCapabilityCandidates = new HashSet<>();
        for ( long requirementBundleId : requirementSourceBundleIds ) {
            BundleWires bundleWires = wiring.get(requirementBundleId);

            goodCapabilityCandidates.addAll( bundleWires.filterCandidates( requirement, candidates ) );
        }

        candidates.retainAll( goodCapabilityCandidates );
    }

    @Override
    public void end() {
    }

    /**
     * Retrieves the id of the bundle declaring the requirement.
     *
     * In the case of fragment bundles the wiring is made from each Host bundle it is attached,
     * except the osgi.wiring.host requirement itself.
     *
     * @param requirement the requirement being resolved
     *
     * @return the ids of the bundles owning the requirement
     */
    private long[] getRequirementBundleIds(BundleRequirement requirement) {
        long sourceId = requirement.getRevision().getBundle().getBundleId();

        if (isFragment(requirement.getRevision())
            && !requirement.getNamespace().equals(HostNamespace.HOST_NAMESPACE)
            && !requirement.getNamespace().equals(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE)) {

            return wiring.get(sourceId).getFragmentHosts();
        }

        return new long[] { sourceId };
    }

    private static boolean isFragment(Resource resource) {
        for (Capability cap : resource.getCapabilities(null)) {
            if (IdentityNamespace.IDENTITY_NAMESPACE.equals(cap.getNamespace())) {
                return IdentityNamespace.TYPE_FRAGMENT
                    .equals(cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
            }
        }
        return false;
    }

    synchronized void update(Bundle bundle) {
        BundleWires bw = new BundleWires(bundle);
        bw.save(path);
        wiring.put(bundle.getBundleId(), bw);
    }

    synchronized void delete(Bundle bundle) {
        if (wiring.get(bundle.getBundleId()) != null) {
            wiring.remove(bundle.getBundleId()).delete(path);
        }
    }
}
