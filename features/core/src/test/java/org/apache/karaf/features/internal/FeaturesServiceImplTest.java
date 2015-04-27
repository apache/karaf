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

import static org.easymock.EasyMock.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.felix.utils.manifest.Clause;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkListener;

/**
 * Test cases for {@link FeaturesServiceImpl}
 */
public class FeaturesServiceImplTest extends TestCase {
    
    File dataFile;

    protected void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }

    public void testGetFeature() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        FeatureImpl feature = new FeatureImpl("transaction");
        versions.put("1.0.0", feature);
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION));
        assertSame(feature, impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION));
    }
    
    public void testGetFeatureStripVersion() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        FeatureImpl feature = new FeatureImpl("transaction");
        versions.put("1.0.0", feature);
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", "  1.0.0  "));
        assertSame(feature, impl.getFeature("transaction", "  1.0.0   "));
    }
    
    public void testGetFeatureNotAvailable() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        versions.put("1.0.0", new FeatureImpl("transaction"));
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNull(impl.getFeature("activemq", FeatureImpl.DEFAULT_VERSION));
    }
    
    public void testGetFeatureHighestAvailable() throws Exception {
        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        versions.put("1.0.0", new FeatureImpl("transaction", "1.0.0"));
        versions.put("2.0.0", new FeatureImpl("transaction", "2.0.0"));
        features.put("transaction", versions);
        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
            protected Map<String,Map<String,Feature>> getFeatures() throws Exception {
                return features;
            };
        };
        assertNotNull(impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION));
        assertSame("2.0.0", impl.getFeature("transaction", FeatureImpl.DEFAULT_VERSION).getVersion());
    }

    public void testStartDoesNotFailWithOneInvalidUri()  {
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        expect(bundleContext.getBundles()).andReturn(null);
        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        bundleContext.addFrameworkListener(EasyMock.<FrameworkListener>anyObject());
        bundleContext.removeFrameworkListener(EasyMock.<FrameworkListener>anyObject());
        replay(bundleContext);
        FeaturesServiceImpl service = new FeaturesServiceImpl();
        service.setBundleContext(bundleContext);
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
    public void testStartDoesNotFailWithNonExistentVersion()  {
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);

        final Map<String, Map<String, Feature>> features = new HashMap<String, Map<String,Feature>>();
        Map<String, Feature> versions = new HashMap<String, Feature>();
        versions.put("1.0.0", new FeatureImpl("transaction", "1.0.0"));
        versions.put("2.0.0", new FeatureImpl("transaction", "2.0.0"));
        features.put("transaction", versions);

        Map<String, Feature> versions2 = new HashMap<String, Feature>();
        versions2.put("1.0.0", new FeatureImpl("ssh", "1.0.0"));
        features.put("ssh", versions2);
        
        final CountDownLatch latch = new CountDownLatch(2);

        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
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
                // this method will be invoked twice while features service is starting
                latch.countDown();
            }
            @Override
            protected Set<Bundle> findBundlesToRefresh() {
                return Collections.emptySet();
            }
        };
        impl.setBundleContext(bundleContext);

        try {
            Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0]));
            impl.setBoot("transaction;version=1.2,ssh;version=1.0.0");
            impl.start();

            // waiting for the features service installation thread to finish its work
            latch.await(2, TimeUnit.SECONDS);

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
    public void testNoDuplicateFeaturesInstallation() throws Exception {
        final List<Feature> installed = new LinkedList<Feature>();

        final FeaturesServiceImpl impl = new FeaturesServiceImpl() {
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

            @Override
            protected InstallResult installBundleIfNeeded(InstallationState state, BundleInfo bundleInfo, boolean verbose) throws IOException, BundleException {
                // let's return a mock bundle and bundle id to keep the features service happy
                Bundle bundle = createNiceMock(Bundle.class);
                expect(bundle.getBundleId()).andReturn(10l).anyTimes();
                replay(bundle);
                return new InstallResult(false, bundle, 0);
            }
            
            @Override
            protected  Bundle isBundleInstalled(BundleInfo bundleInfo) throws IOException, BundleException {
                // let's return a mock bundle and bundle id to keep the features service happy
                Bundle bundle = createNiceMock(Bundle.class);
                expect(bundle.getBundleId()).andReturn(10l).anyTimes();
                replay(bundle);
                return bundle;
            }

            @Override
            protected Set<Bundle> findBundlesToRefresh() {
                return Collections.emptySet();
            }
        };
        impl.addRepository(getClass().getResource("repo2.xml").toURI());

        try {
            impl.installFeature("all");

            // copying the features to a set to filter out the duplicates
            Set<Feature> noduplicates = new HashSet<Feature>();
            noduplicates.addAll(installed);

            assertEquals("Every feature should only have been installed once", installed.size(), noduplicates.size());
        } catch (Exception e) {
            fail(String.format("Service should not throw any exceptions: %s", e));
        }
    }

}
