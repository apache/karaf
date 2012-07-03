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
package org.apache.karaf.bundle.core.internal;

import static java.lang.String.format;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleServiceImpl implements BundleService {
    private static Logger LOG = LoggerFactory.getLogger(BundleService.class);
    /**
     * The header key where we store the active wires when we enable DynamicImport=*
     */
    private static final String ORIGINAL_WIRES = "Original-Wires";

    private final BundleContext bundleContext;
    private final List<BundleStateService> stateServices;

    public BundleServiceImpl(BundleContext bundleContext, List<BundleStateService> stateServices) {
        this.bundleContext = bundleContext;
        this.stateServices = stateServices;
    }

    @Override
    public List<Bundle> selectBundles(List<String> ids, boolean defaultAllBundles) {
        return new BundleSelectorImpl(bundleContext).selectBundles(ids, defaultAllBundles);
    }

    @Override
    public BundleInfo getInfo(Bundle bundle) {
        BundleState combinedState = BundleState.Unknown;
        for (BundleStateService stateService : this.stateServices) {
            BundleState extState = stateService.getState(bundle);
            if (extState != BundleState.Unknown) {
                combinedState = extState;
            }
        }
        return  new BundleInfoImpl(bundle, combinedState);
    }

    @Override
    public String getDiag(Bundle bundle) {
        StringBuilder message = new StringBuilder();
        for (BundleStateService bundleStateService : stateServices) {
            String part = bundleStateService.getDiag(bundle);
            if (part != null) {
                message.append(bundleStateService.getName() + "\n");
                message.append(part);
            }
        }
        if (bundle.getState() == Bundle.INSTALLED) {
            System.out.println("Unsatisfied Requirements:");
            List<BundleRequirement> reqs = getUnsatisfiedRquirements(bundle, null);
            for (BundleRequirement req : reqs) {
                System.out.println(req);
            }
        }
        return message.toString();
    }
    
    @Override
    public List<BundleRequirement> getUnsatisfiedRquirements(Bundle bundle, String namespace) {
        List<BundleRequirement> result = new ArrayList<BundleRequirement>();
        BundleRevision rev = bundle.adapt(BundleRevision.class);
        if (rev != null) {
            List<BundleRequirement> reqs = rev.getDeclaredRequirements(namespace);
            for (BundleRequirement req : reqs) {
                if (!canBeSatisfied(req)) {
                    result.add(req);
                }
            }
        }
        return result;
    }
    
    private boolean canBeSatisfied(BundleRequirement req) {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null) {
                List<BundleCapability> caps = wiring.getCapabilities(null);
                for (BundleCapability cap : caps) {
                    if (req.matches(cap)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<Bundle> getBundlesByURL(String urlFilter) {
        return new BundleSelectorImpl(bundleContext).getBundlesByURL(urlFilter);
    }

    /*
     * Enable DynamicImport=* on the bundle
     */
    public void enableDynamicImports(Bundle bundle) {
        String location =
                String.format("wrap:%s$" +
                        "Bundle-UpdateLocation=%s&" +
                        "DynamicImport-Package=*&" +
                        "%s=%s&" +
                        "overwrite=merge",
                        bundle.getLocation(),
                        bundle.getLocation(),
                        ORIGINAL_WIRES,
                        explode(getWiredBundles(bundle).keySet()));
        LOG.debug(format("Updating %s with URL %s", bundle, location));

        try {
            URL url = new URL(location);
            bundle.update(url.openStream());
            bundleContext.getBundle(0).adapt(FrameworkWiring.class).refreshBundles(Collections.singleton(bundle));
        } catch (Exception e) {
            throw new RuntimeException("Error enabling dynamic imports on bundle" + bundle.getBundleId(), e);
        }
    }

    /*
     * Disable DynamicImport=* on the bundle
     *
     * At this time, we will also calculate the difference in package wiring for the bundle compared to
     * when we enabled the DynamicImport
     */
    public void disableDynamicImports(Bundle bundle) {
        Set<String> current = getWiredBundles(bundle).keySet();
        for (String original : bundle.getHeaders().get(ORIGINAL_WIRES).toString().split(",")) {
            current.remove(original);
        }

        if (current.isEmpty()) {
            LOG.debug("No additional packages have been wired since dynamic import was enabled");
        } else {
            LOG.debug("Additional packages wired since dynamic import was enabled");
            for (String pkg : current) {
                LOG.debug("- " + pkg);
            }
        }

        try {
            bundle.update();
        } catch (BundleException e) {
            throw new RuntimeException("Error disabling dynamic imports on bundle" + bundle.getBundleId(), e);
        }
    }
    
    /*
     * Explode a set of string values in to a ,-delimited string
     */
    private String explode(Set<String> set) {
        StringBuffer result = new StringBuffer();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(",");
            }
        }
        if (result.length() == 0) {
            return "--none--";
        }
        return result.toString();
    }
    
    /*
     * Get the list of bundles from which the given bundle imports packages
     */
    public Map<String, Bundle> getWiredBundles(Bundle bundle) {
        // the set of bundles from which the bundle imports packages
        Map<String, Bundle> exporters = new HashMap<String, Bundle>();

        for (BundleRevision revision : bundle.adapt(BundleRevisions.class).getRevisions()) {
            BundleWiring wiring = revision.getWiring();
            if (wiring != null) {
                List<BundleWire> wires = wiring.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
                if (wires != null) {
                    for (BundleWire wire : wires) {
                        if (wire.getProviderWiring().getBundle().getBundleId() != 0) {
                            exporters.put(wire.getCapability().getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString(),
                                          wire.getProviderWiring().getBundle());
                        }
                    }
                }
            }
        }
        return exporters;
    }

    @Override
    public boolean isDynamicImport(Bundle bundle) {
        return bundle.getHeaders().get(ORIGINAL_WIRES) != null;
    }
}