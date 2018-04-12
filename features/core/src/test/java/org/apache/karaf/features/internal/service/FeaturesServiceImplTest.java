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
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureState;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.TestBase;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.internal.download.DownloadManager;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.apache.karaf.features.internal.service.BundleInstallSupport.FrameworkInfo;
import org.apache.karaf.features.internal.support.TestDownloadManager;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link org.apache.karaf.features.internal.service.FeaturesServiceImpl}
 */
public class FeaturesServiceImplTest extends TestBase {

    Logger logger = LoggerFactory.getLogger(FeaturesServiceImplTest.class);
    Resolver resolver = new ResolverImpl(new Slf4jResolverLog(logger));
    File dataFile;

    @Before
    public void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
        URL.setURLStreamHandlerFactory(protocol -> protocol.equals("custom") ? new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return getClass().getResource(u.getPath()).openConnection();
            }
        } : null);
    }
    
    @After
    public void after() throws Exception {
        Field field = URL.class.getDeclaredField("factory");
        field.setAccessible(true);
        field.set(null, null);
    }
    
    @Test
    public void testListFeatureWithoutVersion() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesServiceImpl impl = featuresServiceWithFeatures(transactionFeature);
        assertNotNull(impl.getFeatures("transaction", null));
        assertSame(transactionFeature, impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0]);
    }

    @Test
    public void testGetFeature() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesServiceImpl impl = featuresServiceWithFeatures(transactionFeature);
        assertNotNull(impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertSame(transactionFeature, impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0]);
    }
    
    @Test
    public void testGetFeatureStripVersion() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesServiceImpl impl = featuresServiceWithFeatures(transactionFeature);
        Feature[] features = impl.getFeatures("transaction", "  1.0.0  ");
        assertEquals(1, features.length);
        Feature feature = features[0];
        assertNotNull(feature);
        assertSame("transaction", feature.getName());
    }
    
    @Test
    public void testGetFeatureNotAvailable() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesServiceImpl impl = featuresServiceWithFeatures(transactionFeature);
        assertEquals(0, impl.getFeatures("activemq", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION).length);
    }
    
    @Test
    public void testGetFeatureHighestAvailable() throws Exception {
        FeaturesServiceImpl impl = featuresServiceWithFeatures(feature("transaction", "1.0.0"),
                                                               feature("transaction", "2.0.0"));
        assertNotNull(impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertEquals("2.0.0", impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0].getVersion());
    }

    @Test
    public void testCyclicFeatures() throws Exception {
        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        EasyMock.replay(installSupport);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(new Storage(), null, null, this.resolver, installSupport, null, cfg);
        impl.addRepository(URI.create("custom:cycle/a-references-b.xml"));
        impl.getFeatureCache();
    }
    
    @Test
    public void testRemoveRepo1() throws Exception {
        final FeaturesService featureService = createTestFeatureService();
        URI repoA = URI.create("custom:remove/a.xml");
        featureService.addRepository(repoA);
        Feature a1Feature = featureService.getFeature("a1");
        installFeature(featureService, a1Feature);
        Feature b1Feature = featureService.getFeature("b1");
        installFeature(featureService, b1Feature);
        featureService.removeRepository(repoA);
        assertNotInstalled(featureService, a1Feature);
        assertNotInstalled(featureService, b1Feature);
    }
    
    @Test
    public void testGetFeatureWithVersion() throws Exception {
        final FeaturesService featureService = createTestFeatureService();
        URI repoA = URI.create("custom:f09.xml");
        featureService.addRepository(repoA);
        Feature feature = featureService.getFeature("test", "1.0.0");
        assertEquals("1.0.0", feature.getVersion());
    }

    @Test
    public void testInstallAndUpdate() throws Exception {
        final FeaturesService featureService = createTestFeatureService();
        URI repoA = URI.create("custom:f09.xml");
        featureService.addRepository(repoA);
        Feature test100 = featureService.getFeature("test", "1.0.0");
        installFeature(featureService, test100);
        assertInstalled(featureService, test100);
        Feature test110 = featureService.getFeature("test", "1.1.0");
        featureService.installFeature(test110, EnumSet.of(Option.Upgrade));
        waitInstalled(featureService, test110);
        assertNotInstalled(featureService, test100);
    }

    @Test
    public void testInstallAndStop() throws Exception {
        Capture<Bundle> stoppedBundle = Capture.newInstance();

        Bundle bundle = EasyMock.niceMock(Bundle.class);
        BundleStartLevel bundleStartLevel = EasyMock.niceMock(BundleStartLevel.class);
        BundleRevision bundleRevision = EasyMock.niceMock(BundleRevision.class);

        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        FrameworkInfo dummyInfo = new FrameworkInfo();
        expect(installSupport.getInfo()).andReturn(dummyInfo).atLeastOnce();
        expect(installSupport.installBundle(EasyMock.eq("root"), EasyMock.eq("a100"), anyObject())).andReturn(bundle);
        installSupport.startBundle(bundle);
        expectLastCall();
        expect(bundle.getBundleId()).andReturn(1L).anyTimes();
        expect(bundle.getSymbolicName()).andReturn("a").anyTimes();
        expect(bundle.getVersion()).andReturn(new Version("1.0.0")).anyTimes();
        expect(bundle.getHeaders()).andReturn(new Hashtable<>()).anyTimes();
        expect(bundle.adapt(BundleStartLevel.class)).andReturn(bundleStartLevel).anyTimes();
        expect(bundle.adapt(BundleRevision.class)).andReturn(bundleRevision).anyTimes();
        expect(bundleRevision.getBundle()).andReturn(bundle).anyTimes();
        expect(bundleRevision.getCapabilities(null)).andReturn(Collections.emptyList()).anyTimes();
        expect(bundleRevision.getRequirements(null)).andReturn(Collections.emptyList()).anyTimes();
        EasyMock.replay(installSupport, bundle, bundleStartLevel, bundleRevision);
        FeaturesService featureService =  new FeaturesServiceImpl(new Storage(), null, null, this.resolver,
                installSupport, null, cfg) {
            @Override
            protected DownloadManager createDownloadManager() throws IOException {
                return new TestDownloadManager(FeaturesServiceImplTest.class, "data1");
            }
        };

        URI repoA = URI.create("custom:data1/features.xml");
        featureService.addRepository(repoA);
        Feature test100 = featureService.getFeature("f", "1.0.0");
        installFeature(featureService, test100);
        assertInstalled(featureService, test100);

        dummyInfo.bundles.put(1L, bundle);
        Map<String, Map<String, FeatureState>> states = new HashMap<>();
        states.computeIfAbsent("root", k -> new HashMap<>()).put("f/1.0.0", FeatureState.Resolved);
        EasyMock.reset(installSupport, bundle, bundleRevision, bundleStartLevel);
        expect(installSupport.getInfo()).andReturn(dummyInfo).anyTimes();
        installSupport.stopBundle(EasyMock.capture(stoppedBundle), EasyMock.anyInt());
        expectLastCall();
        expect(bundle.getBundleId()).andReturn(1L).anyTimes();
        expect(bundle.getSymbolicName()).andReturn("a").anyTimes();
        expect(bundle.getVersion()).andReturn(new Version("1.0.0")).anyTimes();
        expect(bundle.getHeaders()).andReturn(new Hashtable<>()).anyTimes();
        expect(bundle.adapt(BundleStartLevel.class)).andReturn(bundleStartLevel).anyTimes();
        expect(bundle.adapt(BundleRevision.class)).andReturn(bundleRevision).anyTimes();
        expect(bundleRevision.getBundle()).andReturn(bundle).anyTimes();
        expect(bundleRevision.getCapabilities(null)).andReturn(Collections.emptyList()).anyTimes();
        expect(bundleRevision.getRequirements(null)).andReturn(Collections.emptyList()).anyTimes();
        EasyMock.replay(installSupport, bundle, bundleRevision, bundleStartLevel);

        featureService.updateFeaturesState(states, EnumSet.noneOf(Option.class));
        assertSame(bundle, stoppedBundle.getValue());
    }

    @Test
    public void testRemoveRepo2() throws Exception {
        final FeaturesService featureService = createTestFeatureService();
        URI repoA = URI.create("custom:remove/a.xml");
        URI repoB = URI.create("custom:remove/b.xml");
        featureService.addRepository(repoA);
        featureService.addRepository(repoB);
        Feature a1Feature = featureService.getFeature("a1");
        installFeature(featureService, a1Feature);
        Feature b1Feature = featureService.getFeature("b1");
        installFeature(featureService, b1Feature);
        featureService.removeRepository(repoA);
        assertNotInstalled(featureService, a1Feature);
        assertInstalled(featureService, b1Feature);
    }

    private FeaturesServiceImpl featuresServiceWithFeatures(Feature... staticFeatures) {
        final Map<String, Map<String, Feature>> features = features(staticFeatures);
        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        EasyMock.replay(installSupport);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(new Storage(), null, null, this.resolver, installSupport, null, cfg ) {
            protected Map<String,Map<String,Feature>> getFeatureCache() throws Exception {
                return features;
            }
        };
        return impl;
    }

    private FeaturesServiceImpl createTestFeatureService() {
        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        FrameworkInfo dummyInfo = new FrameworkInfo();
        expect(installSupport.getInfo()).andReturn(dummyInfo).atLeastOnce();
        EasyMock.replay(installSupport);
        return new FeaturesServiceImpl(new Storage(), null, null, this.resolver,
                                       installSupport, null, cfg);
    }

    private void assertNotInstalled(FeaturesService featureService, Feature feature) {
        assertFalse("Feature " + feature.getName() + " should not be installed anymore after removal of repo",
                    featureService.isInstalled(feature));
    }
    
    private void assertInstalled(FeaturesService featureService, Feature feature) {
        assertTrue("Feature " + feature.getName() + " should still be installed after removal of repo",
                    featureService.isInstalled(feature));
    }

    private void installFeature(final FeaturesService featureService, Feature feature)
        throws Exception {
        featureService.installFeature(feature, EnumSet.noneOf(Option.class));
        waitInstalled(featureService, feature);
    }

    private void waitInstalled(final FeaturesService featureService, Feature feature)
        throws InterruptedException {
        int count = 40;
        while (!featureService.isInstalled(feature) && count > 0) {
            Thread.sleep(100);
            count --;
        }
        if (count == 0) {
            throw new RuntimeException("Feature " + feature + " not installed.");
        }
    }
    
    /**
     * This test ensures that every feature get installed only once, even if it appears multiple times in the list
     * of transitive feature dependencies (KARAF-1600)
     */
    /*
    @Test
    @SuppressWarnings("unchecked")
    public void testNoDuplicateFeaturesInstallation() throws Exception {
        final List<Feature> installed = new LinkedList<Feature>();
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.installBundleIfNeeded(EasyMock.anyObject(String.class), EasyMock.anyInt(), EasyMock.anyObject(String.class)))
            .andReturn(new BundleInstallerResult(createDummyBundle(1l, "", headers()), true)).anyTimes();
        bundleManager.refreshBundles(EasyMock.anyObject(Set.class), EasyMock.anyObject(Set.class), EasyMock.anyObject(EnumSet.class));
        EasyMock.expectLastCall();
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(bundleManager, null) {
            // override methods which refers to bundle context to avoid mocking everything
            @Override
            protected boolean loadState() {
                return true;
            }

            @Override
            protected void saveDigraph() {

            }

            @Override
            protected void doInstallFeature(InstallationState state, Feature feature, boolean verbose) throws Exception {
                installed.add(feature);

                super.doInstallFeature(state, feature, verbose);
            }

        };
        replay(bundleManager);
        impl.addRepository(getClass().getResource("repo2.xml").toURI());
        impl.installFeature("all");

        // copying the features to a set to filter out the duplicates
        Set<Feature> noduplicates = new HashSet<Feature>();
        noduplicates.addAll(installed);

        assertEquals("Every feature should only have been installed once", installed.size(), noduplicates.size());
    }

    @Test
    public void testGetOptionalImportsOnly() {
        BundleManager bundleManager = new BundleManager(null, 0l);

        List<Clause> result = bundleManager.getOptionalImports("org.apache.karaf,org.apache.karaf.optional;resolution:=optional");
        assertEquals("One optional import expected", 1, result.size());
        assertEquals("org.apache.karaf.optional", result.get(0).getName());

        result = bundleManager.getOptionalImports(null);
        assertNotNull(result);
        assertEquals("No optional imports expected", 0, result.size());
    }
    */

    static class Storage extends StateStorage {
        @Override
        protected InputStream getInputStream() throws IOException {
            return null;
        }
        @Override
        protected OutputStream getOutputStream() throws IOException {
            return null;
        }
    }
}
