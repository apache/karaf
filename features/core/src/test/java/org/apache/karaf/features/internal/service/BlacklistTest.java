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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BlacklistTest {

    @Test
    public void testBlacklistFeatureWithRange() throws IOException {
        Stream<Feature> features = blacklistWith("spring;range=\"[2,3)\"");
        assertTrue(features.noneMatch(f -> f.getId().equals("spring/2.5.6.SEC02") && !f.isBlacklisted()));
    }

    @Test
    public void testBlacklistFeatureWithVersion() throws IOException {
        Stream<Feature> features = blacklistWith("spring;range=2.5.6.SEC02");
        assertTrue(features.noneMatch(f -> f.getId().equals("spring/2.5.6.SEC02") && !f.isBlacklisted()));
    }

    @Test
    public void testBlacklistFeatureWithoutVersion() throws IOException {
        Stream<Feature> features = blacklistWith("spring");
        assertTrue(features.noneMatch(f -> f.getId().startsWith("spring/") && !f.isBlacklisted()));
    }

    @Test
    public void testBlacklistBundle() throws IOException {
        String blacklisted = "mvn:org.apache.servicemix.bundles/org.apache.servicemix.bundles.jasypt/1.7_1";
        Stream<Feature> features = blacklistWith(blacklisted);
        Stream<BundleInfo> bundles = features.flatMap(f -> f.getBundles().stream());
        assertTrue(bundles.noneMatch(b -> b.getLocation().equals(blacklisted) && !b.isBlacklisted()));
    }

    @Test
    public void testBlacklistLoad() throws URISyntaxException {
        Blacklist blacklist = new Blacklist(getClass().getResource("blacklist.txt").toExternalForm());
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("f02.xml").toURI(), true);
        Stream<Feature> features = Arrays.stream(repo.getFeatures());
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig());
        processor.getInstructions().postUnmarshall(blacklist, new HashSet<>());
        repo.processFeatures(processor);
        assertTrue(features.noneMatch(f -> f.getId().equals("spring/2.5.6.SEC02") && !f.isBlacklisted()));
    }

    private Stream<org.apache.karaf.features.Feature> blacklistWith(String blacklistClause) throws IOException {
        URI uri;
        try {
            uri = getClass().getResource("f02.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        File blacklistedProperties = File.createTempFile("blacklisted-", ".properties", new File("target"));
        try (FileOutputStream fos = new FileOutputStream(blacklistedProperties)) {
            fos.write(blacklistClause.getBytes(StandardCharsets.UTF_8));
        }
        RepositoryImpl features = new RepositoryImpl(uri, true);
        FeaturesServiceConfig config = new FeaturesServiceConfig(null, FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE, FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE, null, 1, 0, 0, blacklistedProperties.toURI().toString(), null, null, null, true);
        features.processFeatures(new FeaturesProcessorImpl(config));
        return Arrays.stream(features.getFeatures());
    }

}
