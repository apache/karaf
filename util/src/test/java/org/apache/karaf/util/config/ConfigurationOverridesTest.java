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
package org.apache.karaf.util.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationOverridesTest {

    private static final String PID = "org.apache.karaf.features";

    private final Map<String, String> environment = new HashMap<>();
    private final Map<String, String> systemProperties = new HashMap<>();

    @Test
    public void testNames() {
        Assert.assertEquals("ORG_APACHE_KARAF_FEATURES_UPDATESNAPSHOTS", ConfigurationOverrides.environmentVariable(PID, "updateSnapshots"));
        Assert.assertEquals("ORG_APACHE_FELIX_FILEINSTALL_DEPLOY_POLL", ConfigurationOverrides.environmentVariable("org.apache.felix.fileinstall~deploy", "poll"));
        Assert.assertEquals("MY_FACTORY_NAME_KEY", ConfigurationOverrides.environmentVariable("my.factory-name", "key"));
        Assert.assertEquals("org.apache.karaf.features.updateSnapshots", ConfigurationOverrides.systemProperty(PID, "updateSnapshots"));
    }

    @Test
    public void testSystemPropertyOverridesStatedValue() {
        systemProperties.put("org.apache.karaf.features.updateSnapshots", "none");
        Dictionary<String, Object> properties = properties("updateSnapshots", "crc", "autoRefresh", "true");

        apply(properties, Collections.list(properties.keys()));

        Assert.assertEquals("none", properties.get("updateSnapshots"));
        Assert.assertEquals("true", properties.get("autoRefresh"));
    }

    @Test
    public void testEnvironmentVariableWinsOverSystemProperty() {
        environment.put("ORG_APACHE_KARAF_FEATURES_UPDATESNAPSHOTS", "always");
        systemProperties.put("org.apache.karaf.features.updateSnapshots", "none");

        Assert.assertEquals("always", override("updateSnapshots", properties("updateSnapshots", "crc")));
    }

    @Test
    public void testOverridesKeyThePropertiesLack() {
        systemProperties.put("org.apache.karaf.features.configCfgStore", "false");

        Assert.assertEquals("false", override("configCfgStore", properties()));
        Assert.assertEquals("false", override("configCfgStore", null));
        Assert.assertNull(override("autoRefresh", properties()));
    }

    @Test
    public void testAppendsToTheValueItReplaces() {
        environment.put("ORG_APACHE_KARAF_FEATURES_FEATURESREPOSITORIES", "${featuresRepositories},third");
        Dictionary<String, Object> properties = properties("featuresRepositories", "first,second");

        apply(properties, Collections.singletonList("featuresRepositories"));

        Assert.assertEquals("first,second,third", properties.get("featuresRepositories"));
    }

    @Test
    public void testArray() {
        systemProperties.put("org.apache.karaf.features.featuresRepositories", "[first,second]");

        Assert.assertArrayEquals(new String[] { "first", "second" }, (String[]) override("featuresRepositories", null));
    }

    @Test
    public void testKeepsANumberANumber() {
        systemProperties.put("org.apache.karaf.shell.sshPort", "8102");
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("sshPort", 8101);

        ConfigurationOverrides.apply("org.apache.karaf.shell", properties, Collections.singletonList("sshPort"),
                environment::get, systemProperties::get);

        Assert.assertEquals(8102, properties.get("sshPort"));
    }

    private void apply(Dictionary<String, Object> properties, Collection<String> keys) {
        ConfigurationOverrides.apply(PID, properties, keys, environment::get, systemProperties::get);
    }

    private Object override(String key, Dictionary<String, Object> properties) {
        return ConfigurationOverrides.override(PID, key, properties, environment::get, systemProperties::get);
    }

    private static Dictionary<String, Object> properties(String... keysAndValues) {
        Dictionary<String, Object> properties = new Hashtable<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            properties.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return properties;
    }

}
