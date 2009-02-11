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
package org.apache.servicemix.kernel.gshell.features;

import java.net.URI;
import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import static org.easymock.EasyMock.*;

import org.apache.servicemix.kernel.gshell.features.internal.FeatureImpl;
import org.apache.servicemix.kernel.gshell.features.internal.FeaturesServiceImpl;
import org.apache.servicemix.kernel.gshell.features.FeaturesRegistry;
import org.apache.servicemix.kernel.gshell.features.management.ManagedFeaturesRegistry;
import org.easymock.EasyMock;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.prefs.Preferences;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.springframework.context.ApplicationContext;
import junit.framework.TestCase;

public class FeaturesServiceTest extends TestCase {

    public void testInstallFeature() throws Exception {

        String name = ApplicationContext.class.getName();
        name = name.replace(".", "/")  + ".class";
        name = getClass().getClassLoader().getResource(name).toString();
        name = name.substring("jar:".length(), name.indexOf('!'));

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
        FeaturesRegistry featuresRegistry = EasyMock.createNiceMock(FeaturesRegistry.class);

        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        expect(prefs.node("repositories")).andReturn(repositoriesNode);
        repositoriesNode.clear();
        repositoriesNode.putInt("count", 1);
        repositoriesNode.put("item.0", uri.toString());
        expect(prefs.node("features")).andReturn(featuresNode);
        featuresNode.clear();
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();
        featuresRegistry.register(isA(Repository.class));

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle, featuresRegistry);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setPreferences(preferencesService);
        svc.setBundleContext(bundleContext);
        svc.setFeaturesServiceRegistry(featuresRegistry);
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

        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle, featuresRegistry);

        reset(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle, featuresRegistry);

        expect(bundleContext.getBundles()).andReturn(new Bundle[0]);
        expect(bundleContext.installBundle(isA(String.class),
                                           isA(InputStream.class))).andReturn(installedBundle);
        installedBundle.start();
        expect(installedBundle.getBundleId()).andReturn(12345L);

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

        svc.installFeature("f1");
    }

    public void testUninstallFeature() throws Exception {
        File tmp = File.createTempFile("smx", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.println("<features>");
        pw.println("  <feature name=\"f1\" version=\"0.1\">");
        pw.println("  </feature>");
        pw.println("  <feature name=\"f1\" version=\"0.2\">");
        pw.println("  </feature>");
        pw.println("</features>");
        pw.close();

        URI uri = tmp.toURI();

        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle installedBundle = EasyMock.createMock(Bundle.class);

        FeaturesServiceImpl svc = new FeaturesServiceImpl();
        svc.setBundleContext(bundleContext);
        svc.setFeaturesServiceRegistry(new ManagedFeaturesRegistry());
        svc.addRepository(uri);

        try {
            svc.uninstallFeature("f1");
            fail("Uninstall should have failed as feature is not installed");
        } catch (Exception e) {
            // ok
        }

        svc.installFeature("f1", "0.1");
        svc.installFeature("f1", "0.2");

        try {
            svc.uninstallFeature("f1");
            fail("Uninstall should have failed as feature is installed in multiple versions");
        } catch (Exception e) {
            // ok
        }

        svc.uninstallFeature("f1", "0.1");
        svc.uninstallFeature("f1");
    }
}
