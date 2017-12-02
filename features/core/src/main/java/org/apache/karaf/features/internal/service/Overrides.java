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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.resolver.ResolverUtil.getSymbolicName;
import static org.apache.karaf.features.internal.resolver.ResolverUtil.getVersion;

/**
 * Helper class to deal with overriden bundles at feature installation time.
 */
public final class Overrides {

    public static final String OVERRIDE_RANGE = "range";

    private static final Logger LOGGER = LoggerFactory.getLogger(Overrides.class);

    private Overrides() {
    }

    /**
     * <p>Compute a list of bundles to install, taking into account overrides</p>.
     *
     * <p>The file containing the overrides will be loaded from the given url.
     * Blank lines and lines starting with a '#' will be ignored, all other lines
     * are considered as urls to override bundles.</p>
     *
     * <p>The list of resources to resolve will be scanned and for each bundle,
     * if a bundle override matches that resource, it will be used instead.</p>
     *
     * <p>Matching is done on bundle symbolic name (they have to be the same)
     * and version (the bundle override version needs to be greater than the
     * resource to be resolved, and less than the next minor version.  A range
     * directive can be added to the override url in which case, the matching
     * will succeed if the resource to be resolved is within the given range.</p>
     *
     * @param resources the list of resources to resolve
     * @param overrides list of bundle overrides
     * @param <T> the resource type.
     *
     * @deprecated Use {@link #override(Map, Map)}
     */
    @Deprecated
    public static <T extends Resource> void override(Map<String, T> resources, Collection<String> overrides) {
        // Do override replacement
        for (Clause override : Parser.parseClauses(overrides.toArray(new String[overrides.size()]))) {
            String overrideRange = override.getAttribute(OVERRIDE_RANGE);
            T over = resources.get(override.getName());
            if (over == null) {
                // Ignore invalid overrides
                continue;
            }
            for (String uri : new ArrayList<>(resources.keySet())) {
                Resource res = resources.get(uri);
                if (shouldOverride(res, over, overrideRange)) {
                    resources.put(uri, over);
                }
            }
        }
    }

    /**
     * <p>Input map of resources is checked - if there are matching resources in <code>overridenFrom</code> and
     * there's <strong>no</strong> symbolic name matching, resource for original URI is restored.
     * Effectively this method reverts {@link org.apache.karaf.features.internal.model.processing.BundleReplacements.BundleOverrideMode#MAVEN maven}
     * override mode if there's no symbolic name matching.</p>
     *
     * <p>About versions - with previous <code>${karaf.etc}/overrides.properties</code> both symbolic name
     * should match <strong>and</strong> versions should be compatible - either using implicit rules or by means
     * of <code>range</code> clause. With new mechanism, we know we should use OSGi or Maven override, but we
     * loose information about OSGi version range matching - we assume then that version rules were applied at
     * features JAXB model processing time.</p>
     *
     * @param resources
     * @param overridenFrom
     * @param <T>
     */
    public static <T extends Resource> void override(Map<String, T> resources, Map<String, T> overridenFrom) {
        for (Map.Entry<String, T> original : overridenFrom.entrySet()) {
            T replacement = resources.get(original.getKey());
            if (replacement == null) {
                continue;
            }
            if (!shouldOverride(original.getValue(), replacement, "[0,*)")) {
                // bring back original version
                resources.put(original.getKey(), original.getValue());
            }
        }
    }

    /**
     * @param resource resource to be overriden
     * @param explicitRange range set on the override clause
     * @return if the resource should be overriden by the given override
     */
    private static <T extends Resource> boolean shouldOverride(Resource resource, T override, String explicitRange) {
        if (!getSymbolicName(resource).equals(getSymbolicName(override))) {
            return false;
        }
        VersionRange range;
        if (explicitRange == null) {
            // default to micro version compatibility
            Version v1 = getVersion(resource);
            Version v2 = new Version(v1.getMajor(), v1.getMinor() + 1, 0);
            range = new VersionRange(false, v1, v2, true);
        } else {
            range = VersionRange.parseVersionRange(explicitRange);
        }
        return range.contains(getVersion(override)) && getVersion(resource).compareTo(getVersion(override)) < 0;
    }

    public static Set<String> loadOverrides(String overridesUrl) {
        Set<String> overrides = new LinkedHashSet<>();
        try {
            if (overridesUrl != null) {
                try (
                        InputStream is = new URL(overridesUrl).openStream()
                ) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            overrides.add(line);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.debug("Unable to load overrides bundles list", e.toString());
        } catch (Exception e) {
            LOGGER.debug("Unable to load overrides bundles list", e);
        }
        return overrides;
    }

    public static String extractUrl(String override) {
        Clause[] cs = Parser.parseClauses(new String[]{override});
        if (cs.length != 1) {
            throw new IllegalStateException("Override contains more than one clause: " + override);
        }
        return cs[0].getName();
    }

}
