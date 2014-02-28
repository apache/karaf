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

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.jar.JarInputStream;

import junit.framework.TestCase;

import org.apache.karaf.features.internal.FeatureImpl;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;

import static org.easymock.EasyMock.*;

public class FeaturesServiceTest extends TestCase {

    File dataFile;

    protected void setUp() throws IOException {
        dataFile = File.createTempFile("features", null, null);
    }

    public void testInstallFeature() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);
        
        Repository[] repositories = svc.listRepositories();
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

        verify(bundleContext, installedBundle);

        reset(bundleContext, installedBundle);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle);

        svc.installFeature("f1", FeatureImpl.DEFAULT_VERSION, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        
        Feature[] installed = svc.listInstalledFeatures();
        assertEquals(1, installed.length);
        assertEquals("f1", installed[0].getName());
    }

    public void testUninstallFeature() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f1\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();
        
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);
        PackageAdmin packageAdmin = EasyMock.createMock(PackageAdmin.class);

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);
        svc.setPackageAdmin(packageAdmin);
        
        verify(bundleContext, installedBundle);

        reset(bundleContext, installedBundle);

        // Installs f1 and 0.1
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        // Installs f1 and 0.2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(123456L);
        expect(installedBundle.getBundleId()).andReturn(123456L);
        expect(installedBundle.getBundleId()).andReturn(123456L);
        expect(bundleContext.getBundle(123456L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        // UnInstalls f1 and 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        // UnInstalls f1 and 0.2
        expect(bundleContext.getBundle(123456)).andReturn(installedBundle);
        packageAdmin.refreshPackages(null);
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle, packageAdmin);

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

        svc.uninstallFeature("f1", "0.1", EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        svc.uninstallFeature("f1");
    }    
    
    // Tests Add and Remove Repository
    public void testAddAndRemoveRepository() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);

        // Adds Repository
        svc.addRepository(uri);                                                     
        
        // Removes Repository
        svc.removeRepository(uri);        
    }

    // Tests install of a Repository that includes a feature
    // with a feature dependency
    // The dependant feature is in the same repository
    // Tests uninstall of features
    public void testInstallFeatureWithDependantFeatures() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        // Installs feature f1 with dependency on f2
        // so will install f2 first
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        // Then installs f1
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(bundleContext.getBundle(1234L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable()).anyTimes();
        installedBundle.start();

        // uninstalls first feature name = f1, version = 0.1
        expect(bundleContext.getBundle(1234)).andReturn(installedBundle);
        installedBundle.uninstall();

        // uninstalls first feature name = f2, version = 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");

    }

    // Tests install of a Repository that includes a feature with a feature dependency
    public void testInstallFeatureWithDependantFeaturesAndVersionWithoutPreinstall() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
    }

    // Tests install of a Repository that includes a feature with a feature dependency
    public void testInstallFeatureWithDependantFeaturesAndNoVersionWithoutPreinstall() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.2");
    }

    public void testInstallFeatureWithDependantFeaturesAndRangeWithoutPreinstall() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.2");
    }

    public void testInstallFeatureWithDependantFeaturesAndRangeWithPreinstall() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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
        Bundle installedBundle = EasyMock.createMock(Bundle.class);

        // Installs feature f1 with dependency on f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L).anyTimes();
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle).anyTimes();
        expect(installedBundle.getHeaders()).andReturn(new Hashtable()).anyTimes();
        installedBundle.start();

        expect(bundleContext.getBundles()).andReturn(new Bundle[] { installedBundle });
        expect(installedBundle.getSymbolicName()).andReturn(name).anyTimes();
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        installedBundle.start();

        // uninstalls first feature name = f2, version = 0.1
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeature("f2", "0.1");
        svc.installFeature("f1", "0.1");

        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
    }

    public void testGetFeaturesShouldHandleDifferentVersionPatterns() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
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

        // Installs feature f1 with dependency on f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        // uninstalls first feature name = f2, version = 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle);
        return bundleContext;
    }

    public void testInstallBatchFeatureWithContinueOnFailureNoClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(LogService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        // Installs feature f1 and f2
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
        expect(installedBundle2.getHeaders()).andReturn(new Hashtable()).anyTimes();
        installedBundle2.start();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure, FeaturesService.Option.NoCleanIfFailure));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    public void testInstallBatchFeatureWithContinueOnFailureClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(LogService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        // Installs feature f1 and f2
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
        expect(installedBundle2.getHeaders()).andReturn(new Hashtable()).anyTimes();
        installedBundle2.start();

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    public void testInstallBatchFeatureWithoutContinueOnFailureNoClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(LogService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        // Installs feature f1 and f2
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

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
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

    public void testInstallBatchFeatureWithoutContinueOnFailureClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(LogService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
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

        // Installs feature f1 and f2
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

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
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

    public void testInstallFeatureWithHostToRefresh() throws Exception {
        String bundle1 = getJarUrl(LogService.class);
        String bundle2 = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
        pw.println("  <feature name='f1'>");
        pw.println("    <bundle>" + bundle1 + "</bundle>");
        pw.println("    <bundle>" + bundle2 + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        JarInputStream j = new JarInputStream(new URL(bundle1).openStream());
        Dictionary<String,String> headers = new Hashtable();
        for (Map.Entry e : j.getManifest().getMainAttributes().entrySet()) {
            headers.put(e.getKey().toString(), e.getValue().toString());
        }

        // loads the state
        PackageAdmin packageAdmin = EasyMock.createMock(PackageAdmin.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // Installs feature f1
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        expect(installedBundle1.getSymbolicName()).andReturn(headers.get(Constants.BUNDLE_SYMBOLICNAME)).anyTimes();
        expect(installedBundle1.getHeaders()).andReturn(headers).anyTimes();
        expect(bundleContext.getBundles()).andReturn(new Bundle[] { installedBundle1 });

        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(bundleContext.getBundles()).andReturn(new Bundle[] { installedBundle1, installedBundle2 });
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getSymbolicName()).andReturn("fragment").anyTimes();
        Dictionary d = new Hashtable();
        d.put(Constants.FRAGMENT_HOST, headers.get(Constants.BUNDLE_SYMBOLICNAME));
        expect(installedBundle2.getHeaders()).andReturn(d).anyTimes();

        expect(installedBundle1.getState()).andReturn(Bundle.ACTIVE);
        expect(installedBundle1.getState()).andReturn(Bundle.ACTIVE);
        expect(installedBundle2.getState()).andReturn(Bundle.INSTALLED);
        expect(installedBundle2.getState()).andReturn(Bundle.INSTALLED);

        //
        // This is the real test to make sure the host is actually refreshed
        //
        packageAdmin.refreshPackages(aryEq(new Bundle[] { installedBundle1 }));

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        replay(packageAdmin, bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPackageAdmin(packageAdmin);
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        List<Feature> features = Arrays.asList(svc.listFeatures());
        Collections.reverse(features);
        svc.installFeatures(new CopyOnWriteArraySet<Feature>(features),
                            EnumSet.noneOf(FeaturesService.Option.class));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    /**
     * This test checks schema validation of submited uri.
     */
    public void testSchemaValidation() throws Exception {
        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features xmlns=\"http://karaf.apache.org/xmlns/features/v1.0.0\">");
        pw.println("  <featur>");
        pw.println("    <bundle>somebundle</bundle>");
        pw.println("  </featur>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);

        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
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
    public void testNoSchemaValidation() throws Exception {
        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
        pw.println("  <featur>");
        pw.println("    <bundle>anotherBundle</bundle>");
        pw.println("  </featur>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        expect(bundleContext.getDataFile(EasyMock.<String>anyObject())).andReturn(dataFile).anyTimes();
        replay(bundleContext);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);
    }

    private String getJarUrl(Class cl) {
        String name = cl.getName();
        name = name.replace(".", "/")  + ".class";
        name = getClass().getClassLoader().getResource(name).toString();
        name = name.substring("jar:".length(), name.indexOf('!'));
        return name;
    }

}
