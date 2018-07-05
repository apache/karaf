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

import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_BLACKLISTED;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_BUNDLES;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_CONFIGURATIONFILES;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_CONFIGURATIONS;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_CONFIG_PID;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_DEPENDENCIES;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_INSTALLED;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_NAME;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_REQUIRED;
import static org.apache.karaf.features.management.FeaturesServiceMBean.FEATURE_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Config;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.junit.Test;

public class JmxFeatureTest {

    @Test
    public void testJmxFeatureCompositeData() throws Exception {
        Feature feature = new Feature();
        feature.setName("test-feature");
        feature.setVersion("1.0.0");
        feature.setBlacklisted(true);

        Dependency dependency = new Dependency();
        dependency.setName("test-dependent-feature");
        dependency.setVersion("1.0.0");
        feature.getFeature().add(dependency);

        Bundle bundle = new Bundle();
        bundle.setLocation("mvn:org.test/test/1.0.0");
        feature.getBundle().add(bundle);

        Config config = new Config();
        config.setName(FEATURE_CONFIG_PID);
        config.setValue("org.test.pid");
        config.setAppend(false);
        config.getProperties().put("test-key", "test-value");
        feature.getConfig().add(config);

        ConfigFile configFile = new ConfigFile();
        configFile.setFinalname("test-configfile.cfg");
        feature.getConfigfile().add(configFile);

        JmxFeature jmxFeature = new JmxFeature(feature, true, true);
        CompositeData compositeData = jmxFeature.asCompositeData();

        assertEquals("test-feature", compositeData.get(FEATURE_NAME));
        assertEquals("1.0.0", compositeData.get(FEATURE_VERSION));
        assertTrue((Boolean) compositeData.get(FEATURE_INSTALLED));
        assertTrue((Boolean) compositeData.get(FEATURE_BLACKLISTED));
        assertTrue((Boolean) compositeData.get(FEATURE_REQUIRED));

        TabularData featureDependencies = (TabularData) compositeData.get(FEATURE_DEPENDENCIES);
        assertEquals(1, featureDependencies.size());
        assertNotNull(featureDependencies.get(new Object[] {"test-dependent-feature", "1.0.0"}));

        String[] bundleUrls = (String[]) compositeData.get(FEATURE_BUNDLES);
        assertEquals(new String[] {"mvn:org.test/test/1.0.0"}, bundleUrls);

        TabularData featureConfigs = (TabularData) compositeData.get(FEATURE_CONFIGURATIONS);
        assertEquals(1, featureConfigs.size());
        assertNotNull(featureConfigs.get(new Object[] {FEATURE_CONFIG_PID}));

        TabularData featureConfigFiles = (TabularData) compositeData.get(FEATURE_CONFIGURATIONFILES);
        assertEquals(1, featureConfigFiles.size());
        assertNotNull(featureConfigFiles.get(new Object[] {"test-configfile.cfg"}));
    }
}
