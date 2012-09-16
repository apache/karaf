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
import org.apache.karaf.features.internal.RepositoryImpl;


public class ConditionalTest extends TestCase {

    public void testLoad() throws Exception {
        RepositoryImpl r = new RepositoryImpl(getClass().getResource("internal/f06.xml").toURI());
        // Check repo
        Feature[] features = r.getFeatures();
        assertNotNull(features);
        assertEquals(1, features.length);
        Feature feature = features[0];

        assertNotNull(feature.getConditional());
        assertEquals(1,feature.getConditional().size());

        Conditional conditional = feature.getConditional().get(0);
        assertNotNull(conditional.getCondition());
        assertEquals(1,conditional.getCondition().size());
        Dependency dependency = conditional.getCondition().get(0);
        assertNotNull(dependency);
        assertEquals(dependency.getName(),"http");
        assertNotNull(conditional.getBundles());
        assertEquals(1, feature.getConditional().get(0).getBundles().size());

        String wrapperName = "my6/1.5.3-beta-3".replaceAll("[^A-Za-z0-9 ]", "_");
    }
}
