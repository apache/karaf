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

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.junit.Test;

public class BlacklistTest {

    @Test
    public void testBlacklistFeatureWithRange() {
        Stream<Feature> features = blacklistWith("spring;range=\"[2,3)\"");
        assertTrue(features.noneMatch(f -> f.getId().equals("spring/2.5.6.SEC02")));
    }

    @Test
    public void testBlacklistFeatureWithVersion() {
        Stream<Feature> features = blacklistWith("spring;range=2.5.6.SEC02");
        assertTrue(features.noneMatch(f -> f.getId().equals("spring/2.5.6.SEC02")));
    }

    @Test
    public void testBlacklistFeatureWithoutVersion() {
        Stream<Feature> features = blacklistWith("spring");
        assertTrue(features.noneMatch(f -> f.getId().startsWith("spring/")));
    }

    @Test
    public void testBlacklistBundle() {
        String blacklisted = "mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.jasypt/1.7_1";
        Stream<Feature> features = blacklistWith(blacklisted);
        Stream<BundleInfo> bundles = features.flatMap(f -> f.getBundles().stream());
        assertTrue(bundles.noneMatch(b -> b.getLocation().equals(blacklisted)));
    }

    private Stream<Feature> blacklistWith(String blacklistClause) {
        URL url = getClass().getResource("f02.xml");
        Features features = JaxbUtil.unmarshal(url.toExternalForm(), true);

        Blacklist blacklist = new Blacklist(Collections.singletonList(blacklistClause));
        blacklist.blacklist(features);
        return features.getFeature().stream();
    }
}
