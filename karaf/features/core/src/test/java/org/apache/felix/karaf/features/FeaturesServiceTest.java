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
package org.apache.felix.karaf.features;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import junit.framework.TestCase;
import org.apache.felix.karaf.features.internal.FeaturesServiceImpl;
import org.apache.felix.karaf.features.internal.FeatureImpl;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

public class FeaturesServiceTest extends TestCase {

    public void testInstallFeature() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
        pw.println("  <feature name=\"f1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
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
        assertEquals(name, features[0].getBundles().get(0));

        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        reset(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + FeatureImpl.DEFAULT_VERSION, "12345");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        svc.installFeature("f1", FeatureImpl.DEFAULT_VERSION, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
        
        Feature[] installed = svc.listInstalledFeatures();
        assertEquals(1, installed.length);
        assertEquals("f1", installed[0].getName());
    }

    public void testUninstallFeature() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f1\" version=\"0.2\">");
        pw.println("    <bundle>" + name + "</bundle>");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();
        
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);
        
        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        reset(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        // Installs f1 and 0.1
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // Installs f1 and 0.2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(123456L);
        expect(bundleContext.getBundle(123456L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // UnInstalls f1 and 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // UnInstalls f1 and 0.2
        expect(bundleContext.getBundle(123456)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

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
    public void testAddAndRemoveRepository() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);        
        Bundle installedBundle = EasyMock.createMock(Bundle.class);        

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();               
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // SaveState for addRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // SaveState for removeRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 0);              
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();        
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);        

        // Adds Repository
        svc.addRepository(uri);                                                     
        
        // Removes Repository
        svc.removeRepository(uri);        
    }

    // Tests installing all features in a repo and uninstalling
    // all features in a repo
    public void testInstallUninstallAllFeatures() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);        
        Bundle installedBundle = EasyMock.createMock(Bundle.class);        

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // Installs first feature name = f1, version = 0.1 
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());

        installedBundle.start();
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // Installs second feature name = f1, version = 0.2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(123456L);
        expect(bundleContext.getBundle(123456L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // Installs third feature name = f2, version = 0.2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(1234567L);
        expect(bundleContext.getBundle(1234567L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "1234567");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 0);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "1234567");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        expect(installedBundle.getHeaders()).andReturn(new Hashtable()).anyTimes();

        // uninstallAllFeatures 
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "1234567");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // uninstalls first feature name = f1, version = 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "1234567");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // uninstalls third feature name = f2, version = 0.2
        expect(bundleContext.getBundle(1234567)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();        
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.2", "123456");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // uninstalls second feature name = f1, version = 0.2
        expect(bundleContext.getBundle(123456)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();        
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 0);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);        
        svc.installAllFeatures(uri);
        
        // Uninstalls features with versions.
        svc.uninstallAllFeatures(uri);    
    }    


    // Tests install of a Repository that includes a feature 
    // with a feature dependency  
    // The dependant feature is in the same repository 
    // Tests uninstall of features
    public void testInstallFeatureWithDependantFeatures() throws Exception {

        String name = getJarUrl(Bundle.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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

        // loads the state
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences repositoriesAvailableNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);        
        Bundle installedBundle = EasyMock.createMock(Bundle.class);        

        // savestate from addRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // Installs feature f1 with dependency on f2
        // so will install f2 first
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(12345L);
        expect(bundleContext.getBundle(12345L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        // Then installs f1
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        expect(installedBundle.getBundleId()).andReturn(1234L);
        expect(bundleContext.getBundle(1234L)).andReturn(installedBundle);
        expect(installedBundle.getHeaders()).andReturn(new Hashtable());
        installedBundle.start();
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");
        featuresNode.put("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "1234");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();                      

        // uninstalls first feature name = f1, version = 0.1
        expect(bundleContext.getBundle(1234)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.1", "12345");        
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
       // uninstalls first feature name = f2, version = 0.1
        expect(bundleContext.getBundle(12345)).andReturn(installedBundle);
        installedBundle.uninstall();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());        
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();        
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();                        
        
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 0);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        
        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);        
        svc.addRepository(uri);    

        svc.installFeature("f1", "0.1");
                
        // Uninstall repository
        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f2", "0.1");
        
    }

    public void testInstallBatchFeatureWithContinueOnFailureNoClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(PreferencesService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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

        // loads the state
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences repositoriesAvailableNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // savestate from addRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // Installs feature f1 and f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getHeaders()).andReturn(new Hashtable());
        installedBundle2.start();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.0.0", "54321");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure, FeaturesService.Option.NoCleanIfFailure));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    public void testInstallBatchFeatureWithContinueOnFailureClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(PreferencesService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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

        // loads the state
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences repositoriesAvailableNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // savestate from addRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // Installs feature f1 and f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        installedBundle1.uninstall();

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        expect(installedBundle2.getHeaders()).andReturn(new Hashtable());
        installedBundle2.start();

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        featuresNode.put("f2" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + "0.0.0", "54321");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);
        svc.addRepository(uri);

        svc.installFeatures(new CopyOnWriteArraySet<Feature>(Arrays.asList(svc.listFeatures())),
                            EnumSet.of(FeaturesService.Option.ContinueBatchOnFailure));

//        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);
    }

    public void testInstallBatchFeatureWithoutContinueOnFailureNoClean() throws Exception {
        String bundle1 = getJarUrl(Bundle.class);
        String bundle2 = getJarUrl(PreferencesService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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

        // loads the state
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences repositoriesAvailableNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // savestate from addRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // Installs feature f1 and f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        installedBundle2.start();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
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
        String bundle2 = getJarUrl(PreferencesService.class);

        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
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

        // loads the state
        Preferences prefs = EasyMock.createMock(Preferences.class);
        PreferencesService preferencesService = EasyMock.createMock(PreferencesService.class);
        Preferences repositoriesNode = EasyMock.createMock(Preferences.class);
        Preferences repositoriesAvailableNode = EasyMock.createMock(Preferences.class);
        Preferences featuresNode = EasyMock.createMock(Preferences.class);
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle1 = EasyMock.createMock(Bundle.class);
        Bundle installedBundle2 = EasyMock.createMock(Bundle.class);

        // savestate from addRepository
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        // Installs feature f1 and f2
        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle1), isA(InputStream.class))).andReturn(installedBundle1);
        expect(installedBundle1.getBundleId()).andReturn(12345L);
        installedBundle1.uninstall();

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(eq(bundle2), isA(InputStream.class))).andReturn(installedBundle2);
        expect(installedBundle2.getBundleId()).andReturn(54321L);
        installedBundle2.uninstall();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle1, installedBundle2);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
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

    private String getJarUrl(Class cl) {
        String name = cl.getName();
        name = name.replace(".", "/")  + ".class";
        name = getClass().getClassLoader().getResource(name).toString();
        name = name.substring("jar:".length(), name.indexOf('!'));
        return name;
    }

}
