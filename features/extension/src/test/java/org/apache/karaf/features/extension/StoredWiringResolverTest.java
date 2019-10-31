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
package org.apache.karaf.features.extension;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.*;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

public class StoredWiringResolverTest {
    private static final String TEST_RESOURCES_WIRINGS = "target/test-classes/wirings/";

    private static final int EXPECTED_STOCK_WIRINGS = 3;
    private static final String PACKAGE_FILTER = "(&(osgi.wiring.package=org.osgi.framework)(version>=1.6.0)(!(version>=2.0.0)))";

    private IMocksControl c;

    private StoredWiringResolver wiringResolver;

    @Before
    public void init() {
        c = EasyMock.createControl();

        wiringResolver = new StoredWiringResolver(new File(TEST_RESOURCES_WIRINGS).toPath());
        wiringResolver.load();
    }

    @After
    public void cleanup() {
        new File(TEST_RESOURCES_WIRINGS + "25").delete();
    }

    @Test
    public void load() {
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());

        Assert.assertTrue(wiringResolver.wiring.containsKey(1L));
        Assert.assertTrue(wiringResolver.wiring.containsKey(5L));
        Assert.assertTrue(wiringResolver.wiring.containsKey(9L));
    }

    @Test
    public void filterMatches() {
        List<BundleCapability> candidates = new ArrayList<>();

        long sourceBundleId = 1L;
        long targetBundleId = 2L;
        String targetBundleVersion = "1.0.1";

        BundleCapability candidate1 = mockBundleCapability(targetBundleId, targetBundleVersion);
        candidates.add(candidate1);

        BundleCapability candidate2 = mockBundleCapability(targetBundleId, "2.1.0");
        candidates.add(candidate2);

        BundleRequirement req = packageRequirement(sourceBundleId, false);

        c.replay();

        wiringResolver.filterMatches(req, candidates);

        c.verify();

        assertEquals(1, candidates.size());
        assertEquals(candidate1, candidates.iterator().next());
    }

    @Test
    public void filterMatchesFragmentBundle() {
        List<BundleCapability> candidates = new ArrayList<>();

        long sourceBundleId = 5L;
        long targetBundleId = 2L;
        String targetBundleVersion = "1.0.1";

        BundleCapability candidate1 = mockBundleCapability(targetBundleId, targetBundleVersion);
        candidates.add(candidate1);

        BundleCapability candidate2 = mockBundleCapability(targetBundleId, "2.1.0");
        candidates.add(candidate2);

        BundleRequirement req = packageRequirement(sourceBundleId, true);

        c.replay();

        wiringResolver.filterMatches(req, candidates);

        c.verify();

        assertEquals(1, candidates.size());
        assertEquals(candidate1, candidates.iterator().next());
    }

    @Test
    public void updateNew() {
        long newBundleId = 25L;
        File file = new File(TEST_RESOURCES_WIRINGS + newBundleId);

        Bundle bundle = wiredMockBundle(newBundleId, Collections.emptyList() );

        c.replay();

        // preconditions
        Assert.assertFalse(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());
        Assert.assertFalse(wiringResolver.wiring.containsKey(newBundleId));

        wiringResolver.update(bundle);

        c.verify();

        // assertions
        Assert.assertTrue(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS + 1, wiringResolver.wiring.size());
        Assert.assertTrue(wiringResolver.wiring.containsKey(newBundleId));
    }

    @Test
    public void updateExisting() {
        long newBundleId = 9L;
        File file = new File(TEST_RESOURCES_WIRINGS + newBundleId);

        BundleWire wire = mockBundleWire(PackageNamespace.PACKAGE_NAMESPACE, PACKAGE_FILTER,
                mockBundleCapability(25L, "1.7.8"));
        Bundle bundle = wiredMockBundle(newBundleId, Collections.singletonList(wire));

        c.replay();

        // preconditions
        Assert.assertTrue(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());
        Assert.assertTrue(wiringResolver.wiring.containsKey(newBundleId));

        wiringResolver.update(bundle);

        c.verify();

        // assertions
        Assert.assertTrue(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());
        Assert.assertTrue(wiringResolver.wiring.containsKey(newBundleId));
    }

    @Test
    public void delete() {
        long newBundleId = 25L;
        File file = new File(TEST_RESOURCES_WIRINGS + newBundleId);

        Bundle bundle = wiredMockBundle(newBundleId, Collections.emptyList() );

        c.replay();

        wiringResolver.update(bundle);

        // preconditions
        Assert.assertTrue(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS + 1, wiringResolver.wiring.size());
        Assert.assertTrue(wiringResolver.wiring.containsKey(newBundleId));

        wiringResolver.delete(bundle);

        c.verify();

        // assertions
        Assert.assertFalse(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());
        Assert.assertFalse(wiringResolver.wiring.containsKey(newBundleId));
    }

    @Test
    public void deleteNonExisting() {
        long otherBundleId = 30L;
        File file = new File(TEST_RESOURCES_WIRINGS + otherBundleId);

        Bundle bundle = mockBundle(otherBundleId);

        c.replay();

        // preconditions
        Assert.assertFalse(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());
        Assert.assertFalse(wiringResolver.wiring.containsKey(otherBundleId));

        wiringResolver.delete(bundle);

        c.verify();

        // assertions
        Assert.assertFalse(file.exists());
        Assert.assertEquals(EXPECTED_STOCK_WIRINGS, wiringResolver.wiring.size());
        Assert.assertFalse(wiringResolver.wiring.containsKey(otherBundleId));
    }

    private Bundle mockBundle(long bundleId) {
        Bundle bundle = c.createMock(Bundle.class);
        expect(bundle.getBundleId()).andReturn(bundleId).atLeastOnce();

        return bundle;
    }

    private Bundle wiredMockBundle(long bundleId, List<BundleWire> wires) {
        Bundle bundle = mockBundle(bundleId);

        BundleWiring wiring = c.createMock(BundleWiring.class);
        expect(wiring.getRequiredWires(null)).andReturn(wires);
        expect(bundle.adapt(BundleWiring.class)).andReturn(wiring);

        return bundle;
    }

    private BundleWire mockBundleWire(String packageNamespace, String packageFilter, BundleCapability capability) {
        BundleWire wire = c.createMock(BundleWire.class);

        BundleRequirement req = c.createMock(BundleRequirement.class);
        expect(req.getNamespace()).andReturn(packageNamespace);

        Map<String, String> directives = new HashMap<>();
        directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, packageFilter);
        expect(req.getDirectives()).andReturn(directives);

        expect(wire.getRequirement()).andReturn(req);
        expect(wire.getCapability()).andReturn(capability);

        return wire;
    }

    private BundleRequirement packageRequirement(long sourceBundleId, boolean isFragment) {
        Bundle bundle = c.createMock(Bundle.class);
        expect(bundle.getBundleId()).andReturn(sourceBundleId).atLeastOnce();

        BundleRevision rev = c.createMock(BundleRevision.class);
        expect(rev.getBundle()).andReturn(bundle);

        List<Capability> bundleCapabilities = new ArrayList<>();

        if(isFragment) {
            BundleCapability cap = c.createMock(BundleCapability.class);

            expect(cap.getNamespace()).andReturn(IdentityNamespace.IDENTITY_NAMESPACE);

            Map<String, Object> attrs = new HashMap<>();
            attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_FRAGMENT);
            expect(cap.getAttributes()).andReturn(attrs);

            bundleCapabilities.add(cap);
        }

        expect(rev.getCapabilities(null)).andReturn(bundleCapabilities);

        BundleRequirement req = c.createMock(BundleRequirement.class);

        Map<String, String> directives = new HashMap<>();
        directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, PACKAGE_FILTER);
        expect(req.getDirectives()).andReturn(directives);

        expect(req.getNamespace()).andReturn(PackageNamespace.PACKAGE_NAMESPACE).atLeastOnce();

        expect(req.getRevision()).andReturn(rev).atLeastOnce();

        return req;
    }

    private BundleCapability mockBundleCapability(long bundleId, String version) {
        Bundle bundle = mockBundle(bundleId);

        BundleRevision rev = c.createMock(BundleRevision.class);
        expect(rev.getBundle()).andReturn(bundle);

        BundleCapability cap = c.createMock(BundleCapability.class);
        expect(cap.getRevision()).andReturn(rev);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put( Constants.VERSION_ATTRIBUTE, version );
        expect( cap.getAttributes() ).andReturn( attrs );

        return cap;
    }
}