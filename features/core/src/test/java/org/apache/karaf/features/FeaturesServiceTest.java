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
package org.apache.karaf.features;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.karaf.features.internal.BundleManager;
import org.apache.karaf.features.internal.BundleManager.BundleInstallerResult;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.karaf.features.internal.TestBase;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class FeaturesServiceTest extends TestBase {
    private static final String FEATURE_WITH_INVALID_BUNDLE = "<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
            + "  <feature name='f1'><bundle>%s</bundle><bundle>zfs:unknown</bundle></feature>"
            + "  <feature name='f2'><bundle>%s</bundle></feature>"
            + "</features>";

    File dataFile;

    @Before
    public void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }
    
    private URI createTempRepo(String repoContent, Object ... variables) throws IOException {
        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.printf(repoContent, variables);
        pw.close();
        return tmp.toURI();
    }

    @Test
    public void testInstallFeature() throws Exception {
        URI uri = createTempRepo(
                "<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>" 
                + "  <feature name='f1'><bundle start='true'>bundle-f1</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle = createDummyBundle(12345L, "bundle-f1", headers());
        FeaturesServiceImpl svc = testAddRepository("bundle-f1", uri, bundleManager, installedBundle);
        
        reset(bundleManager);
        
        expect(bundleManager.installBundleIfNeeded(eq("bundle-f1"), eq(0), eq((String)null))).andReturn(new BundleInstallerResult(installedBundle, true));
        expect(bundleManager.getDataFile(EasyMock.anyObject(String.class))).andReturn(dataFile);
        ignoreRefreshes(bundleManager);
        replay(bundleManager);
        svc.installFeature("f1", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        verify(bundleManager);
        
        Feature[] installed = svc.listInstalledFeatures();
        assertEquals(1, installed.length);
        assertEquals("f1", installed[0].getName());
    }

    private FeaturesServiceImpl testAddRepository(String name, URI uri, BundleManager bundleManager,
            Bundle installedBundle) throws IOException, BundleException, Exception {
        expect(bundleManager.getDataFile(EasyMock.anyObject(String.class))).andReturn(dataFile);
        expect(bundleManager.installBundleIfNeeded(eq(name), eq(0), eq((String)null))).andReturn(new BundleInstallerResult(installedBundle, true)).anyTimes();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        Repository[] repositories = svc.listRepositories();
        verify(bundleManager);

        assertNotNull(repositories);
        assertEquals(1, repositories.length);
        assertNotNull(repositories[0]);
        Feature[] features = repositories[0].getFeatures();
        assertNotNull(features);
        assertEquals(1, features.length);
        assertNotNull(features[0]);
        assertEquals("f1", features[0].getName());
        assertNotNull(features[0].getDependencies());
        assertEquals(0, features[0].getDependencies().size());
        assertNotNull(features[0].getBundles());
        assertEquals(1, features[0].getBundles().size());
        assertEquals(name, features[0].getBundles().get(0).getLocation());
        assertTrue(features[0].getBundles().get(0).isStart());
        return svc;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUninstallFeatureWithTwoVersions() throws Exception {
        URI uri  = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><bundle>bundle-0.1</bundle></feature>"
                + "  <feature name='f1' version='0.2'><bundle>bundle-0.1</bundle></feature>" 
                + "</features>");

        Bundle bundlef101 = createDummyBundle(12345L, "bundle-0.1", headers());

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.anyObject(String.class))).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded("bundle-0.1", 0, null)).andReturn(new BundleInstallerResult(bundlef101, true));
        expect(bundleManager.installBundleIfNeeded("bundle-0.1", 0, null)).andReturn(new BundleInstallerResult(bundlef101, false));
        ignoreRefreshes(bundleManager);
        bundleManager.uninstallById(Collections.EMPTY_SET);        
        EasyMock.expectLastCall();
        bundleManager.uninstallById(setOf(12345L));
        EasyMock.expectLastCall();
        
        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);

        try {
            svc.uninstallFeature("f1");
            fail("Uninstall should have failed as feature is not installed");
        } catch (Exception e) {
            // ok
        }

        svc.installFeature("f1", "0.1", EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        svc.installFeature("f1", "0.2", EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));

        try {
            svc.uninstallFeature("f1");
            fail("Uninstall should have failed as feature is installed in multiple versions");
        } catch (Exception e) {
            // ok
        }

        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f1");
        verify(bundleManager);
    }    
    
    @Test
    public void testAddAndRemoveRepository() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><bundle>bundle-f1-0.1</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        EasyMock.verify(bundleManager);

        svc.addRepository(uri);                                                     
        svc.removeRepository(uri);
        verify(bundleManager);
    }

    // Tests install of a Repository that includes a feature
    // with a feature dependency
    // The dependant feature is in the same repository
    // Tests uninstall of features
    @Test
    public void testInstallFeatureWithDependantFeatures() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><feature version='0.1'>f2</feature><bundle>bundle-f1-0.1</bundle></feature>"
                + "  <feature name='f2' version='0.1'><bundle>bundle-f2-0.1</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle bundlef101 = createDummyBundle(12345L, "bundle-f1-0.1", headers());
        Bundle bundlef201 = createDummyBundle(54321L, "bundle-f2-0.1", headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded("bundle-f1-0.1", 0, null)).andReturn(new BundleInstallerResult(bundlef101, true));
        expect(bundleManager.installBundleIfNeeded("bundle-f2-0.1", 0, null)).andReturn(new BundleInstallerResult(bundlef201, true));
        ignoreRefreshes(bundleManager);
        bundleManager.uninstallById(setOf(12345L));        
        EasyMock.expectLastCall();
        bundleManager.uninstallById(setOf(54321L));
        EasyMock.expectLastCall();
        
        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeature("f1", "0.1");
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
        verify(bundleManager);
    }
    
    @SuppressWarnings("unchecked")
    private BundleManager prepareBundleManagerForInstallUninstall(String bundleUri, String bundlename) throws Exception {
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle = createDummyBundle(12345L, bundlename, headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded(bundleUri, 0, null)).andReturn(new BundleInstallerResult(installedBundle, true));
        ignoreRefreshes(bundleManager);
        bundleManager.uninstallById(Collections.EMPTY_SET);        
        EasyMock.expectLastCall();
        bundleManager.uninstallById(setOf(12345L));
        EasyMock.expectLastCall();
        return bundleManager;
    }

    @Test
    public void testInstallFeatureWithDependantFeaturesAndVersionWithoutPreinstall() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><feature version='0.1'>f2</feature></feature>"
                + "  <feature name='f2' version='0.1'><bundle>bundle-0.1</bundle></feature>"
                + "  <feature name='f2' version='0.2'><bundle>bundle-0.2</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = prepareBundleManagerForInstallUninstall("bundle-0.1", "bundle-0.1");

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeature("f1", "0.1");
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
        verify(bundleManager);
    }

    @Test
    public void testInstallFeatureWithDependantFeaturesAndNoVersionWithoutPreinstall() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><feature>f2</feature></feature>"
                + "  <feature name='f2' version='0.1'><bundle>bundle-0.1</bundle></feature>"
                + "  <feature name='f2' version='0.2'><bundle>bundle-0.2</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = prepareBundleManagerForInstallUninstall("bundle-0.2", "bundle-0.2");

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeature("f1", "0.1");
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.2");
        verify(bundleManager);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInstallFeatureWithDependantFeaturesAndRangeWithoutPreinstall() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><feature version='[0.1,0.3)'>f2</feature></feature>"
                + "  <feature name='f2' version='0.1'><bundle>bundle-0.1</bundle></feature>"
                + "  <feature name='f2' version='0.2'><bundle>bundle-0.2</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle bundleVer02 = createDummyBundle(54321L, "bundleVer02", headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded("bundle-0.2", 0, null)).andReturn(new BundleInstallerResult(bundleVer02, true));
        ignoreRefreshes(bundleManager);
        bundleManager.uninstallById(Collections.EMPTY_SET);        
        EasyMock.expectLastCall();
        bundleManager.uninstallById(setOf(54321L));
        EasyMock.expectLastCall();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeature("f1", "0.1");
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.2");
        verify(bundleManager);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInstallFeatureWithDependantFeaturesAndRangeWithPreinstall() throws Exception {
        String bundleVer01Uri = "bundle-0.1";
        String bundleVer02Uri = "bundle-0.2";

        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "<feature name='f1' version='0.1'><feature version='[0.1,0.3)'>f2</feature></feature>"
                + "  <feature name='f2' version='0.1'><bundle>%s</bundle></feature>"
                + "  <feature name='f2' version='0.2'><bundle>%s</bundle></feature>"
                + "</features>", bundleVer01Uri, bundleVer02Uri);
        
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle bundleVer01 = createDummyBundle(12345L, "bundleVer01", headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded(bundleVer01Uri, 0, null)).andReturn(new BundleInstallerResult(bundleVer01, true));
        expect(bundleManager.installBundleIfNeeded(bundleVer01Uri, 0, null)).andReturn(new BundleInstallerResult(bundleVer01, false));
        ignoreRefreshes(bundleManager);
        bundleManager.uninstallById(Collections.EMPTY_SET);        
        EasyMock.expectLastCall();
        bundleManager.uninstallById(setOf(12345L));
        EasyMock.expectLastCall();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeature("f2", "0.1");
        svc.installFeature("f1", "0.1");
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
        verify(bundleManager);
    }

    @Test
    public void testGetFeaturesShouldHandleDifferentVersionPatterns() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><feature version='[0.1,0.3)'>f2</feature></feature>"
                + "  <feature name='f2' version='0.1'><bundle>bundle1</bundle></feature>"
                + "  <feature name='f2' version='0.2'><bundle>bundle2</bundle></feature>"
                + "</features>");
        
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        
        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        verify(bundleManager);

        assertEquals(feature("f2", "0.2"), svc.getFeature("f2", "[0.1,0.3)"));
        assertEquals(feature("f2", "0.2"), svc.getFeature("f2", "0.0.0"));
        assertEquals(feature("f2", "0.2"), svc.getFeature("f2", "0.2"));
        assertNull(svc.getFeature("f2", "0.3"));
    }

    @Test
    public void testInstallBatchFeatureWithContinueOnFailureNoClean() throws Exception {
        String bundle1Uri = "bundle1";
        String bundle2Uri = "bundle2";

        URI uri = createTempRepo(FEATURE_WITH_INVALID_BUNDLE, bundle1Uri, bundle2Uri);
        
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle1 = createDummyBundle(12345L, "bundle1", headers());
        Bundle installedBundle2 = createDummyBundle(54321L, "bundle2", headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded(bundle1Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle1, true));
        expect(bundleManager.installBundleIfNeeded(bundle2Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle2, true));
        expect(bundleManager.installBundleIfNeeded("zfs:unknown", 0, null)).andThrow(new MalformedURLException());
        EasyMock.expectLastCall();
        ignoreRefreshes(bundleManager);

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure, FeaturesService.Option.NoCleanIfFailure));
        verify(bundleManager);
    }
    
    @Test
    public void testInstallBatchFeatureWithContinueOnFailureClean() throws Exception {
        String bundle1Uri = "file:bundle1";
        String bundle2Uri = "file:bundle2";

        URI uri = createTempRepo(FEATURE_WITH_INVALID_BUNDLE, bundle1Uri, bundle2Uri);

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle1 = createDummyBundle(12345L, "bundle1", headers());
        Bundle installedBundle2 = createDummyBundle(54321L, "bundle2", headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded(bundle1Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle1, true));
        expect(bundleManager.installBundleIfNeeded(bundle2Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle2, true));
        expect(bundleManager.installBundleIfNeeded("zfs:unknown", 0, null)).andThrow(new MalformedURLException());
        bundleManager.uninstall(setOf(installedBundle1));
        EasyMock.expectLastCall();
        ignoreRefreshes(bundleManager);

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure));
        verify(bundleManager);
    }

    @Test
    public void testInstallBatchFeatureWithoutContinueOnFailureNoClean() throws Exception {
        String bundle1Uri = "file:bundle1";
        String bundle2Uri = "file:bundle2";

        URI uri = createTempRepo(FEATURE_WITH_INVALID_BUNDLE, bundle1Uri, bundle2Uri);

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle1 = createDummyBundle(12345L, bundle1Uri, headers());
        Bundle installedBundle2 = createDummyBundle(54321L, bundle2Uri, headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded(bundle1Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle1, true));
        expect(bundleManager.installBundleIfNeeded(bundle2Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle2, true));
        expect(bundleManager.installBundleIfNeeded("zfs:unknown", 0, null)).andThrow(new MalformedURLException());
        
        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        try {
            List<Feature> features = Arrays.asList(svc.listFeatures());
            Collections.reverse(features);
            svc.installFeatures(new CopyOnWriteArraySet<Feature>(features),
                                EnumSet.of(FeaturesService.Option.NoCleanIfFailure));
            fail("Call should have thrown an exception");
        } catch (MalformedURLException e) {
        }
        verify(bundleManager);
    }

    @Test
    public void testInstallBatchFeatureWithoutContinueOnFailureClean() throws Exception {
        String bundle1Uri = "file:bundle1";
        String bundle2Uri = "file:bundle2";

        URI uri = createTempRepo(FEATURE_WITH_INVALID_BUNDLE, bundle1Uri, bundle2Uri);
        
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle1 = createDummyBundle(12345L, "bundle1", headers());
        Bundle installedBundle2 = createDummyBundle(54321L, "bundle2", headers());
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(bundleManager.installBundleIfNeeded(bundle1Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle1, true));
        expect(bundleManager.installBundleIfNeeded(bundle2Uri, 0, null)).andReturn(new BundleInstallerResult(installedBundle2, true));
        expect(bundleManager.installBundleIfNeeded("zfs:unknown", 0, null)).andThrow(new MalformedURLException());
        bundleManager.uninstall(setOf(installedBundle1, installedBundle2));
        EasyMock.expectLastCall();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        try {
            List<Feature> features = Arrays.asList(svc.listFeatures());
            Collections.reverse(features);
            svc.installFeatures(new CopyOnWriteArraySet<Feature>(features),
                                EnumSet.noneOf(FeaturesService.Option.class));
            fail("Call should have thrown an exception");
        } catch (MalformedURLException e) {
        }
        verify(bundleManager);
    }

    /**
     * This test checks schema validation of submited uri.
     */
    @Test
    public void testSchemaValidation() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <featur><bundle>somebundle</bundle></featur></features>");

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        try {
            svc.addRepository(uri);
            fail("exception expected");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unable to validate"));
        }
        verify(bundleManager);
    }

    /**
     * This test checks feature service behavior with old, non namespaced descriptor.
     */
    @Test
    public void testLoadOldFeatureFile() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1'><bundle>file:bundle1</bundle><bundle>file:bundle2</bundle></feature>"
                + "</features>");

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        svc.addRepository(uri);
        Feature feature = svc.getFeature("f1");
        Assert.assertNotNull("No feature named fi found", feature);        
        List<BundleInfo> bundles = feature.getBundles();
        Assert.assertEquals(2, bundles.size());
    }

}
