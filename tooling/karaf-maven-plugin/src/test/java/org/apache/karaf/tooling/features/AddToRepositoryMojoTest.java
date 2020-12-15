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

import org.apache.karaf.features.internal.model.Feature;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

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

}
