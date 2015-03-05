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
package org.apache.karaf.shell.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

public class SecuredCommandConfigTransformerTest {
    @Test
    public void testTransformation() throws Exception {
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("foo", "a,b,c");
        props.put("bar[/.*[a]+*/]", "d");
        props.put("bar", "e");
        props.put("zar[/.*HiThere*/]", "f");
        props.put("service.pid", SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "abc");
        Configuration commandConfig = mockConfiguration(props);

        Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("xxx", "yyy");
        props2.put("service.pid", SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "xyz.123");
        Configuration commandConfig2 = mockConfiguration(props2);

        Dictionary<String, Object> props3 = new Hashtable<String, Object>();
        props3.put("test", "toast");
        props3.put("service.pid", "xyz.123");
        Configuration otherConfig = mockConfiguration(props3);

        final Map<String, Configuration> configurations = new HashMap<String, Configuration>();

        ConfigurationAdmin ca = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(ca.listConfigurations(
                "(service.pid=" + SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "*)")).
                andReturn(new Configuration [] {commandConfig, commandConfig2, otherConfig}).anyTimes();
        EasyMock.expect(ca.getConfiguration(EasyMock.isA(String.class), EasyMock.<String>isNull())).andAnswer(new IAnswer<Configuration>() {
            public Configuration answer() throws Throwable {
                String pid = (String) EasyMock.getCurrentArguments()[0];
                Configuration c = configurations.get(pid);
                if (c == null) {
                    c = EasyMock.createMock(Configuration.class);

                    // Put some expectations in the various mocks
                    Dictionary<String, Object> m = new Hashtable<String, Object>();
                    if ("org.apache.karaf.service.acl.command.abc.foo".equals(pid)) {
                        m.put("service.guard", "(&(osgi.command.scope=abc)(osgi.command.function=foo))");
                        m.put("execute", "a,b,c");
                        m.put("foo", "a,b,c");
                    } else if ("org.apache.karaf.service.acl.command.abc.bar".equals(pid)) {
                        m.put("service.guard", "(&(osgi.command.scope=abc)(osgi.command.function=bar))");
                        m.put("execute[/.*/,/.*[a]+*/]", "d");
                        m.put("execute", "e");
                        m.put("bar[/.*[a]+*/]", "d");
                        m.put("bar", "e");
                    } else if ("org.apache.karaf.service.acl.command.abc.zar".equals(pid)) {
                        m.put("service.guard", "(&(osgi.command.scope=abc)(osgi.command.function=zar))");
                        m.put("execute[/.*/,/.*HiThere*/]", "f");
                        m.put("zar[/.*HiThere*/]", "f");
                    } else {
                        fail("Unexpected PID: " + pid);
                    }
                    m.put("*", "*");
                    c.update(m);
                    EasyMock.expectLastCall().once();

                    EasyMock.replay(c);
                    configurations.put(pid, c);
                }
                return c;
            }
        }).anyTimes();
        EasyMock.replay(ca);

        SecuredCommandConfigTransformer scct = new SecuredCommandConfigTransformer();
        scct.setConfigAdmin(ca);
        scct.init();

        assertEquals(3, configurations.size());

        boolean foundFoo = false;
        boolean foundBar = false;
        boolean foundZar = false;
        for (Map.Entry<String, Configuration> entry : configurations.entrySet()) {
            Configuration c = entry.getValue();
            EasyMock.verify(c);
            if ("org.apache.karaf.service.acl.command.abc.foo".equals(entry.getKey())) {
                foundFoo = true;
            } else if ("org.apache.karaf.service.acl.command.abc.bar".equals(entry.getKey())) {
                foundBar = true;
            } else if ("org.apache.karaf.service.acl.command.abc.zar".equals(entry.getKey())) {
                foundZar = true;
            }
        }

        assertTrue(foundFoo);
        assertTrue(foundBar);
        assertTrue(foundZar);

    }

    Configuration mockConfiguration(Dictionary<String, Object> props) {
        Configuration commandConfig = EasyMock.createMock(Configuration.class);
        EasyMock.expect(commandConfig.getPid()).
                andReturn((String) props.get(Constants.SERVICE_PID)).anyTimes();
        EasyMock.expect(commandConfig.getProperties()).andReturn(props).anyTimes();
        EasyMock.replay(commandConfig);
        return commandConfig;
    }

    @Test
    public void testConfigurationEventAdded() throws Exception {
        String testPid = SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "test123";
        Configuration conf = EasyMock.createMock(Configuration.class);
        EasyMock.expect(conf.getPid()).andReturn(testPid).anyTimes();
        EasyMock.replay(conf);

        ConfigurationAdmin cm = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(cm.listConfigurations(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        EasyMock.expect(cm.getConfiguration(testPid, null)).andReturn(conf).anyTimes();
        EasyMock.replay(cm);

        final List<String> generateCalled = new ArrayList<String>();
        SecuredCommandConfigTransformer scct = new SecuredCommandConfigTransformer() {
            @Override
            void generateServiceGuardConfig(Configuration config) throws IOException {
                generateCalled.add(config.getPid());
            }
        };
        scct.setConfigAdmin(cm);
        scct.init();

        @SuppressWarnings("unchecked")
        ServiceReference<ConfigurationAdmin> cmRef = EasyMock.createMock(ServiceReference.class);
        EasyMock.expect(cmRef.getBundle()).andReturn(null).anyTimes();
        EasyMock.replay(cmRef);

        ConfigurationEvent event = new ConfigurationEvent(cmRef, ConfigurationEvent.CM_UPDATED, null, testPid);
        

        assertEquals("Precondition", 0, generateCalled.size());
        scct.configurationEvent(event);
        assertEquals(1, generateCalled.size());
        assertEquals(testPid, generateCalled.iterator().next());
    }

    @Test
    public void testConfigurationEventAddedNonCommand() throws Exception {
        ConfigurationAdmin cm = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(cm.listConfigurations(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(cm);

        SecuredCommandConfigTransformer scct = new SecuredCommandConfigTransformer();
        scct.setConfigAdmin(cm);
        scct.init();

        @SuppressWarnings("unchecked")
        ServiceReference<ConfigurationAdmin> cmRef = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(cmRef);
        ConfigurationEvent event = new ConfigurationEvent(cmRef, ConfigurationEvent.CM_UPDATED, null, "test123");

        scct.configurationEvent(event);
        EasyMock.verify(cm); // Ensure that this doesn't cause any unwanted calls on ConfigAdmin
    }

    @Test
    public void testConfigurationEventDeleted() throws Exception {
        String testPid = SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "test123";

        ConfigurationAdmin cm = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(cm.listConfigurations(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(cm);

        SecuredCommandConfigTransformer scct = new SecuredCommandConfigTransformer();
        scct.setConfigAdmin(cm);
        scct.init();

        @SuppressWarnings("unchecked")
        ServiceReference<ConfigurationAdmin> cmRef = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(cmRef);
        ConfigurationEvent event = new ConfigurationEvent(cmRef, ConfigurationEvent.CM_DELETED, null, testPid);

        Configuration c1 = EasyMock.createMock(Configuration.class);
        c1.delete();
        EasyMock.expectLastCall().once();
        EasyMock.replay(c1);
        Configuration c2 = EasyMock.createMock(Configuration.class);
        c2.delete();
        EasyMock.expectLastCall().once();
        EasyMock.replay(c2);

        EasyMock.reset(cm);
        EasyMock.expect(cm.listConfigurations("(service.pid=org.apache.karaf.service.acl.command.test123.*)")).
                andReturn(new Configuration[] {c1, c2}).once();
        EasyMock.replay(cm);

        scct.configurationEvent(event);

        EasyMock.verify(cm);
        EasyMock.verify(c1);
        EasyMock.verify(c2);
    }

    @Test
    public void testConfigurationEventDeletedNonScope() throws Exception {
        String testPid = SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "abc.def";

        ConfigurationAdmin cm = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(cm.listConfigurations(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(cm);

        SecuredCommandConfigTransformer scct = new SecuredCommandConfigTransformer();
        scct.setConfigAdmin(cm);
        scct.init();

        @SuppressWarnings("unchecked")
        ServiceReference<ConfigurationAdmin> cmRef = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(cmRef);
        ConfigurationEvent event = new ConfigurationEvent(cmRef, ConfigurationEvent.CM_DELETED, null, testPid);

        EasyMock.reset(cm);
        // Do not expect any further calls to cm...
        EasyMock.replay(cm);

        scct.configurationEvent(event);
        EasyMock.verify(cm);
    }

    @Test
    public void testConfigurationLocationChangedEventNoEffect() throws Exception {
        String testPid = SecuredCommandConfigTransformer.PROXY_COMMAND_ACL_PID_PREFIX + "test123";

        ConfigurationAdmin cm = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(cm.listConfigurations(EasyMock.isA(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(cm);

        SecuredCommandConfigTransformer scct = new SecuredCommandConfigTransformer();
        scct.setConfigAdmin(cm);
        scct.init();

        @SuppressWarnings("unchecked")
        ServiceReference<ConfigurationAdmin> cmRef = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(cmRef);
        ConfigurationEvent event = new ConfigurationEvent(cmRef, ConfigurationEvent.CM_LOCATION_CHANGED, null, testPid);

        scct.configurationEvent(event);
        EasyMock.verify(cm); // Ensure that this doesn't cause any unwanted calls on ConfigAdmin
    }
}
