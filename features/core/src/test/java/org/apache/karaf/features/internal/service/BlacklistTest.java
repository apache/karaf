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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class BlacklistTest {

    @Test
    public void testBlacklistFeatureWithRange() {
        URL url = getClass().getResource("f02.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);

        List<String> blacklist = new ArrayList<>();
        blacklist.add("spring;range=\"[2,3)\"");

        Blacklist.blacklist(features, blacklist);
        for (Feature feature : features.getFeature()) {
            assertNotEquals("spring/2.5.6.SEC02", feature.getId());
        }
    }

    @Test
    public void testBlacklistFeatureWithVersion() {
        URL url = getClass().getResource("f02.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);

        List<String> blacklist = new ArrayList<>();
        blacklist.add("spring;range=2.5.6.SEC02");

        Blacklist.blacklist(features, blacklist);
        for (Feature feature : features.getFeature()) {
            assertNotEquals("spring/2.5.6.SEC02", feature.getId());
        }
    }

    @Test
    public void testBlacklistFeatureWithoutVersion() {
        URL url = getClass().getResource("f02.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);

        List<String> blacklist = new ArrayList<>();
        blacklist.add("spring");

        Blacklist.blacklist(features, blacklist);
        for (Feature feature : features.getFeature()) {
            assertFalse(feature.getId().startsWith("spring/"));
        }
    }

    @Test
    public void testBlacklistBundle() {
        URL url = getClass().getResource("f02.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);

        List<String> blacklist = new ArrayList<>();
        blacklist.add("mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.jasypt/1.7_1");

        Blacklist.blacklist(features, blacklist);
        for (Feature feature : features.getFeature()) {
            for (Bundle bundle : feature.getBundle()) {
                assertNotEquals("mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.jasypt/1.7_1",
                                bundle.getLocation());
            }
        }
    }
}
