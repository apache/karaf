/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.dev.core.internal;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.dev.core.DevService;
import org.apache.karaf.dev.core.FrameworkType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevServiceImpl implements DevService {
    private static Logger LOG = LoggerFactory.getLogger(DevServiceImpl.class);
    /**
     * The header key where we store the active wires when we enable DynamicImport=*
     */
    private static final String ORIGINAL_WIRES = "Original-Wires";

    private BundleContext bundleContext;

    public DevServiceImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public FrameworkType getFramework() {
        if (bundleContext.getBundle(0).getSymbolicName().contains("felix")) {
            return FrameworkType.felix;
        } else {
            return FrameworkType.equinox;
        }
    }

    private Properties loadProps() throws IOException {
        return new Properties(new File(System.getProperty("karaf.base"), "etc/config.properties"));
    }

    public void setFramework(FrameworkType framework) {
        if (framework == null) {
            return;
        }
        try {
            Properties properties = loadProps();
            properties.put("karaf.framework", framework.name());
            properties.save();
        } catch (IOException e) {
            throw new RuntimeException("Error settting framework: " + e.getMessage(), e);
        }
    }

    public void setFrameworkDebug(boolean debug) {
        try {
            Properties properties = loadProps();
            if (debug) {
                properties.put("felix.log.level", "4");
                properties.put("osgi.debug", "etc/equinox-debug.properties");
            } else {
                properties.remove("felix.log.level");
                properties.remove("osgi.debug");
            }
            // TODO populate the equinox-debug.properties file with the one provided in shell/dev module
            properties.save();
        } catch (IOException e) {
            throw new RuntimeException("Error settting framework debugging: " + e.getMessage(), e);
        }
    }

    public void restart(boolean clean) {
        System.setProperty("karaf.restart", "true");
        System.setProperty("karaf.restart.clean", Boolean.toString(clean));
        try {
            bundleContext.getBundle(0).stop();
        } catch (BundleException e) {
            throw new RuntimeException("Error stopping framework bundle: " + e.getMessage(), e);
        }
    }

    @Override
    public String setSystemProperty(String key, String value, boolean persist) {
        if (persist) {
            try {
                String base = System.getProperty("karaf.base");
                Properties props = new Properties(new File(base, "etc/system.properties"));
                props.put(key, value);
                props.save();
            } catch (IOException e) {
                throw new RuntimeException("Error persisting system property", e);
            }
        }
        return System.setProperty(key, value);
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
