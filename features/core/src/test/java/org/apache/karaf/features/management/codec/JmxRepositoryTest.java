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
package org.apache.karaf.features.management.codec;

import static org.apache.karaf.features.management.FeaturesServiceMBean.REPOSITORY_BLACKLISTED;
import static org.apache.karaf.features.management.FeaturesServiceMBean.REPOSITORY_FEATURES;
import static org.apache.karaf.features.management.FeaturesServiceMBean.REPOSITORY_NAME;
import static org.apache.karaf.features.management.FeaturesServiceMBean.REPOSITORY_REPOSITORIES;
import static org.apache.karaf.features.management.FeaturesServiceMBean.REPOSITORY_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.junit.Test;

public class JmxRepositoryTest {

    @Test
    public void testJmxRepositoryCompositeData() throws Exception {
        Features features = new Features();
        features.setName("test-1.0.0");
        features.getRepository().add("mvn:org.test/test-dependency/1.0.0/xml/features");
        features.getFeature().add(new Feature("test-feature", "1.0.0"));

        URI uri = new URI("mvn:org.test/test/1.0.0/xml/features");
        Repository repository = new RepositoryImpl(uri, features, true);

        JmxRepository jmxRepository = new JmxRepository(repository);
        CompositeData compositeData = jmxRepository.asCompositeData();

        assertEquals("test-1.0.0", compositeData.get(REPOSITORY_NAME));
        assertEquals(uri.toString(), compositeData.get(REPOSITORY_URI));
        assertTrue((Boolean) compositeData.get(REPOSITORY_BLACKLISTED));

        String[] repositoryUris = (String[]) compositeData.get(REPOSITORY_REPOSITORIES);
        assertEquals(1, repositoryUris.length);
        assertEquals("mvn:org.test/test-dependency/1.0.0/xml/features", repositoryUris[0]);

        TabularData repositoryFeatures = (TabularData) compositeData.get(REPOSITORY_FEATURES);
        assertEquals(1, repositoryFeatures.size());
        assertNotNull(repositoryFeatures.get(new Object[] {"test-feature", "1.0.0"}));
    }
}
