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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.bundle.core.BundleSelector;
import org.apache.karaf.util.ShellUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class BundleSelectorImpl implements BundleSelector {

    private final BundleContext bundleContext;
    
    public BundleSelectorImpl(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    /* (non-Javadoc)
     * @see org.apache.karaf.bundle.core.internal.BundleSelector#selectBundles(java.util.List)
     */
    @Override
    public List<Bundle> selectBundles(List<String> ids, boolean defaultAllBundles, boolean mayAccessSystemBundle) throws Exception {
        List<Bundle> bundles = new ArrayList<Bundle>();
        if (ids != null && !ids.isEmpty()) {
            for (String id : ids) {
                addMatchingBundles(id, bundles, mayAccessSystemBundle);
            }
        } else {
            Collections.addAll(bundles, bundleContext.getBundles());
        }
        return bundles;
    }
    
    public void addMatchingBundles(String id, List<Bundle> bundles, boolean mayAccessSystemBundle) throws Exception {
        // id is a number
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(id);
        
        if (matcher.find()) {
            Bundle bundle = this.getBundleById(id);
            if (mayAccessSystemBundle || !ShellUtil.isASystemBundle(bundleContext, bundle)) {
                addBundle(bundle, id, bundles);
            }
            return;
        }

        // id as a number range
        pattern = Pattern.compile("^(\\d+)-(\\d+)$");
        matcher = pattern.matcher(id);
        if (matcher.find()) {
            int index = id.indexOf('-');
            long startId = Long.parseLong(id.substring(0, index));
            long endId = Long.parseLong(id.substring(index + 1));
            if (startId < endId) {
                for (long i = startId; i <= endId; i++) {
                    Bundle bundle = bundleContext.getBundle(i);
                    addBundle(bundle, String.valueOf(i), bundles);
                }
            }
            return;
        }

        int index = id.indexOf('/');
        List<Bundle> bundlesByName = null;
        if (index != -1) {
            // user has provided name and version
            bundlesByName = getBundleByNameAndVersion(id.substring(0, index), id.substring(index + 1));
        } else {
            // user has provided only the name
            bundlesByName = getBundleByName(id);
        }
        for (Bundle bundleByName : bundlesByName) {
            addBundle(bundleByName, id, bundles);
        }
    }

    private void addBundle(Bundle bundle, String id, List<Bundle> bundles) throws Exception {
        if (bundle == null) {
            // if the bundle is null here, it's because we didn't find it
            System.err.println("Bundle " + id + " is invalid");
        } else {
            bundles.add(bundle);
        }
    }

    /**
     * Get a bundle identified by an id number.
     *
     * @param id the id number.
     * @return the bundle or null if not found.
     */
    private Bundle getBundleById(String id) {
        Bundle bundle = null;
        try {
            long idNumber = Long.parseLong(id);
            bundle = bundleContext.getBundle(idNumber);
        } catch (NumberFormatException nfe) {
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
                    boolean nameMatch = (nameMatcher != null && nameMatcher.find()) || symbolicNameMatcher.find();
                    if (nameMatch) {
                        Pattern versionPattern = Pattern.compile(version);
                        Matcher versionMatcher = versionPattern.matcher(bundleVersion);                    
                        if (versionMatcher.find()) {
                            result.add(bundles[i]);
                        }
                    }
                }
            } else {
                boolean nameMatch = (nameMatcher != null && nameMatcher.find()) || symbolicNameMatcher.find();
                if (nameMatch) {
                    result.add(bundles[i]);
                }
            }
        }
        return result;
    }

}
