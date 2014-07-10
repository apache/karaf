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
package org.apache.karaf.management.mbeans.bundles.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bundles selector which is able to get a bundle by ID, ID range, name, and version.
 */
public class BundlesSelector {

    private BundleContext bundleContext;

    public BundlesSelector(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Select bundles with ID, ID range, name, and version.
     *
     * @param bundleId the bundles ID for selection.
     * @return the corresponding bundles found.
     * @throws Exception in case of selection
     */
    public List<Bundle> selectBundles(String bundleId) throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        if (bundleId != null && bundleId.length() > 0) {

            // bundle ID is a number
            Pattern pattern = Pattern.compile("^\\d+$");
            Matcher matcher = pattern.matcher(bundleId);
            if (matcher.matches()) {
                Bundle bundle = this.getBundleById(bundleId);
                bundles.add(bundle);
                return bundles;
            }

            // bundle is an ID range
            pattern = Pattern.compile("^(\\d+)-(\\d+)$");
            matcher = pattern.matcher(bundleId);
            if (matcher.matches()) {
                int index = bundleId.indexOf('-');
                long startId = Long.parseLong(bundleId.substring(0, index));
                long stopId = Long.parseLong(bundleId.substring(index + 1));
                if (startId < stopId) {
                    for (long i = startId; i <= stopId; i++) {
                        Bundle bundle = bundleContext.getBundle(i);
                        bundles.add(bundle);
                    }
                }
                return bundles;
            }

            // bundle ID is name/version
            int index = bundleId.indexOf('/');
            if (index != -1) {
                // user has provided name and version
                return this.getBundleByNameAndVersion(bundleId.substring(0, index), bundleId.substring(index + 1));
            } else {
                // user has provided only name
                return this.getBundleByName(bundleId);
            }


        }
        return bundles;
    }

    /**
     * Get a bundle with the bundle ID.
     *
     * @param id the bundle ID.
     * @return the corresponding bundle.
     */
    private Bundle getBundleById(String id) {
        Bundle bundle = null;
        try {
            long idNumber = Long.parseLong(id);
            bundle = bundleContext.getBundle(idNumber);
        } catch (Exception e) {
            // ignore
        }
        return bundle;
    }

    /**
     * Get a bundles list with the name or symbolic name matching the pattern.
     *
     * @param name the bundle name or symbolic name pattern to match.
     * @return the bundles list.
     */
    private List<Bundle> getBundleByName(String name) {
        return getBundleByNameAndVersion(name, null);
    }

    /**
     * Get a bundles list with the name or symbolic name matching the name pattern and version matching the version pattern.
     *
     * @param name    the bundle name or symbolic name regex to match.
     * @param version the bundle version regex to match.
     * @return the bundles list.
     */
    private List<Bundle> getBundleByNameAndVersion(String name, String version) {
        Bundle[] bundles = bundleContext.getBundles();

        ArrayList<Bundle> result = new ArrayList<Bundle>();

        Pattern namePattern = Pattern.compile(name);

        for (int i = 0; i < bundles.length; i++) {

            String bundleSymbolicName = bundles[i].getSymbolicName();
            // skip bundles without Bundle-SymbolicName header
            if (bundleSymbolicName == null) {
                continue;
            }

            Matcher symbolicNameMatcher = namePattern.matcher(bundleSymbolicName);

            Matcher nameMatcher = null;
            String bundleName = (String) bundles[i].getHeaders().get(Constants.BUNDLE_NAME);
            if (bundleName != null) {
                nameMatcher = namePattern.matcher(bundleName);
            }

            if (version != null) {
                String bundleVersion = (String) bundles[i].getHeaders().get(Constants.BUNDLE_VERSION);
                if (bundleVersion != null) {
                    boolean nameMatch = (nameMatcher != null && nameMatcher.matches()) || symbolicNameMatcher.matches();
                    if (nameMatch) {
                        Pattern versionPattern = Pattern.compile(version);
                        Matcher versionMatcher = versionPattern.matcher(bundleVersion);
                        if (versionMatcher.matches()) {
                            result.add(bundles[i]);
                        }
                    }
                }
            } else {
                boolean nameMatch = (nameMatcher != null && nameMatcher.matches()) || symbolicNameMatcher.matches();
                if (nameMatch) {
                    result.add(bundles[i]);
                }
            }
        }
        return result;
    }

}
