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
import java.util.Map;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.TestBase;
import org.apache.karaf.features.Slf4jResolverLog;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Test cases for {@link org.apache.karaf.features.internal.service.FeaturesServiceImpl}
 */
public class FeaturesServiceImplTest extends TestBase {

    Logger logger = LoggerFactory.getLogger(FeaturesServiceImplTest.class);;
    Resolver resolver = new ResolverImpl(new Slf4jResolverLog(logger));
    File dataFile;

    @Before
    public void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }

    @Test
    public void testGetFeature() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        final Map<String, Map<String, Feature>> features = features(transactionFeature);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null, new Storage(), null, null, null, this.resolver, null, "", null, null, null, null, null, 0, 0, 0, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            }
        };
        assertNotNull(impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertSame(transactionFeature, impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0]);
    }
    
    @Test
    public void testGetFeatureStripVersion() throws Exception {
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null, new Storage(), null, null, null, this.resolver, null, "", null, null, null, null, null, 0, 0, 0, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features(feature("transaction", "1.0.0"));
            }
        };
        Feature[] features = impl.getFeatures("transaction", "  1.0.0  ");
        assertEquals(1, features.length);
        Feature feature = features[0];
        assertNotNull(feature);
        assertSame("transaction", feature.getName());
    }
    
    @Test
    public void testGetFeatureNotAvailable() throws Exception {
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null, new Storage(), null, null, null, this.resolver, null, "", null, null, null, null, null, 0, 0, 0, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features(feature("transaction", "1.0.0"));
            }
        };
        assertEquals(0, impl.getFeatures("activemq", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION).length);
    }
    
    @Test
    public void testGetFeatureHighestAvailable() throws Exception {
        final Map<String, Map<String, Feature>> features = features(
                feature("transaction", "1.0.0"),
                feature("transaction", "2.0.0")
        );
        final FeaturesServiceImpl impl = new FeaturesServiceImpl(null, null, new Storage(), null, null, null, this.resolver, null, "", null, null, null, null, null, 0, 0, 0, null) {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            }
        };
        assertNotNull(impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertEquals("2.0.0", impl.getFeatures("transaction", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0].getVersion());
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
