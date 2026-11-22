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
package org.apache.karaf.services.logging.command;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class LoggerLevelsTest {

    private Hashtable<String, Object> config;

    @Before
    public void setUp() {
        config = new Hashtable<>();
        config.put("level", "INFO");
        config.put("console", "${karaf.log.console:-OFF}");
        config.put("logger.org.apache.aries.blueprint", "WARN");
        config.put("logger.org.apache.sshd.level", "ERROR");
    }

    @Test
    public void testGetLevels() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("ROOT", "INFO");
        expected.put("org.apache.aries.blueprint", "WARN");
        expected.put("org.apache.sshd", "ERROR");
        assertEquals(expected, LoggerLevels.getLevels(config));
        assertEquals("[ROOT, org.apache.aries.blueprint, org.apache.sshd]", LoggerLevels.getLevels(config).keySet().toString());
    }

    @Test
    public void testPlainKeyTakesPrecedence() {
        config.put("logger.org.apache.aries.blueprint.level", "DEBUG");
        assertEquals("WARN", LoggerLevels.getLevels(config).get("org.apache.aries.blueprint"));
    }

    @Test
    public void testRootLevel() {
        config.remove("level");
        config.put("rootLogger.level", "WARN");
        config.put("log.level", "ERROR");
        // same precedence as the Karaf logging service
        assertEquals("ERROR", LoggerLevels.getLevels(config).get("ROOT"));
        config.remove("log.level");
        assertEquals("WARN", LoggerLevels.getLevels(config).get("ROOT"));
        config.remove("rootLogger.level");
        assertEquals(System.getProperty("karaf.log.level", "INFO"), LoggerLevels.getLevels(config).get("ROOT"));
    }

    @Test
    public void testEffectiveLevel() {
        assertEquals("WARN", LoggerLevels.getEffectiveLevel(config, "org.apache.aries.blueprint"));
        assertEquals("WARN", LoggerLevels.getEffectiveLevel(config, "org.apache.aries.blueprint.container.Foo"));
        assertEquals("ERROR", LoggerLevels.getEffectiveLevel(config, "org.apache.sshd.server"));
        assertEquals("INFO", LoggerLevels.getEffectiveLevel(config, "org.apache.aries"));
        assertEquals("INFO", LoggerLevels.getEffectiveLevel(config, "foo"));
        assertEquals("INFO", LoggerLevels.getEffectiveLevel(config, "ROOT"));
    }

    @Test
    public void testSetRootLevel() {
        LoggerLevels.setLevel(config, null, "debug");
        assertEquals("DEBUG", config.get("level"));
        LoggerLevels.setLevel(config, "root", "WARN");
        assertEquals("WARN", config.get("level"));
    }

    @Test
    public void testSetRootLevelKeepsKey() {
        config.remove("level");
        config.put("rootLogger.level", "INFO");
        LoggerLevels.setLevel(config, "ROOT", "DEBUG");
        assertEquals("DEBUG", config.get("rootLogger.level"));
        assertNull(config.get("level"));
    }

    @Test
    public void testUnsetRootLevel() {
        try {
            LoggerLevels.setLevel(config, "ROOT", "DEFAULT");
            fail("Exception expected");
        } catch (IllegalStateException e) {
            // expected
        }
        assertEquals("INFO", config.get("level"));
    }

    @Test
    public void testSetLoggerLevel() {
        LoggerLevels.setLevel(config, "org.foo", "TRACE");
        assertEquals("TRACE", config.get("logger.org.foo"));
        LoggerLevels.setLevel(config, "org.apache.aries.blueprint", "ERROR");
        assertEquals("ERROR", config.get("logger.org.apache.aries.blueprint"));
        // the suffixed key is kept
        LoggerLevels.setLevel(config, "org.apache.sshd", "WARN");
        assertEquals("WARN", config.get("logger.org.apache.sshd.level"));
        assertNull(config.get("logger.org.apache.sshd"));
        // both keys are updated
        config.put("logger.org.apache.aries.blueprint.level", "DEBUG");
        LoggerLevels.setLevel(config, "org.apache.aries.blueprint", "INFO");
        assertEquals("INFO", config.get("logger.org.apache.aries.blueprint"));
        assertEquals("INFO", config.get("logger.org.apache.aries.blueprint.level"));
        // the other properties are kept
        assertEquals("${karaf.log.console:-OFF}", config.get("console"));
    }

    @Test
    public void testSetLoggerNamedLevel() {
        LoggerLevels.setLevel(config, "org.foo.level", "DEBUG");
        assertEquals("DEBUG", config.get("logger.org.foo.level.level"));
        assertEquals("DEBUG", LoggerLevels.getLevels(config).get("org.foo.level"));
        LoggerLevels.setLevel(config, "org.foo.level", "DEFAULT");
        assertNull(config.get("logger.org.foo.level.level"));
    }

    @Test
    public void testUnsetLoggerLevel() {
        config.put("logger.org.apache.aries.blueprint.level", "DEBUG");
        LoggerLevels.setLevel(config, "org.apache.aries.blueprint", "default");
        assertNull(config.get("logger.org.apache.aries.blueprint"));
        assertNull(config.get("logger.org.apache.aries.blueprint.level"));
        assertFalse(LoggerLevels.getLevels(config).containsKey("org.apache.aries.blueprint"));
        // unknown logger
        LoggerLevels.setLevel(config, "org.foo", "DEFAULT");
        assertEquals(3, config.size());
    }

    @Test
    public void testInvalidLevel() {
        try {
            LoggerLevels.setLevel(config, "org.foo", "VERBOSE");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertNull(config.get("logger.org.foo"));
    }

    @Test
    public void testSetLevelUpdatesConfiguration() throws Exception {
        Configuration configuration = EasyMock.createMock(Configuration.class);
        EasyMock.expect(configuration.getProperties()).andReturn(config);
        configuration.update(config);
        EasyMock.expectLastCall();
        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configAdmin.getConfiguration(LoggerLevels.CONFIGURATION_PID, null)).andReturn(configuration);
        EasyMock.replay(configuration, configAdmin);

        LoggerLevels.setLevel(configAdmin, "org.foo", "DEBUG");
        EasyMock.verify(configuration, configAdmin);
        assertEquals("DEBUG", config.get("logger.org.foo"));
    }

    @Test
    public void testSetLevelCreatesConfiguration() throws Exception {
        Configuration configuration = EasyMock.createMock(Configuration.class);
        EasyMock.expect(configuration.getProperties()).andReturn(null);
        configuration.update(EasyMock.<Dictionary<String, ?>>anyObject());
        EasyMock.expectLastCall().andAnswer(() -> {
            Dictionary<?, ?> properties = (Dictionary<?, ?>) EasyMock.getCurrentArguments()[0];
            assertEquals("WARN", properties.get("level"));
            assertEquals(1, properties.size());
            return null;
        });
        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configAdmin.getConfiguration(LoggerLevels.CONFIGURATION_PID, null)).andReturn(configuration);
        EasyMock.replay(configuration, configAdmin);

        LoggerLevels.setLevel(configAdmin, null, "WARN");
        EasyMock.verify(configuration, configAdmin);
    }

    @Test
    public void testSetLevelReadOnlyConfiguration() throws Exception {
        Configuration configuration = EasyMock.createMock(Configuration.class);
        EasyMock.expect(configuration.getProperties()).andReturn(config);
        configuration.update(config);
        EasyMock.expectLastCall().andThrow(new UnsupportedOperationException());
        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configAdmin.getConfiguration(LoggerLevels.CONFIGURATION_PID, null)).andReturn(configuration);
        EasyMock.replay(configuration, configAdmin);
        try {
            LoggerLevels.setLevel(configAdmin, null, "WARN");
            fail("Exception expected");
        } catch (IllegalStateException e) {
            assertSame(UnsupportedOperationException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testGetLevelsWithoutConfiguration() throws Exception {
        ConfigurationAdmin configAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(configAdmin.listConfigurations("(service.pid=org.apache.karaf.logging)")).andReturn(null).times(2);
        EasyMock.replay(configAdmin);
        assertEquals(System.getProperty("karaf.log.level", "INFO"), LoggerLevels.getLevels(configAdmin).get("ROOT"));
        assertEquals(System.getProperty("karaf.log.level", "INFO"), LoggerLevels.getEffectiveLevel(configAdmin, "org.foo"));
        EasyMock.verify(configAdmin);
    }

}
