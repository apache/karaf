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
package org.apache.karaf.features.internal.service;

import java.io.FileWriter;
import java.net.URI;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.processing.BundleReplacements;
import org.apache.karaf.features.internal.model.processing.FeatureReplacements;
import org.apache.karaf.features.internal.model.processing.FeaturesProcessing;
import org.apache.karaf.features.internal.model.processing.ObjectFactory;
import org.apache.karaf.features.internal.model.processing.OverrideBundleDependency;
import org.apache.karaf.util.maven.Parser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FeaturesProcessorTest {

    public static Logger LOG = LoggerFactory.getLogger(FeaturesProcessorTest.class);

    @Test
    public void jaxbModelForProcessor() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(ObjectFactory.class);
        FeaturesProcessing fp = (FeaturesProcessing) jaxb.createUnmarshaller().unmarshal(getClass().getResourceAsStream("/org/apache/karaf/features/internal/service/org.apache.karaf.features.xml"));
        assertThat(fp.getFeatureReplacements().getReplacements().get(0).getFeature().getName(), equalTo("pax-jsf-resources-support"));

        Marshaller marshaller = jaxb.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(fp, System.out);
    }

    @Test
    public void versionRanges() {
        LOG.info(new VersionRange("1", false).toString());
        LOG.info(new VersionRange("[2,3)", true).toString());
    }

    @Test
    public void mavenURIs() throws Exception {
        Parser p = new Parser("group/artifact/[1,2)/xml/features*");
        assertThat(p.getVersion(), equalTo("[1,2)"));
        assertThat(p.getClassifier(), equalTo("features*"));

        p = new Parser("org.springframework*/*cloud*/*");
        assertThat(p.getVersion(), equalTo("*"));
        assertThat(p.getArtifact(), equalTo("*cloud*"));
        assertThat(p.getGroup(), equalTo("org.springframework*"));
        assertThat(p.getType(), equalTo("jar"));
        assertThat(p.getClassifier(), nullValue());

        p = new Parser("org.ops4j/org.ops4j*/*//uber");
        assertThat(p.getVersion(), equalTo("*"));
        assertThat(p.getArtifact(), equalTo("org.ops4j*"));
        assertThat(p.getGroup(), equalTo("org.ops4j"));
        assertThat(p.getType(), equalTo("jar"));
        assertThat(p.getClassifier(), equalTo("uber"));
    }

    @Test
    public void readingLegacyOverrides() {
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                "file:src/test/resources/org/apache/karaf/features/internal/service/overrides2.properties",
                null, null, null));

        FeaturesProcessing instructions = processor.getInstructions();
        BundleReplacements bundleReplacements = instructions.getBundleReplacements();
        assertThat(bundleReplacements.getOverrideBundles().size(), equalTo(5));
        BundleReplacements.OverrideBundle o1 = bundleReplacements.getOverrideBundles().get(0);
        assertThat(o1.getOriginalUri(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.command/[2.3.0,2.3.0.61033X)"));
        assertThat(o1.getReplacement(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.command/2.3.0.61033X"));
        BundleReplacements.OverrideBundle o2 = bundleReplacements.getOverrideBundles().get(1);
        assertThat(o2.getOriginalUri(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.core/[2.2.0,2.4.0)"));
        assertThat(o2.getReplacement(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.core/2.3.0.61033X"));
        BundleReplacements.OverrideBundle o3 = bundleReplacements.getOverrideBundles().get(2);
        assertThat(o3.getOriginalUri(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.resources/[2.3.0,2.3.14)"));
        assertThat(o3.getReplacement(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.resources/2.3.14"));
        BundleReplacements.OverrideBundle o4 = bundleReplacements.getOverrideBundles().get(3);
        assertThat(o4.getOriginalUri(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.kernel/[2.0.0,2.0.0]"));
        assertThat(o4.getReplacement(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.kernel/2.3.14"));
        BundleReplacements.OverrideBundle o5 = bundleReplacements.getOverrideBundles().get(4);
        assertThat(o5.getOriginalUri(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.infinity/[1.0.0,*)"));
        assertThat(o5.getReplacement(), equalTo("mvn:org.apache.karaf.admin/org.apache.karaf.admin.infinity/2.3.14"));
    }

    @Test
    public void readingLegacyBlacklist() {
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                null,
                "file:src/test/resources/org/apache/karaf/features/internal/service/blacklisted2.properties",
                null, null));

        FeaturesProcessing instructions = processor.getInstructions();
        Blacklist blacklist = instructions.getBlacklist();
        Clause[] clauses = blacklist.getClauses();
        assertThat(clauses.length, equalTo(4));
        assertTrue(blacklist.isFeatureBlacklisted("spring", "2.5.6.SEC02"));
        assertFalse(blacklist.isFeatureBlacklisted("spring", "2.5.7.SEC02"));
        assertFalse(blacklist.isFeatureBlacklisted("jclouds", "1"));

        assertTrue(blacklist.isBundleBlacklisted("mvn:org.spring/spring-infinity/42"));
        assertFalse(blacklist.isBundleBlacklisted("mvn:org.spring/spring-infinity/41"));
        assertTrue(blacklist.isBundleBlacklisted("mvn:org.spring/spring-eternity/42"));
        assertTrue(blacklist.isBundleBlacklisted("mvn:jclouds/jclouds/1"));
    }

    @Test
    public void blacklistingRepositories() {
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                null, null,
                "file:src/test/resources/org/apache/karaf/features/internal/service/fpi01.xml", null));
        URI uri = URI.create("file:src/test/resources/org/apache/karaf/features/internal/service/fp01.xml");
        RepositoryImpl repo = (RepositoryImpl) new RepositoryCacheImpl(processor).create(uri, true);
        assertThat(repo.getRepositories().length, equalTo(3));
        assertFalse(repo.isBlacklisted());
        assertFalse(processor.isRepositoryBlacklisted(repo.getRepositories()[0].toString()));
        assertTrue(processor.isRepositoryBlacklisted(repo.getRepositories()[1].toString()));
        assertFalse(processor.isRepositoryBlacklisted(repo.getRepositories()[2].toString()));
    }

    @Test
    public void blacklistingFeatures() {
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                null, null,
                "file:src/test/resources/org/apache/karaf/features/internal/service/fpi01.xml", null));
        URI uri = URI.create("file:src/test/resources/org/apache/karaf/features/internal/service/fp02.xml");
        RepositoryImpl repo = (RepositoryImpl) new RepositoryCacheImpl(processor).create(uri, true);

        Feature[] features = repo.getFeatures();
        assertTrue(features[0].isBlacklisted());
        assertFalse(features[1].isBlacklisted());
        assertTrue(features[2].isBlacklisted());
        assertTrue(features[3].isBlacklisted());
        assertFalse(features[4].isBlacklisted());
    }

    @Test
    public void blacklistingBundles() {
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                null, null,
                "file:src/test/resources/org/apache/karaf/features/internal/service/fpi01.xml", null));
        URI uri = URI.create("file:src/test/resources/org/apache/karaf/features/internal/service/fp03.xml");
        RepositoryImpl repo = (RepositoryImpl) new RepositoryCacheImpl(processor).create(uri, true);

        Feature f1 = repo.getFeatures()[0];
        assertFalse(f1.getBundles().get(0).isBlacklisted());
        assertFalse(f1.getBundles().get(1).isBlacklisted());
        assertTrue(f1.getBundles().get(2).isBlacklisted());
        assertTrue(f1.getBundles().get(3).isBlacklisted());
        assertTrue(f1.getConditional().get(0).getBundles().get(0).isBlacklisted());
    }

    @Test
    public void overridingBundles() {
        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                null, null,
                "file:src/test/resources/org/apache/karaf/features/internal/service/fpi02.xml", null));
        URI uri = URI.create("file:src/test/resources/org/apache/karaf/features/internal/service/fp03.xml");
        RepositoryImpl repo = (RepositoryImpl) new RepositoryCacheImpl(processor).create(uri, true);

        Feature f1 = repo.getFeatures()[0];
        assertTrue(f1.getBundles().get(0).isOverriden() == BundleInfo.BundleOverrideMode.NONE);
        assertTrue(f1.getBundles().get(1).isOverriden() == BundleInfo.BundleOverrideMode.OSGI);
        assertThat(f1.getBundles().get(1).getLocation(), equalTo("mvn:commons-io/commons-io/1.3.5"));
        assertThat(f1.getBundles().get(1).getOriginalLocation(), equalTo("mvn:commons-io/commons-io/1.3"));
        assertTrue(f1.getBundles().get(2).isOverriden() == BundleInfo.BundleOverrideMode.MAVEN);
        assertThat(f1.getBundles().get(2).getLocation(), equalTo("mvn:commons-codec/commons-codec/1.4.2"));
        assertThat(f1.getBundles().get(2).getOriginalLocation(), equalTo("mvn:commons-codec/commons-codec/0.4"));
        assertTrue(f1.getBundles().get(3).isOverriden() == BundleInfo.BundleOverrideMode.NONE);
        assertTrue(f1.getConditional().get(0).getBundles().get(0).isOverriden() == BundleInfo.BundleOverrideMode.OSGI);
        assertThat(f1.getConditional().get(0).getBundles().get(0).getLocation(), equalTo("mvn:org.glassfish/something-strangest/4.3.1"));
        assertThat(f1.getConditional().get(0).getBundles().get(0).getOriginalLocation(), equalTo("mvn:org.glassfish/something-strangest/4.3.0"));
        assertTrue(f1.getConditional().get(0).getBundles().get(1).isOverriden() == BundleInfo.BundleOverrideMode.NONE);
    }

    @Test
    public void resolvePlaceholders() throws Exception {
        Properties props = new Properties();
        props.put("version.jclouds", "1.9");
        props.put("version.commons-io", "2.5");
        props.store(new FileWriter("target/versions.properties"), null);

        FeaturesProcessorImpl processor = new FeaturesProcessorImpl(new FeaturesServiceConfig(
                null, null,
                "file:src/test/resources/org/apache/karaf/features/internal/service/fpi03.xml",
                "file:target/versions.properties"));

        assertThat(processor.getInstructions().getBlacklistedRepositories().get(0),
                equalTo("mvn:org.jclouds/jclouds-features/1.9/xml/features"));
        assertThat(processor.getInstructions().getBundleReplacements().getOverrideBundles().get(0).getReplacement(),
                equalTo("mvn:commons-io/commons-io/2.5"));
    }

    @Test
    public void serializeWithComments() {
        FeaturesProcessingSerializer serializer = new FeaturesProcessingSerializer();
        FeaturesProcessing featuresProcessing = new FeaturesProcessing();
        featuresProcessing.getBlacklistedRepositories().add("repository 1");
        OverrideBundleDependency.OverrideDependency d1 = new OverrideBundleDependency.OverrideDependency();
        d1.setDependency(true);
        d1.setUri("uri 1");
        featuresProcessing.getOverrideBundleDependency().getRepositories().add(d1);
        OverrideBundleDependency.OverrideFeatureDependency d2 = new OverrideBundleDependency.OverrideFeatureDependency();
        d2.setDependency(false);
        d2.setName("n");
        d2.setVersion("1.2.3");
        featuresProcessing.getOverrideBundleDependency().getFeatures().add(d2);
        BundleReplacements.OverrideBundle override = new BundleReplacements.OverrideBundle();
        override.setOriginalUri("original");
        override.setReplacement("replacement");
        override.setMode(BundleReplacements.BundleOverrideMode.OSGI);
        featuresProcessing.getBundleReplacements().getOverrideBundles().add(override);
        FeatureReplacements.OverrideFeature of = new FeatureReplacements.OverrideFeature();
        of.setMode(FeatureReplacements.FeatureOverrideMode.REPLACE);
        org.apache.karaf.features.internal.model.Feature f = new org.apache.karaf.features.internal.model.Feature();
        f.setName("f1");
        Bundle b = new Bundle();
        b.setLocation("location");
        f.getBundle().add(b);
        of.setFeature(f);
        featuresProcessing.getFeatureReplacements().getReplacements().add(of);
        serializer.write(featuresProcessing, System.out);
    }

}
