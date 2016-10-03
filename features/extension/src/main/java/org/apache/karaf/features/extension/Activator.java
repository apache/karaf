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

import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class Activator implements BundleActivator, ResolverHook, SynchronousBundleListener, ResolverHookFactory {

    private static final String WIRING_PATH = "wiring";

    private final Map<Long, Map<String, String>> wiring = new HashMap<>();
    private BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        load();
        context.addBundleListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getBundle().getBundleId() == 0 && event.getType() == BundleEvent.STARTED) {
            ServiceRegistration<ResolverHookFactory> registration = bundleContext.registerService(ResolverHookFactory.class, this, null);
            try {
                List<Bundle> bundles = wiring.keySet().stream()
                        .map(id -> bundleContext.getBundle(id))
                        .collect(Collectors.toList());
                bundleContext.getBundle().adapt(FrameworkWiring.class)
                        .resolveBundles(bundles);
            } finally {
                registration.unregister();
            }
        } else if (event.getType() == BundleEvent.RESOLVED || event.getType() == BundleEvent.UNRESOLVED) {
            synchronized (wiring) {
                long id = event.getBundle().getBundleId();
                if (event.getType() == BundleEvent.RESOLVED) {
                    Map<String, String> bw = new HashMap<>();
                    for (BundleWire wire : event.getBundle().adapt(BundleWiring.class).getRequiredWires(null)) {
                        bw.put(getRequirementId(wire.getRequirement()), getCapabilityId(wire.getCapability()));
                    }
                    wiring.put(id, bw);
                    saveWiring(id, bw);
                } else {
                    wiring.remove(id);
                    saveWiring(id, null);
                }
            }
        }
    }

    @Override
    public void filterResolvable(Collection<BundleRevision> candidates) {
    }

    @Override
    public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
    }

    @Override
    public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
        long sourceId = requirement.getRevision().getBundle().getBundleId();
        if (isFragment(requirement.getRevision())
                && !requirement.getNamespace().equals(HostNamespace.HOST_NAMESPACE)) {
            sourceId = wiring.get(sourceId).entrySet().stream()
                    .filter(e -> e.getKey().startsWith(HostNamespace.HOST_NAMESPACE))
                    .map(Map.Entry::getValue)
                    .mapToLong(s -> {
                        int idx = s.indexOf(';');
                        if (idx > 0) {
                            s = s.substring(0, idx);
                        }
                        return Long.parseLong(s.trim());
                    })
                    .findFirst()
                    .orElse(-1);
        }
        Map<String, String> bw = wiring.get(sourceId);
        String cap = bw.get(getRequirementId(requirement));
        for (Iterator<BundleCapability> candIter = candidates.iterator(); candIter.hasNext();) {
            BundleCapability cand = candIter.next();
            if (cap != null && !cap.equals(getCapabilityId(cand))
                    || cap == null && cand.getRevision() != requirement.getRevision()) {
                candIter.remove();
            }
        }
    }

    @Override
    public void end() {
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        return this;
    }

    private void load() {
        try {
            Path dir = bundleContext.getDataFile(WIRING_PATH).toPath();
            Files.createDirectories(dir);
            Files.list(dir).forEach(p -> {
                String name = p.getFileName().toString();
                if (name.matches("[0-9]+")) {
                    try (BufferedReader reader = Files.newBufferedReader(p)) {
                        long id = Long.parseLong(name);
                        Map<String, String> map = new HashMap<>();
                        while (true) {
                            String key = reader.readLine();
                            String val = reader.readLine();
                            if (key != null && val != null) {
                                map.put(key, val);
                            } else {
                                break;
                            }
                        }
                        wiring.put(id, map);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveWiring(long id, Map<String, String> wiring) {
        try {
            Path dir = bundleContext.getDataFile(WIRING_PATH).toPath();
            Files.createDirectories(dir);
            Path file = dir.resolve(Long.toString(id));
            if (wiring != null) {
                Files.createDirectories(file.getParent());
                try (BufferedWriter fw = Files.newBufferedWriter(file,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE)) {
                    for (Map.Entry<String, String> wire : wiring.entrySet()) {
                        fw.append(wire.getKey()).append('\n');
                        fw.append(wire.getValue()).append('\n');
                    }
                }
            } else {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private static boolean isFragment(Resource resource) {
        for (Capability cap : resource.getCapabilities(null)) {
            if (IdentityNamespace.IDENTITY_NAMESPACE.equals(cap.getNamespace())) {
                return IdentityNamespace.TYPE_FRAGMENT.equals(
                        cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
            }
        }
        return false;
    }
}
