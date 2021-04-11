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

import org.junit.Test;

import static org.apache.karaf.util.config.ConfigurationPID.parseFilename;
import static org.apache.karaf.util.config.ConfigurationPID.parsePid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationPIDTest {

    @Test
    public void testR7FactoryPID() {
        final String pid = "org.apache.felix.jaas.Configuration.factory~TokenLoginModule";
        final ConfigurationPID configurationPID = parsePid(pid);
        assertEquals(pid, configurationPID.getPid());
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertTrue(configurationPID.isR7());
    }

    @Test
    public void testPreR7FactoryPID() {
        final String pid = "org.apache.felix.jaas.Configuration.factory-TokenLoginModule";
        final ConfigurationPID configurationPID = parsePid(pid);
        assertEquals(pid, configurationPID.getPid());
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertFalse(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithCfgExtension() {
        final String filename = "org.apache.felix.jaas.Configuration.factory~TokenLoginModule.cfg";
        final ConfigurationPID configurationPID = parseFilename(filename);
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertTrue(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithConfigExtension() {
        final String filename = "org.apache.felix.jaas.Configuration.factory~TokenLoginModule.config";
        final ConfigurationPID configurationPID = parseFilename(filename);
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertTrue(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithJsonExtension() {
        final String filename = "org.apache.felix.jaas.Configuration.factory~TokenLoginModule.json";
        final ConfigurationPID configurationPID = parseFilename(filename);
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertTrue(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithCfgJsonExtension() {
        final String filename = "org.apache.felix.jaas.Configuration.factory~TokenLoginModule.cfg.json";
        final ConfigurationPID configurationPID = parseFilename(filename, "cfg.json");
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertTrue(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithNoExtension() {
        final String filename = "org.apache.felix.jaas.Configuration.factory~TokenLoginModule";
        final ConfigurationPID configurationPID = parseFilename(filename, "");
        assertEquals("org.apache.felix.jaas.Configuration.factory", configurationPID.getFactoryPid());
        assertEquals("TokenLoginModule", configurationPID.getName());
        assertTrue(configurationPID.isFactory());
        assertTrue(configurationPID.isR7());
    }

}
