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

import java.net.URI;

import junit.framework.TestCase;
import org.apache.felix.karaf.features.internal.RepositoryImpl;
import org.apache.felix.karaf.features.internal.FeatureImpl;


public class RepositoryTest extends TestCase {

    public void testLoad() throws Exception {
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("repo1.xml").toURI());
        // Check repo
        URI[] repos = r.getRepositories();
        assertNotNull(repos);
        assertEquals(1, repos.length);
        assertEquals(URI.create("urn:r1"), repos[0]);
        // Check features
        Feature[] features = r.getFeatures();
        assertNotNull(features);
        assertEquals(2, features.length);
        assertNotNull(features[0]);
        assertEquals("f1", features[0].getName());
        assertNotNull(features[0].getConfigurations());
        assertEquals(1, features[0].getConfigurations().size());
        assertNotNull(features[0].getConfigurations().get("c1"));
        assertEquals(1, features[0].getConfigurations().get("c1").size());
        assertEquals("v", features[0].getConfigurations().get("c1").get("k"));
        assertNotNull(features[0].getDependencies());
        assertEquals(0, features[0].getDependencies().size());
        assertNotNull(features[0].getBundles());
        assertEquals(2, features[0].getBundles().size());
        assertEquals("b1", features[0].getBundles().get(0));
        assertEquals("b2", features[0].getBundles().get(1));
        assertNotNull(features[1]);
        assertEquals("f2", features[1].getName());
        assertNotNull(features[1].getConfigurations());
        assertEquals(0, features[1].getConfigurations().size());
        assertNotNull(features[1].getDependencies());
        assertEquals(1, features[1].getDependencies().size());
        assertEquals("f1" + FeatureImpl.SPLIT_FOR_NAME_AND_VERSION + FeatureImpl.DEFAULT_VERSION, features[1].getDependencies().get(0).toString());
        assertNotNull(features[1].getBundles());
        assertEquals(1, features[1].getBundles().size());
        assertEquals("b3", features[1].getBundles().get(0));
    }
    
    public void testShowWrongUriInException() throws Exception {
        String uri = "src/test/resources/org/apache/felix/karaf/shell/features/repo1.xml";
        RepositoryImpl r = new RepositoryImpl(new URI(uri));
        try {
            r.load();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(uri));
        }
    }
}
