/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.action.command;

import java.util.Hashtable;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Config;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Registry;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class ManagerImplConfigTest {

    @Command(scope = "test", name = "config-single")
    @Service
    public static class SingleConfigCommand implements Action {
        @Config(pid = "my.pid")
        Map<String, Object> config;

        @Override
        public Object execute() throws Exception {
            return null;
        }
    }

    @Command(scope = "test", name = "config-multi")
    @Service
    public static class MultiConfigCommand implements Action {
        @Config(pid = "pid.one")
        Map<String, Object> configOne;

        @Config(pid = "pid.two")
        Map<String, Object> configTwo;

        @Override
        public Object execute() throws Exception {
            return null;
        }
    }

    @Command(scope = "test", name = "config-empty")
    @Service
    public static class EmptyConfigCommand implements Action {
        @Config(pid = "nonexistent.pid")
        Map<String, Object> config;

        @Override
        public Object execute() throws Exception {
            return null;
        }
    }

    @Test
    public void testConfigInjection() throws Exception {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("key1", "value1");
        props.put("key2", 42);

        Configuration configuration = createMock(Configuration.class);
        expect(configuration.getProperties()).andReturn(props);
        replay(configuration);

        ConfigurationAdmin configAdmin = createMock(ConfigurationAdmin.class);
        expect(configAdmin.getConfiguration("my.pid", "?")).andReturn(configuration);
        replay(configAdmin);

        Registry registry = createMock(Registry.class);
        expect(registry.getService(ConfigurationAdmin.class)).andReturn(configAdmin);
        replay(registry);

        ManagerImpl manager = new ManagerImpl(registry, registry, true);
        SingleConfigCommand cmd = manager.instantiate(SingleConfigCommand.class, registry);

        assertNotNull(cmd.config);
        assertEquals("value1", cmd.config.get("key1"));
        assertEquals(42, cmd.config.get("key2"));
        assertEquals(2, cmd.config.size());

        verify(configuration, configAdmin, registry);
    }

    @Test
    public void testConfigInjectionWithNullProperties() throws Exception {
        Configuration configuration = createMock(Configuration.class);
        expect(configuration.getProperties()).andReturn(null);
        replay(configuration);

        ConfigurationAdmin configAdmin = createMock(ConfigurationAdmin.class);
        expect(configAdmin.getConfiguration("nonexistent.pid", "?")).andReturn(configuration);
        replay(configAdmin);

        Registry registry = createMock(Registry.class);
        expect(registry.getService(ConfigurationAdmin.class)).andReturn(configAdmin);
        replay(registry);

        ManagerImpl manager = new ManagerImpl(registry, registry, true);
        EmptyConfigCommand cmd = manager.instantiate(EmptyConfigCommand.class, registry);

        assertNotNull(cmd.config);
        assertTrue(cmd.config.isEmpty());

        verify(configuration, configAdmin, registry);
    }

    @Test
    public void testConfigInjectionWithoutConfigAdmin() throws Exception {
        Registry registry = createMock(Registry.class);
        expect(registry.getService(ConfigurationAdmin.class)).andReturn(null);
        replay(registry);

        ManagerImpl manager = new ManagerImpl(registry, registry, true);
        SingleConfigCommand cmd = manager.instantiate(SingleConfigCommand.class, registry);

        assertNotNull(cmd.config);
        assertTrue(cmd.config.isEmpty());

        verify(registry);
    }

    @Test
    public void testMultipleConfigInjection() throws Exception {
        Hashtable<String, Object> props1 = new Hashtable<>();
        props1.put("host", "localhost");
        props1.put("port", 8080);

        Hashtable<String, Object> props2 = new Hashtable<>();
        props2.put("timeout", 30000L);

        Configuration config1 = createMock(Configuration.class);
        expect(config1.getProperties()).andReturn(props1);
        replay(config1);

        Configuration config2 = createMock(Configuration.class);
        expect(config2.getProperties()).andReturn(props2);
        replay(config2);

        ConfigurationAdmin configAdmin = createMock(ConfigurationAdmin.class);
        expect(configAdmin.getConfiguration("pid.one", "?")).andReturn(config1);
        expect(configAdmin.getConfiguration("pid.two", "?")).andReturn(config2);
        replay(configAdmin);

        Registry registry = createMock(Registry.class);
        expect(registry.getService(ConfigurationAdmin.class)).andReturn(configAdmin);
        replay(registry);

        ManagerImpl manager = new ManagerImpl(registry, registry, true);
        MultiConfigCommand cmd = manager.instantiate(MultiConfigCommand.class, registry);

        assertNotNull(cmd.configOne);
        assertEquals("localhost", cmd.configOne.get("host"));
        assertEquals(8080, cmd.configOne.get("port"));
        assertEquals(2, cmd.configOne.size());

        assertNotNull(cmd.configTwo);
        assertEquals(30000L, cmd.configTwo.get("timeout"));
        assertEquals(1, cmd.configTwo.size());

        verify(config1, config2, configAdmin, registry);
    }

    @Test
    public void testConfigInjectionWithConfigAdminException() throws Exception {
        ConfigurationAdmin configAdmin = createMock(ConfigurationAdmin.class);
        expect(configAdmin.getConfiguration("my.pid", "?")).andThrow(new java.io.IOException("config error"));
        replay(configAdmin);

        Registry registry = createMock(Registry.class);
        expect(registry.getService(ConfigurationAdmin.class)).andReturn(configAdmin);
        replay(registry);

        ManagerImpl manager = new ManagerImpl(registry, registry, true);
        SingleConfigCommand cmd = manager.instantiate(SingleConfigCommand.class, registry);

        // Should get empty map when ConfigAdmin throws
        assertNotNull(cmd.config);
        assertTrue(cmd.config.isEmpty());

        verify(configAdmin, registry);
    }

    @Test
    public void testConfigInjectionFallsBackToDependencies() throws Exception {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("key", "value");

        Configuration configuration = createMock(Configuration.class);
        expect(configuration.getProperties()).andReturn(props);
        replay(configuration);

        ConfigurationAdmin configAdmin = createMock(ConfigurationAdmin.class);
        expect(configAdmin.getConfiguration("my.pid", "?")).andReturn(configuration);
        replay(configAdmin);

        // Local registry returns null, dependencies registry has ConfigAdmin
        Registry localRegistry = createMock(Registry.class);
        expect(localRegistry.getService(ConfigurationAdmin.class)).andReturn(null);
        replay(localRegistry);

        Registry dependenciesRegistry = createMock(Registry.class);
        expect(dependenciesRegistry.getService(ConfigurationAdmin.class)).andReturn(configAdmin);
        replay(dependenciesRegistry);

        ManagerImpl manager = new ManagerImpl(dependenciesRegistry, dependenciesRegistry, true);
        SingleConfigCommand cmd = manager.instantiate(SingleConfigCommand.class, localRegistry);

        assertNotNull(cmd.config);
        assertEquals("value", cmd.config.get("key"));

        verify(configuration, configAdmin, localRegistry, dependenciesRegistry);
    }
}
