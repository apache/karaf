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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

/**
 * <p>Interface to interact with OSGi framework.</p>
 * <p>Bundles are installed into {@link org.eclipse.equinox.region.Region regions} and {@link Feature features}
 * are used only to get their configs and libraries.</p>
 */
public interface BundleInstallSupport {

    void print(String message, boolean verbose);

    void refreshPackages(Collection<Bundle> bundles) throws InterruptedException;

    Bundle installBundle(String region, String uri, InputStream is) throws BundleException;

    void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException;

    void uninstall(Bundle bundle) throws BundleException;

    void startBundle(Bundle bundle) throws BundleException;

    void stopBundle(Bundle bundle, int options) throws BundleException;

    void setBundleStartLevel(Bundle bundle, int startLevel);

    void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring,
                        Map<Resource, Bundle> resToBnd);

    void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies,
                        Map<String, Set<Long>> bundles)
        throws BundleException, InvalidSyntaxException;

    void saveDigraph();

    RegionDigraph getDiGraphCopy() throws BundleException;
    
    void installConfigs(Feature feature) throws IOException, InvalidSyntaxException;
    
    void installLibraries(Feature feature) throws IOException;

    File getDataFile(String name);
    
    FrameworkInfo getInfo();

    void unregister();

    /**
     * <p>Low-level state of system, provides information about start levels (initial and current), system bundle,
     * bundle of features service and entire map of bundle IDs to {@link Bundle} instances.</p>
     * <p>There's no relation to {@link org.eclipse.equinox.region.Region regions}.</p>
     */
    class FrameworkInfo {
        public Bundle ourBundle;
        public Bundle systemBundle;
        public int initialBundleStartLevel;
        public int currentStartLevel;
        public Map<Long, Bundle> bundles = new HashMap<>();
    }

}
