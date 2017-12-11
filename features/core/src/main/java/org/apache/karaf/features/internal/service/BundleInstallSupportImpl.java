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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.region.DigraphHelper;
import org.apache.karaf.util.bundles.BundleUtils;
import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interaction with OSGi framework, where bundles are installed into it via {@link RegionDigraph}. After a bundle
 * is installed, it may be controlled in standard way via {@link Bundle} interface.
 */
public class BundleInstallSupportImpl implements BundleInstallSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(BundleInstallSupportImpl.class);
    
    private final RegionDigraph digraph;
    private final Bundle ourBundle;
    private final BundleContext ourBundleContext;
    private final FeatureConfigInstaller configInstaller;
    
    /**
     * The system bundle context.
     * For all bundles related operations, we use the system bundle context
     * to allow this bundle to be stopped and still allow the deployment to
     * take place.
     */
    private final BundleContext systemBundleContext;

    private Map<Thread, ResolverHook> hooks = new ConcurrentHashMap<>();
    private ServiceRegistration<ResolverHookFactory> hookRegistration;

    public BundleInstallSupportImpl(Bundle ourBundle,
                   BundleContext ourBundleContext,
                   BundleContext systemBundleContext,
                   FeatureConfigInstaller configInstaller,
                   RegionDigraph digraph) {
        this.ourBundle = ourBundle;
        this.ourBundleContext = ourBundleContext;
        this.systemBundleContext = systemBundleContext;
        this.configInstaller = configInstaller;
        this.digraph = digraph;
        if (systemBundleContext != null) {
            hookRegistration = systemBundleContext.registerService(ResolverHookFactory.class,
                    triggers -> hooks.get(Thread.currentThread()), null);
        }

    }

    public void unregister() {
        if (hookRegistration != null) {
            hookRegistration.unregister();
        }
    }
    
    public void print(String message, boolean verbose) {
        LOGGER.info(message);
        if (verbose) {
            System.out.println(message);
        }
    }

    @Override
    public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        FrameworkWiring fw = systemBundleContext.getBundle().adapt(FrameworkWiring.class);
        fw.refreshBundles(bundles, (FrameworkListener) event -> {
            if (event.getType() == FrameworkEvent.ERROR) {
                LOGGER.error("Framework error", event.getThrowable());
            }
            latch.countDown();
        });
        latch.await();
    }

    @Override
    public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
        if (FeaturesService.ROOT_REGION.equals(region)) {
            return digraph.getRegion(region).installBundleAtLocation(uri, is);
        } else {
            return digraph.getRegion(region).installBundle(uri, is);
        }
    }

    @Override
    public void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException {
        // We need to wrap the bundle to insert a Bundle-UpdateLocation header
        try {
            File file = BundleUtils.fixBundleWithUpdateLocation(is, uri);
            bundle.update(new FileInputStream(file));
            file.delete();
        } catch (IOException e) {
            throw new BundleException("Unable to update bundle", e);
        }
    }

    @Override
    public void uninstall(Bundle bundle) throws BundleException {
        bundle.uninstall();
    }

    @Override
    public void startBundle(Bundle bundle) throws BundleException {
        if (bundle != this.ourBundle || bundle.getState() != Bundle.STARTING) {
            bundle.start();
        }
    }

    @Override
    public void stopBundle(Bundle bundle, int options) throws BundleException {
        bundle.stop(options);
    }

    @Override
    public void setBundleStartLevel(Bundle bundle, int startLevel) {
        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
    }

    @Override
    public void resolveBundles(Set<Bundle> bundles, final Map<Resource, List<Wire>> wiring, Map<Resource, Bundle> resToBnd) {
        // Make sure it's only used for us
        final Thread thread = Thread.currentThread();
        // Translate wiring
        final Map<Bundle, Resource> bndToRes = new HashMap<>();
        for (Resource res : resToBnd.keySet()) {
            bndToRes.put(resToBnd.get(res), res);
        }
        // Hook
        final ResolverHook hook = new ResolverHook() {
            @Override
            public void filterResolvable(Collection<BundleRevision> candidates) {
            }
            @Override
            public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
            }
            @Override
            public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
                if (Thread.currentThread() == thread) {
                    // osgi.ee capabilities are provided by the system bundle, so just ignore those
                    if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE
                            .equals(requirement.getNamespace())) {
                        return;
                    }
                    Bundle sourceBundle = requirement.getRevision().getBundle();
                    Resource sourceResource = bndToRes.get(sourceBundle);
                    List<Wire> wires = wiring.get(sourceResource);
                    if (sourceBundle == null || wires == null) {
                        // This could be a bundle external to this resolution which
                        // is being resolve at the same time, so do not interfere
                        return;
                    }
                    Set<Resource> wired = new HashSet<>();
                    // Get a list of allowed wired resources
                    wired.add(sourceResource);
                    for (Wire wire : wires) {
                        wired.add(wire.getProvider());
                        if (HostNamespace.HOST_NAMESPACE.equals(wire.getRequirement().getNamespace())) {
                            for (Wire hostWire : wiring.get(wire.getProvider())) {
                                wired.add(hostWire.getProvider());
                            }
                        }
                    }
                    // Remove candidates that are not allowed
                    for (Iterator<BundleCapability> candIter = candidates.iterator(); candIter.hasNext(); ) {
                        BundleCapability cand = candIter.next();
                        BundleRevision br = cand.getRevision();
                        if ((br.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
                            br = br.getWiring().getRequiredWires(null).get(0).getProvider();
                        }
                        Resource res = bndToRes.get(br.getBundle());
                        if (!wired.contains(br) && !wired.contains(res)) {
                            candIter.remove();
                        }
                    }
                }
            }
            @Override
            public void end() {
            }
        };
        hooks.put(Thread.currentThread(), hook);
        try {
            FrameworkWiring frameworkWiring = systemBundleContext.getBundle().adapt(FrameworkWiring.class);
            frameworkWiring.resolveBundles(bundles);
        } finally {
            hooks.remove(Thread.currentThread());
        }
    }

    @Override
    public void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies, Map<String, Set<Long>> bundles) throws BundleException, InvalidSyntaxException {
        RegionDigraph temp = digraph.copy();
        // Remove everything
        for (Region region : temp.getRegions()) {
            temp.removeRegion(region);
        }
        // Re-create regions
        for (String name : policies.keySet()) {
            temp.createRegion(name);
        }
        // Dispatch bundles
        for (Map.Entry<String, Set<Long>> entry : bundles.entrySet()) {
            Region region = temp.getRegion(entry.getKey());
            for (long bundleId : entry.getValue()) {
                region.addBundle(bundleId);
            }
        }
        // Add policies
        for (Map.Entry<String, Map<String, Map<String, Set<String>>>> entry1 : policies.entrySet()) {
            Region region1 = temp.getRegion(entry1.getKey());
            for (Map.Entry<String, Map<String, Set<String>>> entry2 : entry1.getValue().entrySet()) {
                Region region2 = temp.getRegion(entry2.getKey());
                RegionFilterBuilder rfb = temp.createRegionFilterBuilder();
                for (Map.Entry<String, Set<String>> entry3 : entry2.getValue().entrySet()) {
                    for (String flt : entry3.getValue()) {
                        rfb.allow(entry3.getKey(), flt);
                    }
                }
                region1.connectRegion(region2, rfb.build());
            }
        }
        // Verify that no other bundles have been installed externally in the mean time
        DigraphHelper.verifyUnmanagedBundles(systemBundleContext, temp);
        // Do replace
        digraph.replace(temp);
    }
    
    @Override
    public void saveDigraph() {
        DigraphHelper.saveDigraph(getDataFile(DigraphHelper.DIGRAPH_FILE), digraph);
    }
    
    @Override
    public RegionDigraph getDiGraphCopy() throws BundleException {
        return digraph.copy();
    }

    @Override
    public void installConfigs(Feature feature) throws IOException, InvalidSyntaxException {
        if (configInstaller != null) {
            configInstaller.installFeatureConfigs(feature);
        }
    }

    @Override
    public void installLibraries(Feature feature) {
        // TODO: install libraries
    }

    @Override
    public File getDataFile(String fileName) {
        return ourBundleContext.getDataFile(fileName);
    }

    @Override
    public FrameworkInfo getInfo() {
        FrameworkInfo info = new FrameworkInfo();
        info.ourBundle = ourBundle;
        FrameworkStartLevel fsl = systemBundleContext.getBundle().adapt(FrameworkStartLevel.class);
        info.initialBundleStartLevel = fsl.getInitialBundleStartLevel();
        info.currentStartLevel = fsl.getStartLevel();
        info.bundles = new HashMap<>();
        for (Bundle bundle : systemBundleContext.getBundles()) {
            info.bundles.put(bundle.getBundleId(), bundle);
        }
        info.systemBundle = info.bundles.get(0L);
        return info;
    }
}
