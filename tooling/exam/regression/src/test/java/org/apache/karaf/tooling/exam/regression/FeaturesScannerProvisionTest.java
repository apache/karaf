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

package org.apache.karaf.tooling.exam.regression;

import static junit.framework.Assert.assertTrue;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FeaturesScannerProvisionTest {

    @Inject
    private FeaturesService featuresService;

    @Configuration
    public Option[] config() {
        return new Option[]{
            karafDistributionConfiguration().frameworkUrl(
                maven().groupId("org.apache.karaf.assemblies").artifactId("apache-karaf").type("zip")
                    .versionAsInProject()),
            scanFeatures(
                maven().groupId("org.apache.karaf.assemblies.features").artifactId("standard").type("xml")
                    .classifier("features").version("2.2.4"), "kar", "wrapper").start() };
    }

    @Test
    public void test() throws Exception {
        assertTrue(featuresService.isInstalled(featuresService.getFeature("kar")));
        assertTrue(featuresService.isInstalled(featuresService.getFeature("wrapper")));
    }

}
