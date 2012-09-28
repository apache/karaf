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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class BundleManagerTest {

    public Bundle createDummyBundle(long id, String symbolicName, Dictionary<String,String> headers) {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        expect(bundle.getBundleId()).andReturn(id).anyTimes();
        expect(bundle.getSymbolicName()).andReturn(symbolicName);
        expect(bundle.getHeaders()).andReturn(headers).anyTimes();
        replay(bundle);
        return bundle;
    }
    
    @Test
    public void testfindBundlestoRefreshWithHostToRefresh() throws Exception {
        Bundle hostBundle = createDummyBundle(12345l, "Host", new Hashtable<String, String>());
        
        Hashtable<String, String> d = new Hashtable<String, String>();
        d.put(Constants.FRAGMENT_HOST, "Host");
        Bundle fragmentBundle = createDummyBundle(54321l, "fragment", d);

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        BundleManager bundleManager = new BundleManager(bundleContext);

        // Host was already installed, fragment is new
        InstallationState state = new InstallationState();
        state.bundles.add(hostBundle);
        state.bundles.add(fragmentBundle);
        state.installed.add(fragmentBundle);
        
        replay(bundleContext);
        Set<Bundle> bundles = bundleManager.findBundlesToRefresh(state);
        EasyMock.verify(bundleContext);

        Assert.assertEquals(1, bundles.size());
        Assert.assertEquals(hostBundle, bundles.iterator().next());
    } 
}
