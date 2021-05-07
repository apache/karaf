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

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationPIDTest {

    @Test
    public void testR7FactoryPID() {
        final String pid = "org.apache.karaf.test.my.factory~Foo";
        final ConfigurationPID configurationPID = ConfigurationPID.parsePid(pid);
        Assert.assertEquals(pid, configurationPID.getPid());
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isFactory());
        Assert.assertTrue(configurationPID.isR7());
    }

    @Test
    public void testNonR7FactoryPID() {
        final String pid = "org.apache.karaf.test.my.factory-Foo";
        final ConfigurationPID configurationPID = ConfigurationPID.parsePid(pid);
        Assert.assertEquals(pid, configurationPID.getPid());
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isFactory());
        Assert.assertFalse(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithCfgExtension() {
        final String filename = "org.apache.karaf.test.my.factory~Foo.cfg";
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename);
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isFactory());
        Assert.assertTrue(configurationPID.isR7());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithConfigExtension() {
        final String filename = "org.apache.karaf.test.my.factory~Foo.config";
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename);
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isR7());
        Assert.assertTrue(configurationPID.isFactory());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithJsonExtension() {
        final String filename = "org.apache.karaf.test.my.factory~Foo.json";
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename);
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isR7());
        Assert.assertTrue(configurationPID.isFactory());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithCfgJsonExtension() {
        final String filename = "org.apache.karaf.test.my.factory~Foo.cfg.json";
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename, "cfg.json");
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isR7());
        Assert.assertTrue(configurationPID.isFactory());
    }

    @Test
    public void testR7FactoryPIDFromFilenameWithNoExtension() {
        final String filename = "org.apache.karaf.test.my.factory~Foo";
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename, "");
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertTrue(configurationPID.isR7());
        Assert.assertTrue(configurationPID.isFactory());
    }

    @Test
    public void testNonR7FactoryPIDFromFilenameWithCfg() {
        final String filename = "org.apache.karaf.test.my.factory-Foo.cfg";
        final ConfigurationPID configurationPID = ConfigurationPID.parseFilename(filename);
        Assert.assertEquals("org.apache.karaf.test.my.factory", configurationPID.getFactoryPid());
        Assert.assertEquals("Foo", configurationPID.getName());
        Assert.assertFalse(configurationPID.isR7());
        Assert.assertTrue(configurationPID.isFactory());
    }

}
