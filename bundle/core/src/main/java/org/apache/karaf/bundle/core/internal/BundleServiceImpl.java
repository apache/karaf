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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.util.jaas.JaasHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class BundleServiceImpl implements BundleService {

    private static final String KARAF_SYSTEM_BUNDLES_START_LEVEL = "karaf.systemBundlesStartLevel";

    private static Logger LOG = LoggerFactory.getLogger(BundleService.class);

    /**
     * The header key where we store the active wires when we enable DynamicImport=*
     */
    private static final String ORIGINAL_WIRES = "Original-Wires";

    private final BundleContext bundleContext;
    private final List<BundleStateService> stateServices = new CopyOnWriteArrayList<>();

    public BundleServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void registerBundleStateService(BundleStateService service) {
        stateServices.add(service);
    }

    public void unregisterBundleStateService(BundleStateService service) {
        stateServices.remove(service);
    }

    @Override
    public List<Bundle> selectBundles(List<String> ids, boolean defaultAllBundles) {
        return selectBundles(null, ids, defaultAllBundles);
    }

    @Override
    public List<Bundle> selectBundles(String context, List<String> ids, boolean defaultAllBundles) {
        return doSelectBundles(doGetBundleContext(context), ids, defaultAllBundles);
    }

    @Override
    public Bundle getBundle(String id) {
        return getBundle(null, id);
    }

    @Override
    public Bundle getBundle(String context, String id) {
        return doGetBundle(doGetBundleContext(context), id);
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
                message.append(bundleStateService.getName());
                message.append("\n");
                message.append(part);
            }
        }
        if (bundle.getState() == Bundle.INSTALLED) {
            System.out.println("Unsatisfied Requirements:");
            List<BundleRequirement> reqs = getUnsatisfiedRequirements(bundle, null);
            for (BundleRequirement req : reqs) {
                System.out.println(req);
            }
        }
        return message.toString();
    }
    
    @Override
    public List<BundleRequirement> getUnsatisfiedRequirements(Bundle bundle, String namespace) {
        List<BundleRequirement> result = new ArrayList<>();
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

    @Override
    public int getSystemBundleThreshold() {
        int sbsl = 50;
        try {
            final String sbslProp = bundleContext.getProperty(KARAF_SYSTEM_BUNDLES_START_LEVEL);
            if (sbslProp != null) {
                sbsl = Integer.valueOf(sbslProp);
            }
        } catch (Exception ignore) {
            // ignore
        }
        return sbsl;
    }

    private BundleContext doGetBundleContext(String context) {
        if (context == null || context.trim().isEmpty()) {
            return bundleContext;
        } else {
            List<Bundle> bundles = new BundleSelectorImpl(bundleContext).selectBundles(Collections.singletonList(context), false);
            if (bundles.isEmpty()) {
                throw new IllegalArgumentException("Context " + context + " does not evaluate to a bundle");
            } else if (bundles.size() > 1) {
                throw new IllegalArgumentException("Context " + context + " is ambiguous");
            }
            BundleContext bundleContext = bundles.get(0).getBundleContext();
            if (bundleContext == null) {
                throw new IllegalArgumentException("Context " + context + " is not resolved");
            }
            return bundleContext;
        }
    }

    private Bundle doGetBundle(BundleContext bundleContext, String id) {
        List<Bundle> bundles = doSelectBundles(bundleContext, Collections.singletonList(id), false);
        if (bundles.isEmpty()) {
            throw new IllegalArgumentException("Bundle " + id + " does not match any bundle");
        } else {
            List<Bundle> filtered = filter(bundles);
            if (filtered.isEmpty()) {
                throw new IllegalArgumentException("Access to bundle " + id + " is forbidden");
            } else if (filtered.size() > 1) {
                throw new IllegalArgumentException("Multiple bundles matching " + id);
            }
            return filtered.get(0);
        }
    }

    private List<Bundle> doSelectBundles(BundleContext bundleContext, List<String> ids, boolean defaultAllBundles) {
        return filter(new BundleSelectorImpl(bundleContext).selectBundles(ids, defaultAllBundles));
    }

    private List<Bundle> filter(List<Bundle> bundles) {
        if (JaasHelper.currentUserHasRole(BundleService.SYSTEM_BUNDLES_ROLE)) {
            return bundles;
        }
        int sbsl = getSystemBundleThreshold();
        List<Bundle> filtered = new ArrayList<>();
        for (Bundle bundle : bundles) {
            int level = bundle.adapt(BundleStartLevel.class).getStartLevel();
            if (level >= sbsl) {
                filtered.add(bundle);
            }
        }
        return filtered;
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
        for (String original : bundle.getHeaders().get(ORIGINAL_WIRES).split(",")) {
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
        StringBuilder result = new StringBuilder();
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
        Map<String, Bundle> exporters = new HashMap<>();

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

    @Override
    public String getStatus(String id) {
        Bundle bundle = getBundle(id);
        return getState(bundle);
    }

    /**
     * Return a String representing current bundle state
     *
     * @param bundle the bundle
     * @return bundle state String
     */
    private String getState(Bundle bundle) {
        switch (bundle.getState()) {
            case Bundle.UNINSTALLED:
                return "Uninstalled";
            case Bundle.INSTALLED:
                return "Installed";
            case Bundle.RESOLVED:
                return "Resolved";
            case Bundle.STARTING:
                return "Starting";
            case Bundle.STOPPING:
                return "Stopping";
            case Bundle.ACTIVE:
                return "Active";
            default:
                return "Unknown";
        }
    }

}