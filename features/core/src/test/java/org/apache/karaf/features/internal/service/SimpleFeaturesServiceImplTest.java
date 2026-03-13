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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test cases for {@link SimpleFeaturesServiceImpl}, including shared tests
 * inherited from {@link AbstractFeaturesServiceTest}.
 */
public class SimpleFeaturesServiceImplTest extends AbstractFeaturesServiceTest {

    @Override
    protected FeaturesService createServiceWithFeatures(Feature... staticFeatures) throws Exception {
        final Map<String, Map<String, Feature>> features = features(staticFeatures);
        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        EasyMock.replay(installSupport);
        return new SimpleFeaturesServiceImpl(new Storage(), null, null, installSupport, cfg) {
            @Override
            protected Map<String, Map<String, Feature>> getFeatureCache() throws Exception {
                return features;
            }
        };
    }

    @Override
    protected FeaturesService createServiceForRepoTests() throws Exception {
        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        EasyMock.replay(installSupport);
        return new SimpleFeaturesServiceImpl(new Storage(), null, null, installSupport, cfg);
    }

    @Test
    public void testBootManagedInterface() throws Exception {
        FeaturesServiceConfig cfg = new FeaturesServiceConfig();
        BundleInstallSupport installSupport = EasyMock.niceMock(BundleInstallSupport.class);
        EasyMock.replay(installSupport);
        SimpleFeaturesServiceImpl svc = new SimpleFeaturesServiceImpl(new Storage(), null, null, installSupport, cfg);
        assertNotNull(svc);
        assertTrue(svc instanceof BootManaged);
    }
}
