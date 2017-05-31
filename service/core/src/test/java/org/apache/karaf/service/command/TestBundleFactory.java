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
package org.apache.karaf.service.command;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class TestBundleFactory {
    ServiceReference<?> createServiceRef(Object ... keyProp) {
        ServiceReference<?> serviceRef = createMock(ServiceReference.class);
        if (keyProp.length % 2 != 0) {
            throw new IllegalArgumentException("");
        }
        Hashtable<String, Object> keyPropMap = new Hashtable<>();
        int c = 0;
        while (c < keyProp.length) {
            String key = (String)keyProp[c++];
            Object value = keyProp[c++];
            keyPropMap.put(key, value);
            expect(serviceRef.getProperty(key)).andReturn(value).anyTimes();
        }
        expect(serviceRef.getPropertyKeys()).andReturn(Collections.list(keyPropMap.keys()).toArray(new String[]{})).anyTimes();
        return serviceRef;
    }
    
    Bundle createBundle(long id, String name) {
        Bundle bundle = createMock(Bundle.class);
        expect(bundle.getBundleId()).andReturn(id).anyTimes();
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put(Constants.BUNDLE_NAME, name);
        expect(bundle.getHeaders()).andReturn(headers).anyTimes();
        return bundle;
    }
    
    private Bundle[] createBundles() {
        Bundle bundle1 = createBundle(1, "Bundle A");
        Bundle bundle2 = createBundle(2, "Bundle B");
        Bundle bundle3 = createBundle(3, "Bundle C");

        ServiceReference<?> ref1 = createServiceRef(Constants.OBJECTCLASS, new String[]{"org.example.MyService"},
            "key1", "value1");
        ServiceReference<?> ref2 = createServiceRef(Constants.OBJECTCLASS, new String[]{"org.example.OtherService"}, "key2", 1);
        // No ObjectClass which can happen for gogo commands
        ServiceReference<?> ref3 = createServiceRef(Constants.OBJECTCLASS, new String[]{}, "osgi.command.scope", "task");

        addRegisteredServices(bundle1, ref1, ref2);
        addRegisteredServices(bundle2, ref2, ref3);
        expect(bundle3.getRegisteredServices()).andReturn(null).anyTimes();

        expect(bundle1.getServicesInUse()).andReturn(null).anyTimes();
        addUsedServices(bundle2, ref1);
        addUsedServices(bundle3, ref1, ref2);
        
        expect(ref1.getUsingBundles()).andReturn(new Bundle[]{bundle2, bundle3}).anyTimes();
        expect(ref2.getUsingBundles()).andReturn(new Bundle[]{bundle3}).anyTimes();
        expect(ref3.getUsingBundles()).andReturn(new Bundle[]{bundle3}).anyTimes();

        replay(bundle1, bundle2, bundle3, ref1, ref2, ref3);
        return new Bundle[] { bundle1, bundle2, bundle3 };
    }
    
    private void addUsedServices(Bundle bundle, ServiceReference<?> ... refs) {
        expect(bundle.getServicesInUse()).andReturn(refs).anyTimes();
    }
    
    private void addRegisteredServices(Bundle bundle, ServiceReference<?> ... refs) {
        expect(bundle.getRegisteredServices()).andReturn(refs).anyTimes();
        for (ServiceReference<?> ref : refs) {
            expect(ref.getBundle()).andReturn(bundle);
        }
    }

    public BundleContext createBundleContext() {
        BundleContext bundleContext = createMock(BundleContext.class);
        Bundle[] bundles = createBundles();
        expect(bundleContext.getBundles()).andReturn(bundles).anyTimes();
        expect(bundleContext.getBundle(0)).andReturn(null).anyTimes();
        expect(bundleContext.getBundle(1)).andReturn(bundles[0]).anyTimes();
        expect(bundleContext.getBundle(2)).andReturn(bundles[1]).anyTimes();
        replay(bundleContext);
        return bundleContext;
    }
}
