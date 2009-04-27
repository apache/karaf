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

package org.apache.servicemix.kernel.testing.support;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


public class FeatureInstaller {

    private Map<URI, FeatureRepositoryImpl> repositories = new HashMap<URI, FeatureRepositoryImpl>();
    private Map<String, Map<String, Feature>> features;
    private BundleContext bundleContext;
    
    public void addRepository(URI uri) throws Exception {
        if (!repositories.values().contains(uri)) {
            internalAddRepository(uri);
        }
    }
    
    protected FeatureRepositoryImpl internalAddRepository(URI uri) throws Exception {
        FeatureRepositoryImpl repo = new FeatureRepositoryImpl(uri);
        repositories.put(uri, repo);
        features = null;
        return repo;
    }
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    
    public void installFeature(String name, String version) throws Exception {
        Feature f = getFeature(name, version);
        if (f == null) {
            throw new Exception("No feature named '" + name 
                    + "' with version '" + version + "' available");
        }
        for (Feature dependency : f.getDependencies()) {
            installFeature(dependency.getName(), dependency.getVersion());
        }
       
        Set<Long> bundles = new HashSet<Long>();
        for (String bundleLocation : f.getBundles()) {
            Bundle b = installBundleIfNeeded(bundleLocation);
            bundles.add(b.getBundleId());
        }
        for (long id : bundles) {
            bundleContext.getBundle(id).start();
        }

        
    }
    
    protected Feature getFeature(String name, String version) throws Exception {
        Map<String, Feature> versions = getFeatures().get(name);
        if (versions == null || versions.isEmpty()) {
            return null;
        } else {
            Feature feature = versions.get(version);
            if (feature == null && FeatureImpl.DEFAULT_VERSION.equals(version)) {
                Version latest = new Version(cleanupVersion(version));
                for (String available : versions.keySet()) {
                    Version availableVersion = new Version(cleanupVersion(available));
                    if (availableVersion.compareTo(latest) > 0) {
                        feature = versions.get(available);
                        latest = availableVersion;
                    }
                }
            }
            return feature;
        }
    }
    
    protected Map<String, Map<String, Feature>> getFeatures() throws Exception {
        if (features == null) {
            //the outer map's key is feature name, the inner map's key is feature version       
            Map<String, Map<String, Feature>> map = new HashMap<String, Map<String, Feature>>();
            // Two phase load:
            // * first load dependent repositories
            for (;;) {
                boolean newRepo = false;
                for (FeatureRepositoryImpl repo : listRepositories()) {
                    for (URI uri : repo.getRepositories()) {
                        if (!repositories.keySet().contains(uri)) {
                            internalAddRepository(uri);
                            newRepo = true;
                        }
                    }
                }
                if (!newRepo) {
                    break;
                }
            }
            // * then load all features
            for (FeatureRepositoryImpl repo : repositories.values()) {
                for (Feature f : repo.getFeatures()) {
                    if (map.get(f.getName()) == null) {
                        Map<String, Feature> versionMap = new HashMap<String, Feature>();
                        versionMap.put(f.getVersion(), f);
                        map.put(f.getName(), versionMap);
                    } else {
                        map.get(f.getName()).put(f.getVersion(), f);
                    }
                }
            }
            features = map;
        }
        return features;
    }
    
    public FeatureRepositoryImpl[] listRepositories() {
        Collection<FeatureRepositoryImpl> repos = repositories.values();
        return repos.toArray(new FeatureRepositoryImpl[repos.size()]);
    }
    
    protected Bundle installBundleIfNeeded(String bundleLocation) throws IOException, BundleException {
        InputStream is = new BufferedInputStream(new URL(bundleLocation).openStream());
        try {
            is.mark(256 * 1024);
            JarInputStream jar = new JarInputStream(is);
            Manifest m = jar.getManifest();
            String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            String vStr = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            Version v = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
            for (Bundle b : bundleContext.getBundles()) {
                if (b.getSymbolicName() != null && b.getSymbolicName().equals(sn)) {
                    vStr = (String) b.getHeaders().get(Constants.BUNDLE_VERSION);
                    Version bv = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
                    if (v.equals(bv)) {
                        return b;
                    }
                }
            }
            try {
                is.reset();
            } catch (IOException e) {
                is.close();
                is = new BufferedInputStream(new URL(bundleLocation).openStream());
            }
            return getBundleContext().installBundle(bundleLocation, is);
        } finally {
            is.close();
        }
    }
    
    private BundleContext getBundleContext() {
        return this.bundleContext;
    }

    /**
     * Clean up version parameters. Other builders use more fuzzy definitions of
     * the version syntax. This method cleans up such a version to match an OSGi
     * version.
     *
     * @param version
     * @return
     */
    static Pattern fuzzyVersion  = Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
                                                   Pattern.DOTALL);
    static Pattern fuzzyModifier = Pattern.compile("(\\d+[.-])*(.*)",
                                                   Pattern.DOTALL);
    static public String cleanupVersion(String version) {
        Matcher m = fuzzyVersion.matcher(version);
        if (m.matches()) {
            StringBuffer result = new StringBuffer();
            String d1 = m.group(1);
            String d2 = m.group(3);
            String d3 = m.group(5);
            String qualifier = m.group(7);

            if (d1 != null) {
                result.append(d1);
                if (d2 != null) {
                    result.append(".");
                    result.append(d2);
                    if (d3 != null) {
                        result.append(".");
                        result.append(d3);
                        if (qualifier != null) {
                            result.append(".");
                            cleanupModifier(result, qualifier);
                        }
                    } else if (qualifier != null) {
                        result.append(".0.");
                        cleanupModifier(result, qualifier);
                    }
                } else if (qualifier != null) {
                    result.append(".0.0.");
                    cleanupModifier(result, qualifier);
                }
                return result.toString();
            }
        }
        return version;
    }

    static void cleanupModifier(StringBuffer result, String modifier) {
        Matcher m = fuzzyModifier.matcher(modifier);
        if (m.matches())
            modifier = m.group(2);

        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
                result.append(c);
        }
    }

}
