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
import java.util.ArrayList;
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
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.apache.karaf.features.internal.util.MultiException;
import org.apache.karaf.features.internal.support.TestBundle;
import org.apache.karaf.features.internal.support.TestBundleRevision;
import org.apache.karaf.features.internal.support.TestDownloadManager;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.FeaturesService.*;
import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.apache.karaf.features.internal.util.MapUtils.copy;
import static org.easymock.EasyMock.anyInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
        List<Boolean> pendingOnSave = new ArrayList<>();
        EasyMock.expectLastCall().andAnswer(() -> {
            pendingOnSave.add(((State) EasyMock.getCurrentArgument(0)).pendingDeployment != null);
            return null;
        }).times(3);
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
        // saved as pending before the bundles are installed and once they are, completed once they are started
        assertEquals(Arrays.asList(true, true, false), pendingOnSave);
    }

    @Test
    public void testInstallSimpleFeatureWhileFrameworkStarts() throws Exception {
        // boot features are deployed while the framework raises its start level: the deployment completes
        assertEquals(Arrays.asList(true, true, false), deploySimpleFeature(Bundle.STARTING));
    }

    @Test
    public void testInstallSimpleFeatureWhileFrameworkStops() throws Exception {
        // starting bundles does not persist while the framework stops, so the deployment stays pending
        assertEquals(Arrays.asList(true, true), deploySimpleFeature(Bundle.STOPPING));
    }

    @Test
    public void testInstallSimpleFeatureAfterFrameworkStopped() throws Exception {
        // the deployment thread is not interrupted and may outlive the framework
        assertEquals(Arrays.asList(true, true), deploySimpleFeature(Bundle.RESOLVED));
    }

    /**
     * Deploys a simple feature with the system bundle in the given state.
     *
     * @return whether {@link State#pendingDeployment} is set at each save of the state
     */
    private List<Boolean> deploySimpleFeature(int systemBundleState) throws Exception {
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
        List<Boolean> pendingOnSave = new ArrayList<>();
        EasyMock.expectLastCall().andAnswer(() -> {
            pendingOnSave.add(((State) EasyMock.getCurrentArgument(0)).pendingDeployment != null);
            return null;
        }).atLeastOnce();
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
        Hashtable<String, String> systemHeaders = new Hashtable<>();
        systemHeaders.put("Bundle-ManifestVersion", "2");
        systemHeaders.put("Bundle-SymbolicName", "org.eclipse.osgi");
        dstate.bundles.put(0L, new TestBundle(0, "org.eclipse.osgi", systemBundleState, systemHeaders));
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
        return pendingOnSave;
    }

    @Test
    public void testInstallFeatureWithFragment() throws Exception {
        String dataDir = "data4";

        TestDownloadManager manager = new TestDownloadManager(getClass(), dataDir);

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f = repo.getFeatures()[0];

        URL loc = getClass().getResource(dataDir + "/b.mf");
        Manifest man = new Manifest(loc.openStream());
        Hashtable<String, String> headers = new Hashtable<>();
        for (Map.Entry<Object, Object> attr : man.getMainAttributes().entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        Bundle bundle = new TestBundle(1, "b", Bundle.ACTIVE, headers) {
            @Override
            public <A> A adapt(Class<A> type) {
                if (type == BundleRevision.class) {
                    return type.cast(new TestBundleRevision(this));
                }
                return super.adapt(type);
            }
        };
        Bundle fragment = createTestBundle(2L, Bundle.RESOLVED, dataDir, "f");

        Map<String, Bundle> bundles = new HashMap<>();
        bundles.put("b", bundle);
        bundles.put("f", fragment);

        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundles.put(1L, bundle);
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.bundlesPerRegion.put(ROOT_REGION, Collections.singleton(1L));
        dstate.partitionFeatures(Collections.singletonList(f));
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
        addToMapSet(request.requirements, ROOT_REGION, f.getName() + "/" + new VersionRange(f.getVersion(), true));

        final List<String> installedBundles = new ArrayList<>();
        MyDeployCallback callback = new MyDeployCallback(dstate, bundles) {
            @Override
            public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
                installedBundles.add(uri);
                return super.installBundle(region, uri, is);
            }
        };
        Deployer deployer = new Deployer(manager, resolver, callback);

        deployer.deploy(dstate, request);
        assertEquals(1, installedBundles.size());
        assertEquals("f", installedBundles.get(0));
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
        EasyMock.expectLastCall().times(3);
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
        // callback.refreshPackages(EasyMock.eq(Collections.singleton(bundleA)));
        // EasyMock.expectLastCall();
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
        // the bundle is already installed and started: there is no pending deployment
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
        EasyMock.expectLastCall().times(3);
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
        EasyMock.expectLastCall().times(3);
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

    @Test
    public void testPendingDeploymentSavedBeforeBundleOperations() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        Map<String, Bundle> bundles = new HashMap<>();
        bundles.put("a100", createTestBundle(1, Bundle.INSTALLED, "data1", "a100"));

        Deployer.DeploymentState dstate = deploymentState(repo);
        dstate.frameworkUuid = "launch-1";
        Deployer.DeploymentRequest request = deploymentRequest(EnumSet.of(Option.NoAutoRefreshBundles), f100);
        RecordingCallback callback = new RecordingCallback(dstate, bundles);
        new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback).deploy(dstate, request);

        assertEquals(Arrays.asList("save pending", "install a100", "save pending", "start a100", "save"), callback.steps);
        // the new requirements are saved before the first bundle operation, with what is needed to resume them
        State first = callback.saved.get(0);
        assertEquals(request.requirements, first.requirements);
        assertTrue(first.installedFeatures.isEmpty());
        assertEquals("launch-1", first.pendingDeployment.frameworkUuid);
        assertEquals(Collections.singleton(Option.NoAutoRefreshBundles.name()), first.pendingDeployment.options);
        assertEquals(Collections.singleton(f100.getId()), callback.saved.get(1).installedFeatures.get(ROOT_REGION));
    }

    @Test
    public void testDeploymentFailingBeforeNewStateRestoresState() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Deployer.DeploymentState dstate = deploymentState(repo);
        RecordingCallback callback = new RecordingCallback(dstate, new HashMap<>()) {
            @Override
            public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
                throw new BundleException("install failure");
            }
        };
        try {
            new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback)
                    .deploy(dstate, deploymentRequest(EnumSet.noneOf(Option.class), repo.getFeatures()[0]));
            fail("The deployment should fail");
        } catch (BundleException e) {
            // expected
        }

        // a failed deployment is not resumed: the state before the deployment is restored
        assertEquals(Arrays.asList("save pending", "save"), callback.steps);
        assertTrue(callback.lastSaved().requirements.isEmpty());
    }

    @Test
    public void testDeploymentFailingAfterNewStateIsNotPending() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        RecordingCallback callback = deployFailingToResolve(repo, f100, Bundle.ACTIVE);

        // a failed deployment is not resumed, but the bundles it has installed are managed
        assertEquals(Arrays.asList("save pending", "install a100", "save pending", "save"), callback.steps);
        assertEquals(Collections.singleton(f100.getId()), callback.lastSaved().installedFeatures.get(ROOT_REGION));
    }

    @Test
    public void testDeploymentFailingWhileFrameworkStopsStaysPending() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        RecordingCallback callback = deployFailingToResolve(repo, repo.getFeatures()[0], Bundle.STOPPING);

        // the failure may be due to the framework stopping
        assertEquals(Arrays.asList("save pending", "install a100", "save pending"), callback.steps);
    }

    private RecordingCallback deployFailingToResolve(RepositoryImpl repo, Feature feature, int systemBundleState)
            throws Exception {
        Map<String, Bundle> bundles = new HashMap<>();
        bundles.put("a100", createTestBundle(1, Bundle.INSTALLED, "data1", "a100"));
        Deployer.DeploymentState dstate = deploymentState(repo);
        Hashtable<String, String> systemHeaders = new Hashtable<>();
        systemHeaders.put("Bundle-ManifestVersion", "2");
        systemHeaders.put("Bundle-SymbolicName", "org.eclipse.osgi");
        dstate.bundles.put(0L, new TestBundle(0, "org.eclipse.osgi", systemBundleState, systemHeaders));
        RecordingCallback callback = new RecordingCallback(dstate, bundles) {
            @Override
            public void resolveBundles(Set<Bundle> bundles, Map<Resource, List<Wire>> wiring, Map<Resource, Bundle> resToBnd) {
                throw new IllegalStateException("resolve failure");
            }
        };
        try {
            new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback)
                    .deploy(dstate, deploymentRequest(EnumSet.noneOf(Option.class), feature));
            fail("The deployment should fail");
        } catch (IllegalStateException e) {
            // expected
        }
        return callback;
    }

    @Test
    public void testDeploymentRefreshingServiceBundleDoesNotSaveAfterRefresh() throws Exception {
        // the refreshed instance can no longer save the state: the deployment stays pending until the next launch
        assertEquals(Arrays.asList("save pending", "save pending", "refresh", "start a100", "start s"),
                deployRefreshingServiceBundle(EnumSet.noneOf(Option.class)));
    }

    @Test
    public void testDeploymentRefreshingServiceBundleTransitivelyDoesNotSaveAfterRefresh() throws Exception {
        // the features service bundle is not refreshed explicitly, as it is unmanaged, but the framework refreshes it
        // along with the bundle it is wired to
        assertEquals(Arrays.asList("save pending", "save pending", "refresh", "start a100"),
                deployRefreshingServiceBundle(EnumSet.of(Option.NoAutoRefreshUnmanagedBundles)));
    }

    /**
     * Deploys a feature whose bundle is updated, while the features service bundle is wired to it.
     *
     * @return the recorded steps of the deployment
     */
    private List<String> deployRefreshingServiceBundle(EnumSet<Option> options) throws Exception {
        String dataDir = "data5";
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource(dataDir + "/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        Feature f101 = repo.getFeatures()[1];
        TestBundle bundleA = createTestBundle(1, Bundle.ACTIVE, dataDir, "a100");

        // the features service bundle is wired to the bundle of f/1.0.0, which is updated to the one of f/1.0.1 (that
        // requires a capability of the features service bundle): it is refreshed
        BundleWire wire = EasyMock.createMock(BundleWire.class);
        EasyMock.expect(wire.getProvider()).andStubReturn(new TestBundleRevision(bundleA));
        // a refresh replaces the wiring of the bundles it refreshes, including the ones wired to them
        boolean[] refreshed = new boolean[1];
        BundleWiring wiring = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(wiring.isCurrent()).andStubAnswer(() -> !refreshed[0]);
        EasyMock.expect(wiring.getProvidedWires(null)).andStubReturn(Collections.emptyList());
        EasyMock.expect(wiring.getRequiredWires(null)).andStubReturn(Collections.singletonList(wire));
        EasyMock.expect(wiring.listResources("/", "*.class", BundleWiring.LISTRESOURCES_RECURSE))
                .andStubReturn(Collections.emptyList());
        EasyMock.replay(wire, wiring);
        TestBundle serviceBundle = new TestBundle(2, "s", Bundle.ACTIVE, manifestHeaders(dataDir, "s")) {
            final BundleRevision revision = new TestBundleRevision(this);

            @Override
            public <A> A adapt(Class<A> type) {
                if (type == BundleRevision.class) {
                    return type.cast(revision);
                }
                if (type == BundleWiring.class) {
                    return type.cast(wiring);
                }
                return super.adapt(type);
            }
        };

        Deployer.DeploymentState dstate = deploymentState(repo);
        dstate.serviceBundle = serviceBundle;
        addToMapSet(dstate.state.installedFeatures, ROOT_REGION, f100.getId());
        dstate.state.stateFeatures.put(ROOT_REGION, new HashMap<>(Collections.singletonMap(f100.getId(), FeatureState.Started.name())));
        addToMapSet(dstate.state.managedBundles, ROOT_REGION, 1L);
        dstate.bundles.put(1L, bundleA);
        dstate.bundles.put(2L, serviceBundle);
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, 1L);
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, 2L);
        RecordingCallback callback = new RecordingCallback(dstate, new HashMap<>()) {
            @Override
            public void refreshPackages(Collection<Bundle> bundles) {
                super.refreshPackages(bundles);
                refreshed[0] = true;
            }
        };
        Deployer.DeploymentRequest request = deploymentRequest(options, f101);
        request.autoRefresh = true;
        new Deployer(new TestDownloadManager(getClass(), dataDir), resolver, callback).deploy(dstate, request);
        return callback.steps;
    }

    @Test
    public void testDeleteConfigurationsSavedAsPending() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        TestBundle bundleA = createTestBundle(1, Bundle.ACTIVE, "data1", "a100");

        // f/1.0.0 is installed, and uninstalled along with its configurations
        Deployer.DeploymentState dstate = deploymentState(repo);
        addToMapSet(dstate.state.requirements, ROOT_REGION, f100.getName() + "/" + new VersionRange(f100.getVersion(), true));
        addToMapSet(dstate.state.installedFeatures, ROOT_REGION, f100.getId());
        dstate.state.stateFeatures.put(ROOT_REGION, new HashMap<>(Collections.singletonMap(f100.getId(), FeatureState.Started.name())));
        addToMapSet(dstate.state.managedBundles, ROOT_REGION, 1L);
        dstate.bundles.put(1L, bundleA);
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, 1L);
        RecordingCallback callback = new RecordingCallback(dstate, new HashMap<>());
        new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback)
                .deploy(dstate, deploymentRequest(EnumSet.of(Option.DeleteConfigurations)));

        assertEquals(Arrays.asList("save pending", "save pending", "deleteConfigs " + f100.getId(), "save pending", "save"),
                callback.steps);
        assertEquals(Collections.singletonMap(ROOT_REGION, Collections.singleton(f100.getId())),
                callback.saved.get(0).pendingDeployment.configsToDelete);
        // once deleted, the configurations are no longer to delete, even if the deployment is resumed
        assertTrue(callback.saved.get(2).pendingDeployment.configsToDelete.isEmpty());
    }

    @Test
    public void testDeploymentWithoutBundleChangesSavesOnce() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        RecordingCallback callback = deployInstalledFeature(repo, f100, null);

        assertEquals(Collections.singletonList("save"), callback.steps);
    }

    @Test
    public void testDeploymentWithoutBundleChangesEndsInterruptedDeployment() throws Exception {
        // e.g. the interrupted deployment had started all bundles, but the framework stopped before it was saved
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        RecordingCallback callback = deployInstalledFeature(repo, f100, new State.PendingDeployment());

        assertEquals(Collections.singletonList("save"), callback.steps);
        assertNull(callback.lastSaved().pendingDeployment);
    }

    /**
     * Deploys the given feature, which is installed and started already.
     */
    private RecordingCallback deployInstalledFeature(RepositoryImpl repo, Feature feature,
                                                     State.PendingDeployment interrupted) throws Exception {
        Deployer.DeploymentState dstate = deploymentState(repo);
        addToMapSet(dstate.state.requirements, ROOT_REGION, feature.getName() + "/" + new VersionRange(feature.getVersion(), true));
        addToMapSet(dstate.state.installedFeatures, ROOT_REGION, feature.getId());
        dstate.state.stateFeatures.put(ROOT_REGION, new HashMap<>(Collections.singletonMap(feature.getId(), FeatureState.Started.name())));
        addToMapSet(dstate.state.managedBundles, ROOT_REGION, 1L);
        dstate.state.pendingDeployment = interrupted;
        dstate.bundles.put(1L, createTestBundle(1, Bundle.ACTIVE, "data1", "a100"));
        addToMapSet(dstate.bundlesPerRegion, ROOT_REGION, 1L);
        RecordingCallback callback = new RecordingCallback(dstate, new HashMap<>());
        new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback)
                .deploy(dstate, deploymentRequest(EnumSet.noneOf(Option.class), feature));
        return callback;
    }

    @Test
    public void testResumeFailingBeforeBundleChangesEndsInterruptedDeployment() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Deployer.DeploymentState dstate = deploymentState(repo);
        addToMapSet(dstate.state.requirements, ROOT_REGION, "missing/1.0.0");
        dstate.state.pendingDeployment = new State.PendingDeployment();
        RecordingCallback callback = new RecordingCallback(dstate, new HashMap<>());
        Deployer.DeploymentRequest request = deploymentRequest(EnumSet.noneOf(Option.class));
        request.requirements = copy(dstate.state.requirements);
        try {
            new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback).deploy(dstate, request);
            fail("The deployment should fail");
        } catch (Exception e) {
            // expected: the feature does not exist (e.g. its repository is not available)
        }

        // otherwise, the deployment would fail again on each start
        assertEquals(Collections.singletonList("save"), callback.steps);
        assertNull(callback.lastSaved().pendingDeployment);
        assertEquals(dstate.state.requirements, callback.lastSaved().requirements);
    }

    @Test
    public void testFailedStartWhileFrameworkStopsIsReportedOnce() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Map<String, Bundle> bundles = new HashMap<>();
        bundles.put("a100", createTestBundle(1, Bundle.INSTALLED, "data1", "a100"));
        Deployer.DeploymentState dstate = deploymentState(repo);
        Hashtable<String, String> systemHeaders = new Hashtable<>();
        systemHeaders.put("Bundle-ManifestVersion", "2");
        systemHeaders.put("Bundle-SymbolicName", "org.eclipse.osgi");
        dstate.bundles.put(0L, new TestBundle(0, "org.eclipse.osgi", Bundle.STOPPING, systemHeaders));
        List<String> messages = new ArrayList<>();
        RecordingCallback callback = new RecordingCallback(dstate, bundles) {
            @Override
            public void print(String message, boolean verbose) {
                messages.add(message);
            }

            @Override
            public void startBundle(Bundle bundle) throws BundleException {
                throw new BundleException("the framework is stopping");
            }
        };
        try {
            new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback)
                    .deploy(dstate, deploymentRequest(EnumSet.noneOf(Option.class), repo.getFeatures()[0]));
            fail("The deployment should fail");
        } catch (MultiException e) {
            // expected
        }

        assertEquals(Arrays.asList("save pending", "install a100", "save pending"), callback.steps);
        assertEquals(1, messages.stream().filter(m -> m.startsWith("Framework is stopping")).count());
    }

    @Test
    public void testResumeDeletesConfigurationsOfInterruptedDeployment() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        RecordingCallback callback = resumeWithConfigsToDelete(repo, f100);

        assertTrue(callback.steps.contains("deleteConfigs " + f100.getId()));
        assertNull(callback.lastSaved().pendingDeployment);
    }

    @Test
    public void testResumeKeepsConfigurationsOfReinstalledFeature() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());
        Feature f100 = repo.getFeatures()[0];
        RecordingCallback callback = resumeWithConfigsToDelete(repo, f100, f100);

        assertFalse(callback.steps.contains("deleteConfigs " + f100.getId()));
        assertNull(callback.lastSaved().pendingDeployment);
    }

    /**
     * Deploys the given features after a deployment that was interrupted before deleting the configurations of the
     * given feature.
     */
    private RecordingCallback resumeWithConfigsToDelete(RepositoryImpl repo, Feature uninstalled, Feature... features)
            throws Exception {
        Map<String, Bundle> bundles = new HashMap<>();
        bundles.put("a100", createTestBundle(1, Bundle.INSTALLED, "data1", "a100"));
        Deployer.DeploymentState dstate = deploymentState(repo);
        dstate.state.pendingDeployment = new State.PendingDeployment();
        addToMapSet(dstate.state.pendingDeployment.configsToDelete, ROOT_REGION, uninstalled.getId());
        RecordingCallback callback = new RecordingCallback(dstate, bundles);
        new Deployer(new TestDownloadManager(getClass(), "data1"), resolver, callback)
                .deploy(dstate, deploymentRequest(EnumSet.noneOf(Option.class), features));
        return callback;
    }

    private Deployer.DeploymentState deploymentState(RepositoryImpl repo) throws Exception {
        Deployer.DeploymentState dstate = new Deployer.DeploymentState();
        dstate.state = new State();
        dstate.bundles = new HashMap<>();
        dstate.bundlesPerRegion = new HashMap<>();
        dstate.partitionFeatures(Arrays.asList(repo.getFeatures()));
        dstate.filtersPerRegion = new HashMap<>();
        dstate.filtersPerRegion.put(ROOT_REGION, new HashMap<>());
        return dstate;
    }

    private Deployer.DeploymentRequest deploymentRequest(EnumSet<Option> options, Feature... features) {
        Deployer.DeploymentRequest request = new Deployer.DeploymentRequest();
        request.bundleUpdateRange = DEFAULT_BUNDLE_UPDATE_RANGE;
        request.featureResolutionRange = DEFAULT_FEATURE_RESOLUTION_RANGE;
        request.globalRepository = null;
        request.options = options;
        request.stateChanges = Collections.emptyMap();
        request.updateSnaphots = SnapshotUpdateBehavior.None;
        request.requirements = new HashMap<>();
        for (Feature feature : features) {
            addToMapSet(request.requirements, ROOT_REGION, feature.getName() + "/" + new VersionRange(feature.getVersion(), true));
        }
        return request;
    }

    private Hashtable<String, String> manifestHeaders(String dir, String name) throws IOException {
        URL loc = getClass().getResource(dir + "/" + name + ".mf");
        Manifest man = new Manifest(loc.openStream());
        Hashtable<String, String> headers = new Hashtable<>();
        for (Map.Entry<Object, Object> attr : man.getMainAttributes().entrySet()) {
            headers.put(attr.getKey().toString(), attr.getValue().toString());
        }
        return headers;
    }

    /**
     * Records the steps of a deployment that matter for {@link State#pendingDeployment}, and the saved states.
     */
    private static class RecordingCallback extends MyDeployCallback {
        final List<String> steps = new ArrayList<>();
        final List<State> saved = new ArrayList<>();

        RecordingCallback(Deployer.DeploymentState dstate, Map<String, Bundle> bundles) {
            super(dstate, bundles);
        }

        State lastSaved() {
            return saved.get(saved.size() - 1);
        }

        @Override
        public void saveState(State state) {
            steps.add(state.pendingDeployment != null ? "save pending" : "save");
            saved.add(state.copy());
            super.saveState(state);
        }

        @Override
        public Bundle installBundle(String region, String uri, InputStream is) throws BundleException {
            steps.add("install " + uri);
            return super.installBundle(region, uri, is);
        }

        @Override
        public void startBundle(Bundle bundle) throws BundleException {
            steps.add("start " + bundle.getLocation());
            ((TestBundle) bundle).state = Bundle.ACTIVE;
        }

        @Override
        public void stopBundle(Bundle bundle, int options) {
            ((TestBundle) bundle).state = Bundle.RESOLVED;
        }

        @Override
        public void refreshPackages(Collection<Bundle> bundles) {
            steps.add("refresh");
        }

        @Override
        public void deleteConfigs(Feature feature) {
            steps.add("deleteConfigs " + feature.getId());
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
        public void deleteConfigs(Feature feature) throws IOException, InvalidSyntaxException {

        }

        @Override
        public void installLibraries(Feature feature) throws IOException {

        }

        @Override
        public void bundleBlacklisted(BundleInfo bundleInfo) {

        }
    }
}
