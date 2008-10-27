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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.apache.servicemix.kernel.gshell.features.internal.FeaturesServiceImpl;
import org.easymock.EasyMock;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.prefs.Preferences;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

public class FeaturesServiceTest {

    @Test
    public void testInstallFeature() throws Exception {
        URI uri = getClass().getResource("repo2.xml").toURI();

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
        assertEquals("http://repo1.maven.org/maven2/org/apache/geronimo/specs/geronimo-stax-api_1.0_spec/1.0.1/geronimo-stax-api_1.0_spec-1.0.1.jar", features[0].getBundles().get(0));

        verify(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        reset(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

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
        featuresNode.put("f1", "12345");
        prefs.putBoolean("bootFeaturesInstalled", false);
        prefs.flush();

        replay(preferencesService, prefs, repositoriesNode, featuresNode, bundleContext, installedBundle);

        svc.installFeature("f1");
    }
}
