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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.DeploymentEvent;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.apache.karaf.features.internal.support.TestBundle;
import org.apache.karaf.features.internal.support.TestDownloadManager;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.FeaturesService.*;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.easymock.EasyMock.anyInt;
import static org.junit.Assert.fail;

public class DeployerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployerTest.class);
    private Resolver resolver = new ResolverImpl(new Slf4jResolverLog(LOGGER));

    @Test
    public void testInstallSimpleFeature() throws Exception {
        IMocksControl c = EasyMock.createControl();
        String dataDir = "data1";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        Feature f101 = repo.getFeatures()[1];

        Deployer.DeployCallback callback = c.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);
        EasyMock.expectLastCall();
        callback.replaceDigraph(EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.installConfigs(f100);
        EasyMock.expectLastCall();
        callback.installLibraries(f100);
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.anyObject(),
                                EasyMock.anyObject(),
                                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);
        EasyMock.expectLastCall();

        Bundle bundleA = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");
        EasyMock.expect(callback.installBundle(EasyMock.eq(ROOT_REGION), EasyMock.eq("a100"), EasyMock.anyObject()))
                .andReturn(bundleA);

        c.replay();

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.partitionFeatures(Arrays.asList(f100, f101));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f100.getName() + "/" + new VersionRange(f100.getVersion(), true));

        deployer.deploy(dstate, request);

        c.verify();
    }

    @Test
    public void testUpdateSimpleFeature() throws Exception {
        IMocksControl c = EasyMock.createControl();

        final String dataDir = "data1";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        Feature f101 = repo.getFeatures()[1];

        Deployer.DeployCallback callback = c.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        final TestBundle bundleA = createTestBundle(1L, Bundle.ACTIVE, dataDir, "a100");

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);
        EasyMock.expectLastCall();

        callback.stopBundle(EasyMock.eq(bundleA), anyInt());
        EasyMock.expectLastCall().andStubAnswer(() -> {
            bundleA.state = Bundle.RESOLVED;
            return null;
        });
        callback.updateBundle(EasyMock.eq(bundleA), EasyMock.anyObject(), EasyMock.anyObject());
        EasyMock.expectLastCall().andStubAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                URL loc = getClass().getResource(dataDir + "/" + "a101" + ".mf");
                Manifest man = new Manifest(loc.openStream());
                Hashtable<String, String> headers = new Hashtable<>();
                for (Map.Entry<Object, Object> attr : man.getMainAttributes().entrySet()) {
                    headers.put(attr.getKey().toString(), attr.getValue().toString());
                }
                bundleA.update(headers);
                return null;
            }
        });
        callback.startBundle(EasyMock.eq(bundleA));
        EasyMock.expectLastCall();

        callback.replaceDigraph(EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.installConfigs(f101);
        EasyMock.expectLastCall();
        callback.installLibraries(f101);
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.eq(Collections.singleton(bundleA)),
                                EasyMock.anyObject(),
                                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);
        EasyMock.expectLastCall();
        callback.refreshPackages(EasyMock.eq(Collections.singleton(bundleA)));
        EasyMock.expectLastCall();
        callback.callListeners(FeatureEventMatcher.eq(new FeatureEvent(FeatureEvent.EventType.FeatureUninstalled, f100, FeaturesService.ROOT_REGION, false)));
        EasyMock.expectLastCall();
        callback.callListeners(FeatureEventMatcher.eq(new FeatureEvent(FeatureEvent.EventType.FeatureInstalled, f101, FeaturesService.ROOT_REGION, false)));
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);
        EasyMock.expectLastCall();

        c.replay();

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        addToMapSet(dstate.state.installedFeatures, ROOT_REGION, f100.getId());
        addToMapSet(dstate.state.managedBundles, ROOT_REGION, 1L);
        dstate.bundles = new HashMap<>();
        dstate.bundles.put(1L, bundleA);
        dstate.bundlesPerRegion = new HashMap<>();
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, 1L);
        dstate.partitionFeatures(Arrays.asList(f100, f101));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f101.getName() + "/" + new VersionRange(f101.getVersion(), true));

        deployer.deploy(dstate, request);

        c.verify();
    }

    @Test
    public void testUpdateServiceBundle() throws Exception {
        IMocksControl c = EasyMock.createControl();
        String dataDir = "data1";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f1 = repo.getFeatures()[0];

        Bundle serviceBundle = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");

        Deployer.DeployCallback callback = c.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);
        EasyMock.expectLastCall();
        callback.replaceDigraph(EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        EasyMock.expectLastCall();
        callback.installConfigs(f1);
        EasyMock.expectLastCall();
        callback.installLibraries(f1);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.anyObject(),
                                EasyMock.anyObject(),
                                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);
        EasyMock.expectLastCall();

        c.replay();

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundles.put(serviceBundle.getBundleId(), serviceBundle);
        dstate.bundlesPerRegion = new HashMap<>();
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, serviceBundle.getBundleId());
        dstate.partitionFeatures(Collections.singletonList(f1));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f1.getName());

        deployer.deploy(dstate, request);

        c.verify();
    }

    @Test
    public void testPrerequisite() throws Exception {
        IMocksControl c = EasyMock.createControl();

        String dataDir = "data2";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f1 = repo.getFeatures()[0];
        Feature f2 = repo.getFeatures()[1];

        Bundle serviceBundle1 = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");
        Bundle serviceBundle2 = createTestBundle(2, Bundle.ACTIVE, dataDir, "b100");

        Deployer.DeployCallback callback = c.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);
        EasyMock.expectLastCall();
        callback.installBundle(EasyMock.eq(ROOT_REGION), EasyMock.eq("a100"), EasyMock.anyObject());
        EasyMock.expectLastCall().andReturn(serviceBundle1);
        callback.replaceDigraph(EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.installConfigs(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.installLibraries(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);
        EasyMock.expectLastCall();

        c.replay();

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.partitionFeatures(Arrays.asList(f1, f2));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f2.getName());

        try {
            deployer.deploy(dstate, request);
            fail("Should have thrown an exception");
        } catch (Deployer.PartialDeploymentException e) {
            // ok
        }

        c.verify();

        c.reset();

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_STARTED);
        EasyMock.expectLastCall();
        callback.installBundle(EasyMock.eq(ROOT_REGION), EasyMock.eq("b100"), EasyMock.anyObject());
        EasyMock.expectLastCall().andReturn(serviceBundle2);
        callback.replaceDigraph(EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.installConfigs(f2);
        EasyMock.expectLastCall();
        callback.installLibraries(f2);
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_INSTALLED);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.BUNDLES_RESOLVED);
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(DeploymentEvent.DEPLOYMENT_FINISHED);
        EasyMock.expectLastCall();

        EasyMock.replay(callback);

        dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        addToMapSet(dstate.state.installedFeatures, ROOT_REGION, f1.getId());
        dstate.state.stateFeatures.put(ROOT_REGION, Collections.singletonMap(f1.getId(), "Started"));
        addToMapSet(dstate.state.managedBundles, ROOT_REGION, serviceBundle1.getBundleId());
        dstate.bundles = new HashMap<>();
        dstate.bundles.put(serviceBundle1.getBundleId(), serviceBundle1);
        dstate.bundlesPerRegion = new HashMap<>();
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, serviceBundle1.getBundleId());
        dstate.partitionFeatures(Arrays.asList(f1, f2));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());

        request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f2.getName());

        deployer.deploy(dstate, request);

        EasyMock.verify(callback);
    }

    @Test
    public void testPrereqOnPrereq1() throws Exception {
        doTestPrereqOnPrereq(1);
    }

    @Test
    public void testPrereqOnPrereq2() throws Exception {
        doTestPrereqOnPrereq(2);
    }

    @Test
    public void testPrereqOnPrereq3() throws Exception {
        doTestPrereqOnPrereq(3);
    }

    @Test
    public void testPrereqOnPrereq4() throws Exception {
        doTestPrereqOnPrereq(4);
    }

    private void doTestPrereqOnPrereq(int scenario) throws Exception {
        IMocksControl c = EasyMock.createControl();

        String dataDir = "data3";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());

        Map<String, Bundle> bundles = new HashMap<>();
        bundles.put("a100", createTestBundle(1, Bundle.ACTIVE, dataDir, "a100"));
        bundles.put("b100", createTestBundle(2, Bundle.ACTIVE, dataDir, "b100"));
        bundles.put("c100", createTestBundle(3, Bundle.ACTIVE, dataDir, "c100"));

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.partitionFeatures(Arrays.asList(repo.getFeatures()));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;

        MyDeployCallback callback = new MyDeployCallback(dstate, bundles);
        Deployer deployer = new Deployer(manager, resolver, callback);
        c.replay();

        for (int i = 1; i <= 4; i++) {
            request.requirements = new HashMap<>();
            addToMapSet(request.requirements, ROOT_REGION, "demo-" + scenario + "-c");
            Set<String> prereqs = new HashSet<>();
            while (true) {
                try {
                    deployer.deploy(callback.dstate, request);
                    break;
                } catch (Deployer.PartialDeploymentException e) {
                    if (!prereqs.containsAll(e.getMissing())) {
                        prereqs.addAll(e.getMissing());
                    } else {
                        throw new Exception("Deployment aborted due to loop in missing prerequisites: " + e.getMissing());
                    }
                }
            }
        }
    }

    private TestBundle createTestBundle(long bundleId, int state, String dir, String name) throws IOException, BundleException {
        URL loc = getClass().getResource(dir + "/" + name + ".mf");
        Manifest man = new Manifest(loc.openStream());
        Hashtable<String, String> headers = new Hashtable<>();
        for (Map.Entry<Object, Object> attr : man.getMainAttributes().entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        return new TestBundle(bundleId, name, state, headers);
    }

    static class FeatureEventMatcher implements IArgumentMatcher {
        final FeatureEvent expected;

        FeatureEventMatcher(FeatureEvent expected) {
            this.expected = expected;
        }

        public static FeatureEvent eq(FeatureEvent expected) {
            EasyMock.reportMatcher(new FeatureEventMatcher(expected));
            return null;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof FeatureEvent)) {
                return false;
            }
            FeatureEvent arg = (FeatureEvent) argument;
            return arg.getFeature() == expected.getFeature()
                    && arg.getType() == expected.getType()
                    && arg.isReplay() == expected.isReplay();
        }

        @Override
        public void appendTo(StringBuffer buffer) {

        }
    }

    private static class MyDeployCallback implements Deployer.DeployCallback {
        final Deployer.DeploymentState dstate;
        final Map<String, Bundle> bundles;

        public MyDeployCallback(Deployer.DeploymentState dstate, Map<String, Bundle> bundles) {
            this.dstate = dstate;
            this.bundles = bundles;
        }

        @Override
        public void print(String message, boolean verbose) {
        }

        @Override
        public void saveState(State state) {
            this.dstate.state.replace(state);
        }

        @Override
        public void persistResolveRequest(Deployer.DeploymentRequest request) throws IOException {
        }

        @Override
        public void callListeners(FeatureEvent featureEvent) {
        }

        @Override
        public void callListeners(DeploymentEvent deployEvent) {
        }

        @Override
        public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
            return bundles.get(uri);
        }

        @Override
        public void updateBundle(Bundle bundle, String uri, InputStream is) throws BundleException {

        }

        @Override
        public void uninstall(Bundle bundle) throws BundleException {

        }

        @Override
        public void startBundle(Bundle bundle) throws BundleException {

        }

        @Override
        public void stopBundle(Bundle bundle, int options) throws BundleException {

        }

        @Override
        public void setBundleStartLevel(Bundle bundle, int startLevel) {

        }

        @Override
        public void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring, Map<Resource, Bundle> resToBnd) {

        }

        @Override
        public void refreshPackages(Collection<Bundle> bundles) throws InterruptedException {

        }

        @Override
        public void replaceDigraph(Map<String, Map<String, Map<String, Set<String>>>> policies, Map<String, Set<Long>> bundles) throws BundleException, InvalidSyntaxException {

        }

        @Override
        public void installConfigs(Feature feature) throws IOException, InvalidSyntaxException {

        }

        @Override
        public void installLibraries(Feature feature) throws IOException {

        }

        @Override
        public void bundleBlacklisted(BundleInfo bundleInfo) {

        }
    }
}
