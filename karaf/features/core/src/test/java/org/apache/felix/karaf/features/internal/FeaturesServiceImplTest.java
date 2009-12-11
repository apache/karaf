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
package org.apache.felix.karaf.features.internal;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.felix.karaf.features.Feature;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;

import static org.easymock.EasyMock.*;

/**
 * Test cases for {@link FeaturesServiceImpl}
 */
public class FeaturesServiceImplTest extends TestCase {
    
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

    public void testStartDoesNotFailWithOneInvalidUri() throws BackingStoreException {
        PreferencesService preferencesService = createNiceMock(PreferencesService.class);
        Preferences prefs = createNiceMock(Preferences.class);
        Preferences emptyPrefs = createNiceMock(Preferences.class);
        expect(preferencesService.getUserPreferences("FeaturesServiceState")).andStubReturn(prefs);
        replay(preferencesService);

        expect(prefs.node("repositories")).andReturn(emptyPrefs);
        expect(prefs.node("features")).andReturn(emptyPrefs);
        replay(prefs);

        FeaturesServiceImpl service = new FeaturesServiceImpl();
        service.setPreferences(preferencesService);

        try {
            service.setUrls("mvn:inexistent/features/1.0/xml/features");
            service.start();
        } catch (Exception e) {
            fail(String.format("Service should not throw start-up exception but log the error instead: %s", e));
        }
    }
}
