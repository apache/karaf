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

import java.net.URI;

import junit.framework.TestCase;
import org.apache.karaf.features.internal.resolver.FeatureResource;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.osgi.resource.Resource;


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
        assertEquals(3, features.length);
        assertNotNull(features[0]);
        assertEquals("http://karaf.apache.org/xmlns/features/v1.1.0", features[0].getNamespace());
        assertEquals("f1", features[0].getName());
        assertNotNull(features[0].getConfigurations());
        assertEquals(1, features[0].getConfigurations().size());
        assertNotNull(features[0].getConfigurations().get(0).getName());
        assertEquals("c1", features[0].getConfigurations().get(0).getName());
        assertEquals(1, features[0].getConfigurations().get(0).getProperties().size());
        assertEquals("v", features[0].getConfigurations().get(0).getProperties().get("k"));
        assertNotNull(features[0].getDependencies());
        assertEquals(0, features[0].getDependencies().size());
        assertNotNull(features[0].getBundles());
        assertEquals(2, features[0].getBundles().size());
        assertEquals("b1", features[0].getBundles().get(0).getLocation());
        assertEquals("b2", features[0].getBundles().get(1).getLocation());
        assertNotNull(features[1]);
        assertEquals("f2", features[1].getName());
        assertNotNull(features[1].getConfigurations());
        assertEquals(0, features[1].getConfigurations().size());
        assertNotNull(features[1].getDependencies());
        assertEquals(1, features[1].getDependencies().size());
        assertEquals("f1" + org.apache.karaf.features.internal.model.Feature.VERSION_SEPARATOR + org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION, features[1].getDependencies().get(0).toString());
        assertNotNull(features[1].getBundles());
        assertEquals(1, features[1].getBundles().size());
        assertEquals("b3", features[1].getBundles().get(0).getLocation());
        assertEquals("f3", features[2].getName());
        assertNotNull(features[2].getConfigurationFiles());
        assertEquals(1, features[2].getConfigurationFiles().size());
        assertEquals("cf1", features[2].getConfigurationFiles().get(0).getFinalname());
        assertEquals(true, features[2].getConfigurationFiles().get(0).isOverride());
        assertEquals("cfloc", features[2].getConfigurationFiles().get(0).getLocation());
    }

    public void testLoadFormattedRepo() throws Exception {
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("repo2.xml").toURI());
        // Check repo
        URI[] repos = r.getRepositories();
        assertNotNull(repos);
        assertEquals(1, repos.length);
        assertEquals(URI.create("urn:r1"), repos[0]);
        // Check features
        Feature[] features = r.getFeatures();
        assertNotNull(features);
        assertEquals(3, features.length);
        assertNotNull(features[0]);
        assertEquals("f1", features[0].getName());
        assertNotNull(features[0].getConfigurations());
        assertEquals(1, features[0].getConfigurations().size());
        assertNotNull(features[0].getConfigurations().get(0).getName());
        assertEquals("c1", features[0].getConfigurations().get(0).getName());
        assertEquals(1, features[0].getConfigurations().get(0).getProperties().size());
        assertEquals("v", features[0].getConfigurations().get(0).getProperties().get("k"));
        assertNotNull(features[0].getDependencies());
        assertEquals(0, features[0].getDependencies().size());
        assertNotNull(features[0].getBundles());
        assertEquals(2, features[0].getBundles().size());
        assertEquals("b1", features[0].getBundles().get(0).getLocation());
        assertEquals("b2", features[0].getBundles().get(1).getLocation());
        assertNotNull(features[1]);
        assertEquals("f2", features[1].getName());
        assertNotNull(features[1].getConfigurations());
        assertEquals(0, features[1].getConfigurations().size());
        assertNotNull(features[1].getDependencies());
        assertEquals(1, features[1].getDependencies().size());
        assertEquals("f1" + org.apache.karaf.features.internal.model.Feature.VERSION_SEPARATOR + org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION, features[1].getDependencies().get(0).toString());
        assertNotNull(features[1].getBundles());
        assertEquals(1, features[1].getBundles().size());
        assertEquals("b3", features[1].getBundles().get(0).getLocation());
        assertEquals("f3", features[2].getName());
        assertNotNull(features[2].getConfigurationFiles());
        assertEquals(1, features[2].getConfigurationFiles().size());
        assertEquals("cf1", features[2].getConfigurationFiles().get(0).getFinalname());
        assertEquals(true, features[2].getConfigurationFiles().get(0).isOverride());
        assertEquals("cfloc", features[2].getConfigurationFiles().get(0).getLocation());
    }
    
    public void testLoadConfigAppend() throws Exception {
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("repo4.xml").toURI());
        // Check repo
        URI[] repos = r.getRepositories();
        assertNotNull(repos);
        assertEquals(1, repos.length);
        assertEquals(URI.create("urn:r1"), repos[0]);
        // Check features
        Feature[] features = r.getFeatures();
        assertNotNull(features);
        assertEquals(3, features.length);
        assertNotNull(features[0]);
        assertEquals("f1", features[0].getName());
        assertNotNull(features[0].getConfigurations());
        assertEquals(1, features[0].getConfigurations().size());
        assertNotNull(features[0].getConfigurations().get(0).getName());
		assertEquals("c1", features[0].getConfigurations().get(0).getName());
        assertEquals(1, features[0].getConfigurations().get(0).getProperties().size());
        assertEquals("v", features[0].getConfigurations().get(0).getProperties().get("k"));
        assertTrue(features[0].getConfigurations().get(0).isAppend());
        assertNotNull(features[0].getDependencies());
        assertEquals(0, features[0].getDependencies().size());
        assertNotNull(features[0].getBundles());
        assertEquals(2, features[0].getBundles().size());
        assertEquals("b1", features[0].getBundles().get(0).getLocation());
        assertEquals("b2", features[0].getBundles().get(1).getLocation());
        assertNotNull(features[1]);
        assertEquals("f2", features[1].getName());
        assertNotNull(features[1].getConfigurations());
        assertEquals(0, features[1].getConfigurations().size());
        assertNotNull(features[1].getDependencies());
        assertEquals(1, features[1].getDependencies().size());
        assertEquals("f1" + org.apache.karaf.features.internal.model.Feature.VERSION_SEPARATOR + org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION, features[1].getDependencies().get(0).toString());
        assertNotNull(features[1].getBundles());
        assertEquals(1, features[1].getBundles().size());
        assertEquals("b3", features[1].getBundles().get(0).getLocation());
        assertEquals("f3", features[2].getName());
        assertNotNull(features[2].getConfigurationFiles());
        assertEquals(1, features[2].getConfigurationFiles().size());
        assertEquals("cf1", features[2].getConfigurationFiles().get(0).getFinalname());
        assertEquals(true, features[2].getConfigurationFiles().get(0).isOverride());
        assertEquals("cfloc", features[2].getConfigurationFiles().get(0).getLocation());
    }

    public void testLoadRepoWithCapabilitiesAndRequirement() throws Exception {
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("repo3.xml").toURI());
        // Check features
        Feature[] features = r.getFeatures();
        assertNotNull(features);
        assertEquals(1, features.length);
        assertNotNull(features[0]);
        assertEquals("f1", features[0].getName());
        assertEquals(1, features[0].getCapabilities().size());
        assertEquals("cap", features[0].getCapabilities().get(0).getValue().trim());
        assertEquals(1, features[0].getRequirements().size());
        assertEquals("req", features[0].getRequirements().get(0).getValue().trim());

        Resource res = FeatureResource.build(features[0], null, null);
        assertEquals(1, res.getCapabilities("cap").size());
        assertEquals(1, res.getRequirements("req").size());
    }

    public void testShowWrongUriInException() throws Exception {
        String uri = "src/test/resources/org/apache/karaf/shell/features/repo1.xml";
        try {
            new RepositoryImpl(new URI(uri));
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(uri));
        }
    }
}
