/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.features;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.tooling.features.AddToRepositoryMojo.Override;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.Map.Entry;

public class AddToRepositoryMojoTest {

    @Test
    public void test() throws Exception {
        AddToRepositoryMojo mojo = new AddToRepositoryMojo();

        List<String> featuresNames = new ArrayList<>();
        featuresNames.add("test");

        Set<Feature> features = new HashSet<>();

        Feature testFeature = new Feature();
        testFeature.setName("test");
        testFeature.setVersion("1.0.0");

        Map<String, Feature> featuresMap = new HashMap<>();
        featuresMap.put("test/1.0.0", testFeature);

        Feature otherFeature = new Feature();
        otherFeature.setName("other");
        otherFeature.setVersion("2.0.0");
        featuresMap.put("other/2.0.0", otherFeature);

        mojo.addFeatures(featuresNames, features, featuresMap, false);

        Assert.assertEquals(1, features.size());
        Iterator<Feature> iterator = features.iterator();
        Feature check = iterator.next();
        Assert.assertEquals("test", check.getName());
        Assert.assertEquals("1.0.0", check.getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotMatch() throws Exception {
        AddToRepositoryMojo mojo = new AddToRepositoryMojo();

        List<String> featuresNames = new ArrayList<>();
        featuresNames.add("fo.*");

        Set<Feature> features = new HashSet<>();

        Feature testFeature = new Feature();
        testFeature.setName("test");
        testFeature.setVersion("1.0.0");

        Map<String, Feature> featuresMap = new HashMap<>();
        featuresMap.put("test/1.0.0", testFeature);

        Feature otherFeature = new Feature();
        otherFeature.setName("other");
        otherFeature.setVersion("2.0.0");
        featuresMap.put("other/2.0.0", otherFeature);

        mojo.addFeatures(featuresNames, features, featuresMap, false);
    }

    @Test
    public void testRegex() throws Exception {
        AddToRepositoryMojo mojo = new AddToRepositoryMojo();

        List<String> featuresNames = new ArrayList<>();
        featuresNames.add("te.*");

        Set<Feature> features = new HashSet<>();

        Feature testFeature = new Feature();
        testFeature.setName("test");
        testFeature.setVersion("1.0.0");

        Map<String, Feature> featuresMap = new HashMap<>();
        featuresMap.put("test/1.0.0", testFeature);

        Feature tempFeature = new Feature();
        tempFeature.setName("temp");
        tempFeature.setVersion("1.1.0");
        featuresMap.put("temp/1.1.0", tempFeature);

        Feature otherFeature = new Feature();
        otherFeature.setName("other");
        otherFeature.setVersion("2.0.0");
        featuresMap.put("other/2.0.0", otherFeature);

        mojo.addFeatures(featuresNames, features, featuresMap, false);

        Assert.assertEquals(2, features.size());
        Iterator<Feature> iterator = features.iterator();
        Feature check = iterator.next();
        Assert.assertEquals("temp", check.getName());
        Assert.assertEquals("1.1.0", check.getVersion());
        check = iterator.next();
        Assert.assertEquals("test", check.getName());
        Assert.assertEquals("1.0.0", check.getVersion());
    }

    @Test
    @SuppressWarnings("serial")
    public void testOverrides() throws Exception {
        AddToRepositoryMojo mojo = new AddToRepositoryMojo();

        for (Entry<String,String> entry : new LinkedHashMap<String,String>() {{
            put("mvn:org.apache.karaf.features/org.apache.karaf.features.core/2.2.0", "mvn:org.apache.karaf.features/org.apache.karaf.features.core/2.2.8");
            put("mvn:org.apache.karaf.features/org.apache.karaf.features.core/1.0.0", "mvn:org.apache.karaf.features/org.apache.karaf.features.core/2.2.8;range=[1.0,3.0)");
            put("mvn:commons-net/commons-net/3.8.0", "mvn:commons-net/commons-net/3.10.0;range=[3,3.10.0)");
            put("mvn:test/test/1.0-SNAPSHOT", "mvn:test/test/1.0.8-SNAPSHOT");
            put("mvn:test/test/0.9-SNAPSHOT", "mvn:test/test/1.0-SNAPSHOT;range=[0,1)");
        }}.entrySet()) {
            Assert.assertTrue(mojo.shouldOverride(new Bundle(entry.getKey()), new Override(entry.getValue())));
        }

        for (Entry<String,String> entry : new LinkedHashMap<String,String>() {{
            // in theory, these 2 should mean the same thing
            put("mvn:org.apache.karaf.features/org.apache.karaf.features.core/1.0.0", "mvn:org.apache.karaf.features/org.apache.karaf.features.core/2.2.8");
            put("mvn:org.apache.karaf.features/org.apache.karaf.features.core/1.0.0", "mvn:org.apache.karaf.features/org.apache.karaf.features.core/2.2.8;range=[2.2,2.3)");
            put("mvn:test/test/1.2-SNAPSHOT", "mvn:test/test/1.0.8-SNAPSHOT");
            put("mvn:test/test/1.0-SNAPSHOT", "mvn:test/test/1.0-SNAPSHOT;range=[0,1)");
        }}.entrySet()) {
            Assert.assertFalse(mojo.shouldOverride(new Bundle(entry.getKey()), new Override(entry.getValue())));
        }
    }

}
