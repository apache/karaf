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
package org.apache.karaf.features.internal;

import static org.easymock.EasyMock.replay;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class BundleManagerTest extends TestBase {
    
    @Test
    public void testfindBundlestoRefreshWithHostToRefresh() throws Exception {
        Bundle hostBundle = createDummyBundle(12345l, "Host", headers());
        Bundle fragmentBundle = createDummyBundle(54321l, "fragment", headers(Constants.FRAGMENT_HOST, "Host"));

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        BundleManager bundleManager = new BundleManager(bundleContext);

        // Host was already installed, fragment is new
        Set<Bundle> existing = new HashSet<Bundle>(Arrays.asList(hostBundle, fragmentBundle));
        Set<Bundle> installed = new HashSet<Bundle>(Arrays.asList(fragmentBundle));
        
        replay(bundleContext);
        Set<Bundle> bundles = bundleManager.findBundlesWithFragmentsToRefresh(existing, installed);
        EasyMock.verify(bundleContext);

        Assert.assertEquals(1, bundles.size());
        Assert.assertEquals(hostBundle, bundles.iterator().next());
    }
    
    @Test
    public void testfindBundlestoRefreshWithOptionalPackages() throws Exception {
        Bundle exporterBundle = createDummyBundle(12345l, "exporter", headers(Constants.EXPORT_PACKAGE, "org.my.package"));
        Bundle importerBundle = createDummyBundle(54321l, "importer", headers(Constants.IMPORT_PACKAGE, "org.my.package;resolution:=optional"));

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        BundleManager bundleManager = new BundleManager(bundleContext);

        // Importer was already installed, exporter is new
        Set<Bundle> existing = new HashSet<Bundle>(Arrays.asList(importerBundle, exporterBundle));
        Set<Bundle> installed = new HashSet<Bundle>(Arrays.asList(exporterBundle));
        
        replay(bundleContext);
        Set<Bundle> bundles = bundleManager.findBundlesWithOptionalPackagesToRefresh(existing, installed);
        EasyMock.verify(bundleContext);

        Assert.assertEquals(1, bundles.size());
        Assert.assertEquals(importerBundle, bundles.iterator().next());
    }
}
