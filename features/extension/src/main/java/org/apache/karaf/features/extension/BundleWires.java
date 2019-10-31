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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

class BundleWires {
    long bundleId;
    Map<String, Set<String>> wiring = new HashMap<>();

    BundleWires(Bundle bundle) {
        this.bundleId = bundle.getBundleId();
        for (BundleWire wire : bundle.adapt(BundleWiring.class).getRequiredWires(null)) {
            String requirementId = getRequirementId(wire.getRequirement());
            String capabilityId = getCapabilityId(wire.getCapability());

            Set<String> capabilityIds = this.wiring.computeIfAbsent( requirementId, key -> new HashSet<>() );
            capabilityIds.add(capabilityId);
        }
    }

    BundleWires(long bundleId, BufferedReader reader) throws IOException {
        this.bundleId = bundleId;
        while (true) {
            String key = reader.readLine();
            String val = reader.readLine();
            if (key != null && val != null) {
                Set<String> capabilityIds = this.wiring.computeIfAbsent( key, k -> new HashSet<>() );
                capabilityIds.add(val);
            } else {
                break;
            }
        }
    }

    void save(Path path) {
        try {
            Files.createDirectories(path);
            Path file = path.resolve(Long.toString(this.bundleId));
            Files.createDirectories(file.getParent());
            try (BufferedWriter fw = Files.newBufferedWriter(file, TRUNCATE_EXISTING, WRITE, CREATE)) {
                for (Map.Entry<String, Set<String>> wires : wiring.entrySet()) {
                    String requirementId = wires.getKey();
                    Set<String> capabilityIds = wires.getValue();

                    for ( String capabilityId : capabilityIds ) {
                        fw.append( requirementId ).append( '\n' );
                        fw.append( capabilityId ).append( '\n' );
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void delete(Path path) {
        try {
            Files.createDirectories(path);
            Path file = path.resolve(Long.toString(this.bundleId));
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    long[] getFragmentHosts() {
        return wiring.entrySet().stream() //
            .filter(e -> e.getKey().startsWith(HostNamespace.HOST_NAMESPACE)) //
            .map(Map.Entry::getValue) //
            .flatMap( Set::stream ) //
            .mapToLong(this::getBundleId).toArray();
    }
    
    private long getBundleId(String value) {
        int idx = value.indexOf(';');
        if (idx > 0) {
            value = value.substring(0, idx);
        }
        return Long.parseLong(value.trim());
    }

    Set<BundleCapability> filterCandidates( BundleRequirement requirement, Collection<BundleCapability> candidates) {
        Set<String> wiredCapabilityIds = wiring.get(getRequirementId(requirement));

        return candidates.stream() //
            .filter( capability -> isCapabilityWiredToBundle( wiredCapabilityIds, capability ) ) //
            .collect( Collectors.toSet() );
    }

    private boolean isCapabilityWiredToBundle( Set<String> capabilityIds, BundleCapability capability) {
        return
            // is this bundle wired to the candidate capability?
            (capabilityIds != null && capabilityIds.contains(getCapabilityId(capability)))
            // if not and the bundle has no wirings to the capability check if itself satisfies it
            || (capabilityIds == null && capability.getRevision().getBundle().getBundleId() == this.bundleId);
    }

    private String getRequirementId(Requirement requirement) {
        String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        if (filter != null) {
            return requirement.getNamespace() + "; " + filter;
        } else {
            return requirement.getNamespace();
        }
    }

    private String getCapabilityId(BundleCapability capability) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(capability.getRevision().getBundle().getBundleId());
        Object v = capability.getAttributes().get(Constants.VERSION_ATTRIBUTE);
        if (v != null) {
            sb.append("; version=").append(v.toString());
        }
        return sb.toString();
    }
}
