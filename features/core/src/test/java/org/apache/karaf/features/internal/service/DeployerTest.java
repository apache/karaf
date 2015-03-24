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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Slf4jResolverLog;
import org.apache.karaf.features.internal.support.TestBundle;
import org.apache.karaf.features.internal.support.TestDownloadManager;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
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

        String dataDir = "data1";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        repo.load(true);
        Feature f100 = repo.getFeatures()[0];
        Feature f101 = repo.getFeatures()[1];

        Deployer.DeployCallback callback = EasyMock.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.replaceDigraph(EasyMock.<Map<String, Map<String, Map<String, Set<String>>>>>anyObject(),
                EasyMock.<Map<String, Set<Long>>>anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.<State>anyObject());
        EasyMock.expectLastCall();
        callback.installFeature(f100);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.<Set<Bundle>>anyObject(),
                                EasyMock.<Map<Resource, List<Wire>>>anyObject(),
                                EasyMock.<Map<Resource, Bundle>>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();

        Bundle bundleA = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");
        EasyMock.expect(callback.installBundle(EasyMock.eq(ROOT_REGION), EasyMock.eq("a100"), EasyMock.<InputStream>anyObject()))
                .andReturn(bundleA);

        EasyMock.replay(callback);

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.features = new HashMap<>();
        dstate.features.put(f100.getId(), f100);
        dstate.features.put(f101.getId(), f101);
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<String, Map<String, Set<String>>>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.overrides = Collections.emptySet();
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = UPDATE_SNAPSHOTS_NONE;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f100.getName() + "/" + new VersionRange(f100.getVersion(), true));

        deployer.deploy(dstate, request);

        EasyMock.verify(callback);
    }

    @Test
    public void testUpdateSimpleFeature() throws Exception {

        final String dataDir = "data1";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        repo.load(true);
        Feature f100 = repo.getFeatures()[0];
        Feature f101 = repo.getFeatures()[1];

        Deployer.DeployCallback callback = EasyMock.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        final TestBundle bundleA = createTestBundle(1L, Bundle.ACTIVE, dataDir, "a100");

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();

        callback.stopBundle(EasyMock.eq(bundleA), anyInt());
        EasyMock.expectLastCall().andStubAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                bundleA.state = Bundle.RESOLVED;
                return null;
            }
        });
        callback.updateBundle(EasyMock.eq(bundleA), EasyMock.<String>anyObject(), EasyMock.<InputStream>anyObject());
        EasyMock.expectLastCall().andStubAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                URL loc = getClass().getResource(dataDir + "/" + "a101" + ".mf");
                Manifest man = new Manifest(loc.openStream());
                Hashtable<String, String> headers = new Hashtable<>();
                for (Map.Entry attr : man.getMainAttributes().entrySet()) {
                    headers.put(attr.getKey().toString(), attr.getValue().toString());
                }
                bundleA.update(headers);
                return null;
            }
        });
        callback.startBundle(EasyMock.eq(bundleA));
        EasyMock.expectLastCall();

        callback.replaceDigraph(EasyMock.<Map<String, Map<String, Map<String, Set<String>>>>>anyObject(),
                EasyMock.<Map<String, Set<Long>>>anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.<State>anyObject());
        EasyMock.expectLastCall();
        callback.installFeature(f101);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.eq(Collections.<Bundle>singleton(bundleA)),
                                EasyMock.<Map<Resource, List<Wire>>>anyObject(),
                                EasyMock.<Map<Resource, Bundle>>anyObject());
        EasyMock.expectLastCall();
        callback.refreshPackages(EasyMock.eq(Collections.<Bundle>singleton(bundleA)));
        EasyMock.expectLastCall();
        callback.callListeners(FeatureEventMatcher.eq(new FeatureEvent(FeatureEvent.EventType.FeatureUninstalled, f100, FeaturesService.ROOT_REGION, false)));
        EasyMock.expectLastCall();
        callback.callListeners(FeatureEventMatcher.eq(new FeatureEvent(FeatureEvent.EventType.FeatureInstalled, f101, FeaturesService.ROOT_REGION, false)));
        EasyMock.expectLastCall();

        EasyMock.replay(callback);

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        addToMapSet(dstate.state.installedFeatures, ROOT_REGION, f100.getId());
        addToMapSet(dstate.state.managedBundles, ROOT_REGION, 1L);
        dstate.bundles = new HashMap<>();
        dstate.bundles.put(1L, bundleA);
        dstate.bundlesPerRegion = new HashMap<>();
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, 1L);
        dstate.features = new HashMap<>();
        dstate.features.put(f100.getId(), f100);
        dstate.features.put(f101.getId(), f101);
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<String, Map<String, Set<String>>>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.overrides = Collections.emptySet();
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = UPDATE_SNAPSHOTS_NONE;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f101.getName() + "/" + new VersionRange(f101.getVersion(), true));

        deployer.deploy(dstate, request);

        EasyMock.verify(callback);
    }

    @Test
    public void testUpdateServiceBundle() throws Exception {

        String dataDir = "data1";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        repo.load(true);
        Feature f1 = repo.getFeatures()[0];

        Bundle serviceBundle = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");

        Deployer.DeployCallback callback = EasyMock.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.replaceDigraph(EasyMock.<Map<String, Map<String, Map<String, Set<String>>>>>anyObject(),
                EasyMock.<Map<String, Set<Long>>>anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.<State>anyObject());
        EasyMock.expectLastCall();
        callback.installFeature(f1);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.<Set<Bundle>>anyObject(),
                                EasyMock.<Map<Resource, List<Wire>>>anyObject(),
                                EasyMock.<Map<Resource, Bundle>>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();

        EasyMock.replay(callback);

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundles.put(serviceBundle.getBundleId(), serviceBundle);
        dstate.bundlesPerRegion = new HashMap<>();
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, serviceBundle.getBundleId());
        dstate.features = Collections.singletonMap(f1.getId(), f1);
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<String, Map<String, Set<String>>>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.overrides = Collections.emptySet();
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = UPDATE_SNAPSHOTS_NONE;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f1.getName());

        deployer.deploy(dstate, request);

        EasyMock.verify(callback);
    }

    @Test
    public void testPrerequisite() throws Exception {

        String dataDir = "data2";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        repo.load(true);
        Feature f1 = repo.getFeatures()[0];
        Feature f2 = repo.getFeatures()[1];

        Bundle serviceBundle1 = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");
        Bundle serviceBundle2 = createTestBundle(2, Bundle.ACTIVE, dataDir, "b100");

        Deployer.DeployCallback callback = EasyMock.createMock(Deployer.DeployCallback.class);
        Deployer deployer = new Deployer(manager, resolver, callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.installBundle(EasyMock.eq(ROOT_REGION), EasyMock.eq("a100"), EasyMock.<InputStream>anyObject());
        EasyMock.expectLastCall().andReturn(serviceBundle1);
        callback.replaceDigraph(EasyMock.<Map<String, Map<String, Map<String, Set<String>>>>>anyObject(),
                EasyMock.<Map<String, Set<Long>>>anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.<State>anyObject());
        EasyMock.expectLastCall();
        callback.installFeature(f1);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.<Set<Bundle>>anyObject(),
                                EasyMock.<Map<Resource, List<Wire>>>anyObject(),
                                EasyMock.<Map<Resource, Bundle>>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
        EasyMock.expectLastCall();

        EasyMock.replay(callback);

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.features = new HashMap<>();
        dstate.features.put(f1.getId(), f1);
        dstate.features.put(f2.getId(), f2);
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<String, Map<String, Set<String>>>());

        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.overrides = Collections.emptySet();
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = UPDATE_SNAPSHOTS_NONE;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f2.getName());

        try {
            deployer.deploy(dstate, request);
            fail("Should have thrown an exception");
        } catch (Deployer.PartialDeploymentException e) {
            // ok
        }

        EasyMock.verify(callback);

        EasyMock.reset(callback);

        callback.print(EasyMock.anyString(), EasyMock.anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        callback.installBundle(EasyMock.eq(ROOT_REGION), EasyMock.eq("b100"), EasyMock.<InputStream>anyObject());
        EasyMock.expectLastCall().andReturn(serviceBundle2);
        callback.replaceDigraph(EasyMock.<Map<String, Map<String, Map<String, Set<String>>>>>anyObject(),
                EasyMock.<Map<String, Set<Long>>>anyObject());
        EasyMock.expectLastCall();
        callback.saveState(EasyMock.<State>anyObject());
        EasyMock.expectLastCall();
        callback.installFeature(f2);
        EasyMock.expectLastCall();
        callback.resolveBundles(EasyMock.<Set<Bundle>>anyObject(),
                                EasyMock.<Map<Resource, List<Wire>>>anyObject(),
                                EasyMock.<Map<Resource, Bundle>>anyObject());
        EasyMock.expectLastCall();
        callback.callListeners(EasyMock.<FeatureEvent>anyObject());
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
        dstate.features = new HashMap<>();
        dstate.features.put(f1.getId(), f1);
        dstate.features.put(f2.getId(), f2);
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<String, Map<String, Set<String>>>());

        request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = EnumSet.noneOf(Option.class);
        request.overrides = Collections.emptySet();
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = UPDATE_SNAPSHOTS_NONE;
        request.requirements = new HashMap<>();
        addToMapSet(request.requirements, ROOT_REGION, f2.getName());

        deployer.deploy(dstate, request);

        EasyMock.verify(callback);
    }

    private TestBundle createTestBundle(long bundleId, int state, String dir, String name) throws IOException, BundleException {
        URL loc = getClass().getResource(dir + "/" + name + ".mf");
        Manifest man = new Manifest(loc.openStream());
        Hashtable<String, String> headers = new Hashtable<>();
        for (Map.Entry attr : man.getMainAttributes().entrySet()) {
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

        public boolean matches(Object argument) {
            if (!(argument instanceof FeatureEvent)) {
                return false;
            }
            FeatureEvent arg = (FeatureEvent) argument;
            return arg.getFeature() == expected.getFeature()
                    && arg.getType() == expected.getType()
                    && arg.isReplay() == expected.isReplay();
        }

        public void appendTo(StringBuffer buffer) {

        }
    }

}
