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
import static org.easymock.EasyMock.isA;
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
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.karaf.features.internal.BundleManager;
import org.apache.karaf.features.internal.BundleManager.BundleInstallerResult;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.karaf.features.internal.TestBase;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.LoggerFactory;

public class FeaturesServiceTest extends TestBase {

    File dataFile;

    protected void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }

    @Test
    public void testInstallFeature() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\">");
        pw.println("    <bundle start='true'>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        Bundle installedBundle = createDummyBundle(12345L, name, headers());
        FeaturesServiceImpl svc = testAddRepository(name, tmp, uri, bundleManager, installedBundle);
        
        reset(bundleManager);
        
        expect(bundleManager.installBundleIfNeeded(eq(name), eq(0), eq((String)null))).andReturn(new BundleInstallerResult(installedBundle, true));
        TreeSet<Bundle> existing = new TreeSet<Bundle>(Arrays.asList(installedBundle));
        bundleManager.refreshBundles(eq(existing), eq(existing), eq(EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles)));
        EasyMock.expectLastCall();
        expect(bundleManager.getDataFile(EasyMock.anyObject(String.class))).andReturn(tmp);
        
        replay(bundleManager);
        svc.installFeature("f1", org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        verify(bundleManager);
        
        Feature[] installed = svc.listInstalledFeatures();
        assertEquals(1, installed.length);
        assertEquals("f1", installed[0].getName());
    }

    private FeaturesServiceImpl testAddRepository(String name, File tmp, URI uri, BundleManager bundleManager,
            Bundle installedBundle) throws IOException, BundleException, Exception {
        expect(bundleManager.getDataFile(EasyMock.anyObject(String.class))).andReturn(tmp);
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
    public void testUninstallFeature() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f1\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();
        
        Bundle installedBundle = createDummyBundle(12345L, name, headers());

        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.anyObject(String.class))).andReturn(tmp).anyTimes();
        expect(bundleManager.installBundleIfNeeded(name, 0, null)).andReturn(new BundleInstallerResult(installedBundle, true));
        expect(bundleManager.installBundleIfNeeded(name, 0, null)).andReturn(new BundleInstallerResult(installedBundle, false));
        bundleManager.refreshBundles(EasyMock.anyObject(Set.class), EasyMock.anyObject(Set.class), EasyMock.anyObject(EnumSet.class));
        EasyMock.expectLastCall().anyTimes();
        bundleManager.uninstallBundles(EasyMock.anyObject(Set.class));
        EasyMock.expectLastCall().times(2);
        
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
    }    
    
    // Tests Add and Remove Repository
    @Test
    public void testAddAndRemoveRepository() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f1\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        // loads the state
        BundleManager bundleManager = EasyMock.createMock(BundleManager.class);
        expect(bundleManager.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleManager);
        FeaturesServiceImpl svc = new FeaturesServiceImpl(bundleManager);
        EasyMock.verify(bundleManager);

        svc.addRepository(uri);                                                     
        svc.removeRepository(uri);
    }

    // Tests install of a Repository that includes a feature
    // with a feature dependency
    // The dependant feature is in the same repository
    // Tests uninstall of features
    @Test
    public void testInstallFeatureWithDependantFeatures() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("  <feature version=\"0.1\">f2</feature>");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);
        Bundle framework = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 with dependency on f2
        // so will install f2 first
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable<String, String>());
        installedBundle.start();

        // Then installs f1
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(installedBundle.getSymbolicName()).andReturn(name).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(bundleContext.getBundle(1234L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        installedBundle.start();

        // uninstalls first feature name = f1, version = 0.1
        expect(bundleContext.getBundle(1234)).andReturn(installedBundle);
        installedBundle.uninstall();

        // uninstalls first feature name = f2, version = 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        expect(bundleContext.getBundle()).andReturn(framework).anyTimes();
        expect(framework.adapt(FrameworkWiring.class)).andReturn(null).anyTimes();

        replay(bundleContext, installedBundle, framework);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");

    }

    // Tests install of a Repository that includes a feature with a feature dependency
    @Test
    public void testInstallFeatureWithDependantFeaturesAndVersionWithoutPreinstall() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <feature version=\"0.1\">f2</feature>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = prepareBundleContextForInstallUninstall();

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
    }

    // Tests install of a Repository that includes a feature with a feature dependency
    @Test
    public void testInstallFeatureWithDependantFeaturesAndNoVersionWithoutPreinstall() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <feature>f2</feature>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = prepareBundleContextForInstallUninstall();

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.2");
    }

    @Test
    public void testInstallFeatureWithDependantFeaturesAndRangeWithoutPreinstall() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <feature version=\"[0.1,0.3)\">f2</feature>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = prepareBundleContextForInstallUninstall();

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.2");
    }

    @Test
    public void testInstallFeatureWithDependantFeaturesAndRangeWithPreinstall() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <feature version=\"[0.1,0.3)\">f2</feature>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle framework = EasyMock.createMock(Bundle.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 with dependency on f2
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L).anyTimes();
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle).anyTimes();
        expect(installedBundle.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        installedBundle.start();

        expect(bundleContext.getBundles()).andReturn(new Bundle[] { installedBundle });
        expect(installedBundle.getSymbolicName()).andReturn(name).anyTimes();
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        installedBundle.start();

        // uninstalls first feature name = f2, version = 0.1
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        expect(bundleContext.getBundle()).andReturn(framework).anyTimes();
        expect(framework.adapt(FrameworkWiring.class)).andReturn(null).anyTimes();

        replay(bundleContext, installedBundle, framework);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeature("f2", "0.1");
        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
    }

    @Test
    public void testGetFeaturesShouldHandleDifferentVersionPatterns() throws Exception {

        String name = getJarUrl(BlueprintContainer.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <feature version=\"[0.1,0.3)\">f2</feature>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f2\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();
        
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        
        bundleContext.getDataFile((String) EasyMock.anyObject());
        EasyMock.expectLastCall().andReturn(File.createTempFile("test", "test")); 
        EasyMock.replay(bundleContext);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        Feature feature = svc.getFeature("f2", "[0.1,0.3)");
        assertEquals("f2", feature.getName());
        assertEquals("0.2", feature.getVersion());

        Feature feature2 = svc.getFeature("f2", "0.0.0");
        assertEquals("f2", feature2.getName());
        assertEquals("0.2", feature2.getVersion());

        Feature feature3 = svc.getFeature("f2", "0.2");
        assertEquals("f2", feature3.getName());
        assertEquals("0.2", feature3.getVersion());

        Feature feature4 = svc.getFeature("f2", "0.3");
        assertNull(feature4);
    }

    private BundleContext prepareBundleContextForInstallUninstall() throws Exception {
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);
        Bundle framework = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 with dependency on f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable<String, String>());
        installedBundle.start();

        // uninstalls first feature name = f2, version = 0.1
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(installedBundle.getSymbolicName()).andReturn("mybundle").anyTimes();
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        expect(bundleContext.getBundle()).andReturn(framework).anyTimes();
        expect(framework.adapt(FrameworkWiring.class)).andReturn(null).anyTimes();

        replay(bundleContext, installedBundle, framework);
        return bundleContext;
    }

    @Test
    public void testInstallBatchFeatureWithContinueOnFailureNoClean() throws Exception {
        String bundle1 = getJarUrl(BlueprintContainer.class);
        String bundle2 = getJarUrl(LoggerFactory.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name='f1'>");
        pw.println("    <bundle>" + bundle1 + "</bundle>");
        pw.println("    <bundle>" + "zfs:unknown" + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name='f2'>");
        pw.println("    <bundle>" + bundle2 + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle1.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();
        expect(installedBundle2.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 and f2
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        installedBundle2.start();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        expect(installedBundle1.getSymbolicName()).andReturn("bundle1").anyTimes();
        expect(installedBundle2.getSymbolicName()).andReturn("bundle2").anyTimes();

        replay(bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure, FeaturesService.Option.NoCleanIfFailure));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    @Test
    public void testInstallBatchFeatureWithContinueOnFailureClean() throws Exception {
        String bundle1 = getJarUrl(BlueprintContainer.class);
        String bundle2 = getJarUrl(LoggerFactory.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name='f1'>");
        pw.println("    <bundle>" + bundle1 + "</bundle>");
        pw.println("    <bundle>" + "zfs:unknown" + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name='f2'>");
        pw.println("    <bundle>" + bundle2 + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle1.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();
        expect(installedBundle2.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 and f2
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getSymbolicName()).andReturn("bundle1").anyTimes();
        installedBundle1.uninstall();

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getHeaders()).andReturn(new Hashtable<String, String>()).anyTimes();
        expect(installedBundle2.getSymbolicName()).andReturn("bundle2").anyTimes();
        installedBundle2.start();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    @Test
    public void testInstallBatchFeatureWithoutContinueOnFailureNoClean() throws Exception {
        String bundle1 = getJarUrl(BlueprintContainer.class);
        String bundle2 = getJarUrl(LoggerFactory.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name='f1'>");
        pw.println("    <bundle>" + bundle1 + "</bundle>");
        pw.println("    <bundle>" + "zfs:unknown" + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name='f2'>");
        pw.println("    <bundle>" + bundle2 + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle1.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();
        expect(installedBundle2.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 and f2
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        installedBundle2.start();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        try {
            List<Feature> features = Arrays.asList(svc.listFeatures());
            Collections.reverse(features);
            svc.installFeatures(new CopyOnWriteArraySet<Feature>(features),
                                EnumSet.of(FeaturesService.Option.NoCleanIfFailure));
            fail("Call should have thrown an exception");
        } catch (MalformedURLException e) {
        }

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    @Test
    public void testInstallBatchFeatureWithoutContinueOnFailureClean() throws Exception {
        String bundle1 = getJarUrl(BlueprintContainer.class);
        String bundle2 = getJarUrl(LoggerFactory.class);

        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name='f1'>");
        pw.println("    <bundle>" + bundle1 + "</bundle>");
        pw.println("    <bundle>" + "zfs:unknown" + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name='f2'>");
        pw.println("    <bundle>" + bundle2 + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // required since the sorted set uses it
        expect(installedBundle1.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();
        expect(installedBundle2.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();

        // Installs feature f1 and f2
        expect(bundleContext.createFilter(EasyMock.<String>anyObject())).andReturn(null).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        installedBundle1.uninstall();

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        installedBundle2.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);

        try {
            List<Feature> features = Arrays.asList(svc.listFeatures());
            Collections.reverse(features);
            svc.installFeatures(new CopyOnWriteArraySet<Feature>(features),
                                EnumSet.noneOf(FeaturesService.Option.class));
            fail("Call should have thrown an exception");
        } catch (MalformedURLException e) {
        }

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    /**
     * This test checks schema validation of submited uri.
     */
    @Test
    public void testSchemaValidation() throws Exception {
        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <featur>");
        pw.println("    <bundle>somebundle</bundle>");
        pw.println("  </featur>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle bundle = EasyMock.createMock(Bundle.class);
        // required since the sorted set uses it
        expect(bundle.compareTo(EasyMock.<Bundle>anyObject())).andReturn(0).anyTimes();
        expect(bundleContext.getBundle()).andReturn(bundle);
        expect(bundle.adapt(FrameworkWiring.class)).andReturn(null);
        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        try {
            svc.addRepository(uri);
            fail("exception expected");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unable to validate"));
        }
    }

    /**
     * This test checks feature service behavior with old, non namespaced descriptor.
     */
    @Test
    public void testLoadOldFeatureFile() throws Exception {
        String bundle1 = getJarUrl(BlueprintContainer.class);
        String bundle2 = getJarUrl(LoggerFactory.class);
        
        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features name=\"test\" xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name='f1'>");
        pw.println("    <bundle>" + bundle1 + "</bundle>");
        pw.println("    <bundle>" + bundle2 + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        replay(bundleContext);

        FeaturesServiceImpl svc = new FeaturesServiceImpl(new BundleManager(bundleContext));
        svc.addRepository(uri);
        Feature feature = svc.getFeature("f1");
        Assert.assertNotNull("No feature named fi found", feature);        
        List<BundleInfo> bundles = feature.getBundles();
        Assert.assertEquals(2, bundles.size());
    }

    private String getJarUrl(Class<?> cl) {
        String name = cl.getName();
        name = name.replace(".", "/")  + ".class";
        name = getClass().getClassLoader().getResource(name).toString();
        name = name.substring("jar:".length(), name.indexOf('!'));
        return name;
    }

}
