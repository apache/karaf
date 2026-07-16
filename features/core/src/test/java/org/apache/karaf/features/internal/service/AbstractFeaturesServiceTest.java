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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.TestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Abstract test class with tests that work against the {@link FeaturesService} API,
 * shared between {@link FeaturesServiceImpl} and {@link SimpleFeaturesServiceImpl}.
 */
public abstract class AbstractFeaturesServiceTest extends TestBase {

    /**
     * Create a FeaturesService pre-populated with the given features in its cache.
     */
    protected abstract FeaturesService createServiceWithFeatures(Feature... features) throws Exception;

    /**
     * Create a FeaturesService that can load repositories from URIs.
     */
    protected abstract FeaturesService createServiceForRepoTests() throws Exception;

    protected URI createTempRepo(String repoContent, Object... variables) throws IOException {
        File tmp = File.createTempFile("karaf", ".feature");
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        pw.printf(repoContent, variables);
        pw.close();
        return tmp.toURI();
    }

    //
    // Feature query tests
    //

    @Test
    public void testListFeatureWithoutVersion() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesService svc = createServiceWithFeatures(transactionFeature);
        assertNotNull(svc.getFeatures("transaction", null));
        assertSame(transactionFeature, svc.getFeatures("transaction",
                org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0]);
    }

    @Test
    public void testGetFeature() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesService svc = createServiceWithFeatures(transactionFeature);
        assertNotNull(svc.getFeatures("transaction",
                org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertSame(transactionFeature, svc.getFeatures("transaction",
                org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0]);
    }

    @Test
    public void testGetFeatureStripVersion() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesService svc = createServiceWithFeatures(transactionFeature);
        Feature[] features = svc.getFeatures("transaction", "  1.0.0  ");
        assertEquals(1, features.length);
        Feature feature = features[0];
        assertNotNull(feature);
        assertSame("transaction", feature.getName());
    }

    @Test
    public void testGetFeatureNotAvailable() throws Exception {
        Feature transactionFeature = feature("transaction", "1.0.0");
        FeaturesService svc = createServiceWithFeatures(transactionFeature);
        assertEquals(0, svc.getFeatures("activemq",
                org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION).length);
    }

    @Test
    public void testGetFeatureHighestAvailable() throws Exception {
        FeaturesService svc = createServiceWithFeatures(
                feature("transaction", "1.0.0"),
                feature("transaction", "2.0.0"));
        assertNotNull(svc.getFeatures("transaction",
                org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
        assertEquals("2.0.0", svc.getFeatures("transaction",
                org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)[0].getVersion());
    }

    //
    // Repository tests
    //

    @Test
    public void testGetFeaturesShouldHandleDifferentVersionPatterns() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1' version='0.1'><feature version='[0.1,0.3)'>f2</feature></feature>"
                + "  <feature name='f2' version='0.1'><bundle>bundle1</bundle></feature>"
                + "  <feature name='f2' version='0.2'><bundle>bundle2</bundle></feature>"
                + "</features>");

        FeaturesService svc = createServiceForRepoTests();
        svc.addRepository(uri);

        assertEquals(feature("f2", "0.2"), svc.getFeatures("f2", "[0.1,0.3)")[0]);
        assertEquals(feature("f2", "0.2"), svc.getFeatures("f2", "0.0.0")[0]);
        assertEquals(feature("f2", "0.2"), svc.getFeatures("f2", "0.2")[0]);
        assertEquals(0, svc.getFeatures("f2", "0.3").length);
    }

    @Test
    public void testSchemaValidation() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <featur><bundle>somebundle</bundle></featur></features>");

        FeaturesService svc = createServiceForRepoTests();
        try {
            svc.addRepository(uri);
            org.junit.Assert.fail("exception expected");
        } catch (Exception e) {
            org.junit.Assert.assertTrue(e.getMessage().contains("Unable to validate"));
        }
    }

    @Test
    public void testLoadOldFeatureFile() throws Exception {
        URI uri = createTempRepo("<features name='test' xmlns='http://karaf.apache.org/xmlns/features/v1.0.0'>"
                + "  <feature name='f1'><bundle>file:bundle1</bundle><bundle>file:bundle2</bundle></feature>"
                + "</features>");

        FeaturesService svc = createServiceForRepoTests();
        svc.addRepository(uri);
        Feature[] features = svc.getFeatures("f1");
        assertEquals(1, features.length);
        Feature feat = features[0];
        assertNotNull("No feature named f1 found", feat);
        java.util.List<BundleInfo> bundles = feat.getBundles();
        assertEquals(2, bundles.size());
    }

    @Test
    public void testJsonFeatureFile() throws Exception {
        URI uri = createTempRepo("{" +
                "\"name\": \"test\"," +
                "\"feature\": [" +
                "{ \"name\": \"f1\", " +
                "\"bundle\": [" +
                "{ \"location\": \"file:bundle1\" }," +
                "{ \"location\": \"file:bundle2\" }" +
                "]" +
                "}" +
                "]" +
                "}");

        FeaturesService svc = createServiceForRepoTests();
        svc.addRepository(uri);
        Feature[] features = svc.getFeatures("f1");
        assertEquals(1, features.length);
        Feature feat = features[0];
        assertNotNull("No feature named f1 found", feat);
        java.util.List<BundleInfo> bundles = feat.getBundles();
        assertEquals(2, bundles.size());
    }

    //
    // Shared helpers
    //

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
