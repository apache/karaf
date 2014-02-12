
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.features.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.internal.model.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to deal with overriden bundles at feature installation time.
 \*/
public class Overrides {

    private static final Logger LOGGER = LoggerFactory.getLogger(Overrides.class);

    private static final String OVERRIDE_RANGE = "range";
    private static final String VENDOR_WARNING = "Malicious code possibly introduced by patch override, see log for details";

    /**
     * Compute a list of bundles to install, taking into account overrides.
     *
     * The file containing the overrides will be loaded from the given url.
     * Blank lines and lines starting with a '#' will be ignored, all other lines
     * are considered as urls to override bundles.
     *
     * The list of bundles to install will be scanned and for each bundle,
     * if a bundle override matches that bundle, it will be used instead.
     *
     * Matching is done on bundle symbolic name (they have to be the same)
     * and version (the bundle override version needs to be greater than the
     * bundle to be installed, and less than the next minor version.  A range
     * directive can be added to the override url in which case, the matching
     * will succeed if the bundle to be installed is within the given range.
     *
     * @param infos the list of bundles to install
     * @param overridesUrl url pointing to the file containing the list of override bundles
     * @return a new list of bundles to install
     */
    public static List<BundleInfo> override(List<BundleInfo> infos, String overridesUrl) {
        List<Clause> overrides = loadOverrides(overridesUrl);
        if (overrides.isEmpty()) {
            return infos;
        }
        try {
            Map<String, Manifest> manifests = new HashMap<String, Manifest>();
            for (Clause override : overrides) {
                Manifest manifest = getManifest(override.getName());
                manifests.put(override.getName(), manifest);
            }
            List<BundleInfo> newInfos = new ArrayList<BundleInfo>();
            for (BundleInfo info : infos) {
                Manifest manifest = getManifest(info.getLocation());
                if (manifest != null) {
                    String bsn = getBundleSymbolicName(manifest);
                    Version ver = getBundleVersion(manifest);
                    String ven = getBundleVendor(manifest);
                    String url = info.getLocation();
                    for (Clause override : overrides) {
                        Manifest overMan = manifests.get(override.getName());
                        if (overMan == null) {
                            continue;
                        }
                        String oBsn = getBundleSymbolicName(overMan);
                        if (!bsn.equals(oBsn)) {
                            continue;
                        }

                        Version oVer = getBundleVersion(overMan);
                        VersionRange range;
                        String vr = extractVersionRange(override);
                        if (vr == null) {
                            // default to micro version compatibility
                            Version v2 = new Version(oVer.getMajor(), oVer.getMinor(), 0);
                            if (v2.equals(oVer)) {
                                continue;
                            }
                            range = new VersionRange(false, v2, oVer, true);
                        } else {
                            range = VersionRange.parseVersionRange(vr);
                        }

                        String vendor = getBundleVendor(overMan);

                        // Before we do a replace, lets check if vendors change
                        if (ven == null) {
                             if (vendor != null) {
                                 LOGGER.warn(VENDOR_WARNING);
                             }
                        } else {
                             if (vendor == null) {
                                 LOGGER.warn(VENDOR_WARNING);
                             } else {
                                  if (!vendor.equals(ven)) {
                                      LOGGER.warn(VENDOR_WARNING);
                                  } 
                             }
                        }
                        // The resource matches, so replace it with the overridden resource
                        // if the override is actually a newer version than what we currently have
                        if (range.contains(ver) && ver.compareTo(oVer) < 0) {
                            LOGGER.warn("Overriding original bundle " + url + " to " + override.getName());
                            ver = oVer;
                            url = override.getName();
                        }
                    }
                    if (!info.getLocation().equals(url)) {
                        Bundle b = new Bundle();
                        b.setLocation(url);
                        b.setStartLevel(info.getStartLevel());
                        b.setStart(info.isStart());
                        b.setDependency(info.isDependency());
                        newInfos.add(b);
                    } else {
                        newInfos.add(info);
                    }
                } else {
                    newInfos.add(info);
                }
            }
            return newInfos;
        } catch (Exception e) {
            LOGGER.info("Unable to process bundle overrides", e);
            return infos;
        }
    }

    public static List<Clause> loadOverrides(String overridesUrl) {
        List<Clause> overrides = new ArrayList<Clause>();
        try {
            if (overridesUrl != null) {
                InputStream is = new URL(overridesUrl).openStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            Clause[] cs = Parser.parseHeader(line);
                            for (Clause c : cs) {
                                overrides.add(c);
                            }
                        }
                    }
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to load overrides bundles list", e);
        }
        return overrides;
    }

    private static Version getBundleVersion(Manifest manifest) {
        String ver = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        return VersionTable.getVersion(ver);
    }

    private static String getBundleSymbolicName(Manifest manifest) {
        String bsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        bsn = stripSymbolicName(bsn);
        return bsn;
    }

    private static String getBundleVendor(Manifest manifest) {
        String ven = manifest.getMainAttributes().getValue(Constants.BUNDLE_VENDOR);
        return ven;
    }

    private static Manifest getManifest(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry = null;
            while ( (entry = zis.getNextEntry()) != null ) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    return new Manifest(zis);
                }
            }
            return null;
        } finally {
            is.close();
        }
    }

    private static String extractVersionRange(Clause override) {
        return override.getAttribute(OVERRIDE_RANGE);
    }

    private static String stripSymbolicName(String symbolicName) {
        Clause[] cs = Parser.parseHeader(symbolicName);
        if (cs == null || cs.length != 1) {
            throw new IllegalArgumentException("Bad Bundle-SymbolicName header: " + symbolicName);
        }
        return cs[0].getName();
    }
}
