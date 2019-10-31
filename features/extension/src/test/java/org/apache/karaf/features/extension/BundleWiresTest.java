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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Namespace;

public class BundleWiresTest {

    private static final Path BASE_PATH = new File("target/bundles").toPath();
    private static final String targetBundleVersion = "1.0.1";
    private static final int targetBundleId = 2;
    private static final String packageFilter = "(&(osgi.wiring.package=org.osgi.framework)(version>=1.6.0)(!(version>=2.0.0)))";
    private IMocksControl c;

    @Before
    public void init() {
        c = EasyMock.createControl();
    }

    @Test
    public void testFromBundle() throws IOException {
        BundleWire wire = packageWire(packageFilter, bundleCap(targetBundleId, targetBundleVersion, true ));
        Bundle bundle = wiredBundle(Arrays.asList(wire));
        c.replay();
        BundleWires bwires = new BundleWires(bundle);
        bwires.save(BASE_PATH);
        c.verify();
        Iterator<String> lines = Files.lines(new File("target/bundles/1").toPath()).iterator();
        Assert.assertEquals(PackageNamespace.PACKAGE_NAMESPACE + "; " + packageFilter, lines.next());
        Assert.assertEquals(targetBundleId + "; version=" + targetBundleVersion, lines.next());
        bwires.delete(BASE_PATH);
    }
    
    @Test
    public void testFromFile() throws IOException {
        BundleWires wires = readFromFile();
        assertEquals(1, wires.wiring.size());
        Entry<String, Set<String>> wire = wires.wiring.entrySet().iterator().next();
        assertEquals(PackageNamespace.PACKAGE_NAMESPACE + "; " + packageFilter, wire.getKey());
        assertEquals(targetBundleId + "; version=" + targetBundleVersion, wire.getValue().iterator().next());
    }
    
    @Test
    public void testFilterCandidates() throws IOException {
        BundleWires wires = readFromFile();
        BundleRequirement req = packageRequirement(packageFilter);
        BundleCapability candidate1 = bundleCap(targetBundleId, targetBundleVersion, true );
        List<BundleCapability> candidates = new ArrayList<>();
        candidates.add(candidate1);
        BundleCapability matchingCandidate = bundleCap(targetBundleId, "1.1.0", true );
        candidates.add(matchingCandidate);
        c.replay();

        Set<BundleCapability> goodCandidates = wires.filterCandidates( req, candidates );
        assertEquals(1, goodCandidates.size());
        assertEquals(candidate1, goodCandidates.iterator().next());
        c.verify();
    }

    @Test
    public void testFragmentWithThreeHosts() throws IOException {
        String hostFilter = "(&(osgi.wiring.host=our-host)(bundle-version>=0.0.0))";

        long host1BundleId = 2L;
        String host1Version = "1.0.0";
        long host2BundleId = 3L;
        String host2Version = "1.5.0";
        long host3BundleId = 4L;
        String host3Version = "2.0.0";

        BundleWire host1 = hostWire(hostFilter, bundleCap(host1BundleId, host1Version, true ));
        BundleWire host2 = hostWire(hostFilter, bundleCap(host2BundleId, host2Version, true ));
        BundleWire host3 = hostWire(hostFilter, bundleCap(host3BundleId, host3Version, true ));

        Bundle bundle = wiredBundle(Arrays.asList(host1, host2, host3));

        c.replay();

        BundleWires bwires = new BundleWires(bundle);
        bwires.save(BASE_PATH);

        c.verify();

        List<Long> hosts = LongStream.of( bwires.getFragmentHosts() ).boxed().collect( Collectors.toList());

        Assert.assertTrue(hosts.contains( host1BundleId ));
        Assert.assertTrue(hosts.contains( host2BundleId ));
        Assert.assertTrue(hosts.contains( host3BundleId ));

        Iterator<String> lines = Files.lines(new File("target/bundles/1").toPath()).iterator();

        // the save order isn't guaranteed (a Set is used internally), so we need to collect the info first
        Set<String> wirings = new HashSet<>();

        Assert.assertEquals(HostNamespace.HOST_NAMESPACE + "; " + hostFilter, lines.next());
        wirings.add( lines.next() );

        Assert.assertEquals(HostNamespace.HOST_NAMESPACE + "; " + hostFilter, lines.next());
        wirings.add( lines.next() );

        Assert.assertEquals(HostNamespace.HOST_NAMESPACE + "; " + hostFilter, lines.next());
        wirings.add( lines.next() );

        Assert.assertTrue(wirings.contains( host1BundleId + "; version=" + host1Version ));
        Assert.assertTrue(wirings.contains( host2BundleId + "; version=" + host2Version ));
        Assert.assertTrue(wirings.contains( host3BundleId + "; version=" + host3Version ));

        bwires.delete(BASE_PATH);
    }

    @Test
    public void testFilterCandidatesSelfSatisfiedCapability() throws IOException {
        // no wires are created with itself
        Bundle bundle = wiredBundle( Collections.emptyList() );

        BundleCapability candidate1 = bundleCap(1, targetBundleVersion, false );
        List<BundleCapability> candidates = new ArrayList<>();
        candidates.add(candidate1);

        BundleCapability matchingCandidate = bundleCap(2, "1.1.0", false );
        candidates.add(matchingCandidate);

        BundleRequirement req = packageRequirement(packageFilter);

        c.replay();

        BundleWires wires = new BundleWires(bundle);

        Set<BundleCapability> goodCandidates = wires.filterCandidates( req, candidates );
        assertEquals(1, goodCandidates.size());
        assertEquals(candidate1, goodCandidates.iterator().next());

        c.verify();
    }

    private BundleWires readFromFile() throws IOException {
        File wiringsFile = new File("src/test/resources/wirings/1");
        BufferedReader reader = new BufferedReader(new FileReader(wiringsFile)); 
        BundleWires wires = new BundleWires(1, reader);
        return wires;
    }

    private BundleWire packageWire(String packageFilter, BundleCapability bundleRef) {
        BundleWire wire = c.createMock(BundleWire.class);
        BundleRequirement req = packageRequirement(packageFilter);
        expect(wire.getRequirement()).andReturn(req);
        expect(wire.getCapability()).andReturn(bundleRef);
        return wire;
    }

    private BundleRequirement packageRequirement(String packageFilter) {
        BundleRequirement req = c.createMock(BundleRequirement.class);
        Map<String, String> directives = new HashMap<>();
        directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, packageFilter);
        expect(req.getDirectives()).andReturn(directives);
        expect(req.getNamespace()).andReturn(PackageNamespace.PACKAGE_NAMESPACE);
        return req;
    }

    private BundleWire hostWire(String hostFilter, BundleCapability bundleRef) {
        BundleWire wire = c.createMock(BundleWire.class);
        BundleRequirement req = hostRequirement(hostFilter);
        expect(wire.getRequirement()).andReturn(req);
        expect(wire.getCapability()).andReturn(bundleRef);
        return wire;
    }

    private BundleRequirement hostRequirement(String packageFilter) {
        BundleRequirement req = c.createMock(BundleRequirement.class);
        Map<String, String> directives = new HashMap<>();
        directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, packageFilter);
        expect(req.getDirectives()).andReturn(directives);
        expect(req.getNamespace()).andReturn(HostNamespace.HOST_NAMESPACE);
        return req;
    }

    private BundleCapability bundleCap( long bundleId, String version, boolean expectGetAttributes ) {
        BundleRevision rev = c.createMock(BundleRevision.class);
        Bundle bundle = c.createMock(Bundle.class);
        expect(bundle.getBundleId()).andReturn(bundleId);
        expect(rev.getBundle()).andReturn(bundle);
        BundleCapability cap = c.createMock(BundleCapability.class);
        expect(cap.getRevision()).andReturn(rev);
        if(expectGetAttributes) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put( Constants.VERSION_ATTRIBUTE, version );
            expect( cap.getAttributes() ).andReturn( attrs );
        }
        return cap;
    }

    private Bundle wiredBundle(List<BundleWire> wires) {
        Bundle bundle = c.createMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(1l);
        BundleWiring wiring = c.createMock(BundleWiring.class);
        expect(wiring.getRequiredWires(null)).andReturn(wires);
        expect(bundle.adapt(BundleWiring.class)).andReturn(wiring);
        return bundle;
    }

}
