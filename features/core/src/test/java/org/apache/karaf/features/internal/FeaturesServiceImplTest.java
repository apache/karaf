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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.internal.BundleManager.BundleInstallerResult;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link FeaturesServiceImpl}
 */
public class FeaturesServiceImplTest extends TestBase {
    
    File dataFile;

    @Before
    public void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }

    @Test
    public void testGetFeature() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        final Map<String, Map<String, Feature>> features = features(transactionFeature);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertSame(transactionFeature, impl.getFeature("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
    }
    
    @Test
    public void testGetFeatureStripVersion() throws Exception {
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features(feature("transaction", "1.0.0"));
            };
        };
        Feature feature = impl.getFeature("transaction", "  1.0.0  ");
        assertNotNull(feature);
        assertSame("transaction", feature.getName());
    }
    
    @Test
    public void testGetFeatureNotAvailable() throws Exception {
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features(feature("transaction", "1.0.0"));
            };
        };
        assertNull(impl.getFeature("activemq", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
    }
    
    @Test
    public void testGetFeatureHighestAvailable() throws Exception {
        final Map<String, Map<String, Feature>> features = features(
                feature("transaction", "1.0.0"),
                feature("transaction", "2.0.0")
        );
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertSame("2.0.0", impl.getFeature("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION).getVersion());
    }

    @Test
    public void testStartDoesNotFailWithOneInvalidUri()  {
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.createAndRegisterEventAdminListener()).andReturn(null);
        replay(bundleManager);
        FeaturesServiceImpl service = new FeaturesServiceImpl(bundleManager, null);
        try {
            service.setUrls("mvn:inexistent/features/1.0/xml/features");
            service.start();
        } catch (Exception e) {
            fail(String.format("Service should not throw start-up exception but log the error instead: %s", e));
        }
    }

    /**
     * This test checks KARAF-388 which allows you to specify version of boot feature.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testStartDoesNotFailWithNonExistentVersion()  {
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.createAndRegisterEventAdminListener()).andReturn(null);
        bundleManager.refreshBundles(EasyMock.anyObject(Set.class), EasyMock.anyObject(Set.class), EasyMock.anyObject(EnumSet.class));
        EasyMock.expectLastCall().anyTimes();

        final Map<String, Map<String, Feature>> features = features(
                feature("transaction", "1.0.0"),
                feature("transaction", "2.0.0"),
                feature("ssh", "1.0.0")
        );

        final FeaturesServiceImpl impl = new FeaturesServiceImpl(bundleManager, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };

            // override methods which refers to bundle context to avoid mocking everything
            @Override
            protected boolean loadState() {
                return true;
            }
            @Override
            protected void saveState() {
            }
        };
       
        BootFeaturesInstaller bootFeatures = new BootFeaturesInstaller(impl, "transaction;version=1.2,ssh;version=1.0.0");
        replay(bundleManager);
        try {
            Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0]));
            impl.start();
            bootFeatures.installBootFeatures();
            assertFalse("Feature transaction 1.0.0 should not be installed", impl.isInstalled(impl.getFeature("transaction", "1.0.0")));
            assertFalse("Feature transaction 2.0.0 should not be installed", impl.isInstalled(impl.getFeature("transaction", "2.0.0")));
            assertTrue("Feature ssh should be installed", impl.isInstalled(impl.getFeature("ssh", "1.0.0")));
        } catch (Exception e) {
            fail(String.format("Service should not throw start-up exception but log the error instead: %s", e));
        }
        
    }
    
    /**
     * This test ensures that every feature get installed only once, even if it appears multiple times in the list
     * of transitive feature dependencies (KARAF-1600)
     */
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
            protected void saveState() {

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
        BundleManager bundleManager = new BundleManager(null, null, 0l);

        List<Clause> result = bundleManager.getOptionalImports("org.apache.karaf,org.apache.karaf.optional;resolution:=optional");
        assertEquals("One optional import expected", 1, result.size());
        assertEquals("org.apache.karaf.optional", result.get(0).getName());

        result = bundleManager.getOptionalImports(null);
        assertNotNull(result);
        assertEquals("No optional imports expected", 0, result.size());
    }
}
