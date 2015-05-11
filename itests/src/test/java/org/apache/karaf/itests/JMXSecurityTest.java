/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JMXSecurityTest extends KarafTestSupport {
    private static AtomicInteger counter = new AtomicInteger(0);

    @Configuration
    public Option[] config() {
        List<Option> options = new ArrayList<Option>(Arrays.asList(super.config()));

        // Add some extra options used by this test...
        options.addAll(Arrays.asList(
            editConfigurationFilePut("etc/jmx.acl.org.apache.karaf.services.cfg", "getServices(boolean)", "viewer"),
            editConfigurationFilePut("etc/jmx.acl.org.apache.karaf.services.cfg", "getServices(long)", "manager"),
            editConfigurationFilePut("etc/jmx.acl.org.apache.karaf.services.cfg", "getServices(long,boolean)", "admin")));
        return options.toArray(new Option[] {});
    }

    @Test
    public void testJMXSecurityAsViewer() throws Exception {
        String suffix = "_" + counter.incrementAndGet();
        String managerUser = "managerUser" + System.currentTimeMillis() + suffix;
        String managerGroup = "managerGroup" + System.currentTimeMillis() + suffix;
        String viewerUser = "viewerUser" + System.currentTimeMillis() + suffix;

        // Create a viewer user and a manager user
        System.out.println(executeCommand("jaas:manage --realm karaf" +
            ";jaas:useradd " + managerUser + " " + managerUser +
            ";jaas:groupadd " + managerUser + " " + managerGroup +
            ";jaas:grouproleadd " + managerGroup + " viewer" +
            ";jaas:grouproleadd " + managerGroup + " manager" +
            ";jaas:useradd " + viewerUser + " " + viewerUser +
            ";jaas:roleadd " + viewerUser + " viewer" +
            ";jaas:update" +
            ";jaas:manage --realm karaf" +
            ";jaas:users", new RolePrincipal("admin")));

        JMXConnector connector = getJMXConnector(viewerUser, viewerUser);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName systemMBean = new ObjectName("org.apache.karaf:type=system,name=root");

        assertEquals(100, connection.getAttribute(systemMBean, "StartLevel"));
        assertSetAttributeSecEx(connection, systemMBean, new Attribute("StartLevel", 101));
        assertEquals("Changing the start level should have no effect for a viewer",
               100, connection.getAttribute(systemMBean, "StartLevel"));
        assertInvokeSecEx(connection, systemMBean, "halt");

        ObjectName memoryMBean = new ObjectName("java.lang:type=Memory");
        assertEquals(false, connection.getAttribute(memoryMBean, "Verbose"));
        assertSetAttributeSecEx(connection, memoryMBean, new Attribute("Verbose", true));
        assertEquals("Changing the verbosity should have no effect for a viewer",
                false, connection.getAttribute(memoryMBean, "Verbose"));
        assertInvokeSecEx(connection, memoryMBean, "gc");

        testJMXSecurityMBean(connection, false, false);
        testKarafConfigAdminMBean(connection, false, false);
        testOSGiConfigAdminMBean(connection, false, false);
    }

    @Test
    public void testJMXSecurityAsManager() throws Exception {
        String suffix = "_" + counter.incrementAndGet();
        String managerUser = "managerUser" + System.currentTimeMillis() + suffix;
        String managerGroup = "managerGroup" + System.currentTimeMillis() + suffix;
        String viewerUser = "viewerUser" + System.currentTimeMillis() + suffix;

        // Create a viewer user and a manager user
        System.out.println(executeCommand("jaas:manage --realm karaf" +
            ";jaas:useradd " + managerUser + " " + managerUser +
            ";jaas:groupadd " + managerUser + " " + managerGroup +
            ";jaas:grouproleadd " + managerGroup + " viewer" +
            ";jaas:grouproleadd " + managerGroup + " manager" +
            ";jaas:useradd " + viewerUser + " " + viewerUser +
            ";jaas:roleadd " + viewerUser + " viewer" +
            ";jaas:update" +
            ";jaas:manage --realm karaf" +
            ";jaas:users", new RolePrincipal("admin")));

        JMXConnector connector = getJMXConnector(managerUser, managerUser);
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName systemMBean = new ObjectName("org.apache.karaf:type=system,name=root");

        assertEquals(100, connection.getAttribute(systemMBean, "StartLevel"));
        assertSetAttributeSecEx(connection, systemMBean, new Attribute("StartLevel", 101));
        assertEquals("Changing the start level should have no effect for a viewer",
               100, connection.getAttribute(systemMBean, "StartLevel"));
        assertInvokeSecEx(connection, systemMBean, "halt");

        ObjectName memoryMBean = new ObjectName("java.lang:type=Memory");
        assertEquals(false, connection.getAttribute(memoryMBean, "Verbose"));
        assertSetAttributeSecEx(connection, memoryMBean, new Attribute("Verbose", true));
        assertEquals("Changing the verbosity should have no effect for a viewer",
                false, connection.getAttribute(memoryMBean, "Verbose"));
        connection.invoke(memoryMBean, "gc", new Object [] {}, new String [] {});

        testJMXSecurityMBean(connection, true, false);
        testKarafConfigAdminMBean(connection, true, false);
        testOSGiConfigAdminMBean(connection, true, false);
    }

    @Test
    public void testJMXSecurityAsAdmin() throws Exception {
        JMXConnector connector = getJMXConnector();
        MBeanServerConnection connection = connector.getMBeanServerConnection();
        ObjectName systemMBean = new ObjectName("org.apache.karaf:type=system,name=root");

        assertEquals(100, connection.getAttribute(systemMBean, "StartLevel"));
        try {
            connection.setAttribute(systemMBean, new Attribute("StartLevel", 101));
            assertGetAttributeRetry("Start level should have been changed",
                    101, connection, systemMBean, "StartLevel");
        } finally {
            connection.setAttribute(systemMBean, new Attribute("StartLevel", 100));
        }
        assertGetAttributeRetry("Start level should be changed back now",
               100, connection, systemMBean, "StartLevel");

        ObjectName memoryMBean = new ObjectName("java.lang:type=Memory");
        assertEquals(false, connection.getAttribute(memoryMBean, "Verbose"));
        try {
            connection.setAttribute(memoryMBean, new Attribute("Verbose", true));
            assertEquals(true, connection.getAttribute(memoryMBean, "Verbose"));
        } finally {
            connection.setAttribute(memoryMBean, new Attribute("Verbose", false));
        }
        assertEquals("Verbosity should be changed back to false",
                false, connection.getAttribute(memoryMBean, "Verbose"));
        connection.invoke(memoryMBean, "gc", new Object [] {}, new String [] {});

        testJMXSecurityMBean(connection, true, true);
        testKarafConfigAdminMBean(connection, true, true);
        testOSGiConfigAdminMBean(connection, true, true);
    }

    @Test
    public void testJMXSecurityCannotLogInAsGroupDirectly() throws Exception {
        String suffix = "_" + counter.incrementAndGet();
        String managerUser = "managerUser" + System.currentTimeMillis() + suffix;
        String managerGroup = "managerGroup" + System.currentTimeMillis() + suffix;
        String viewerUser = "viewerUser" + System.currentTimeMillis() + suffix;

        System.out.println(executeCommand("jaas:manage --realm karaf" +
            ";jaas:useradd " + managerUser + " " + managerUser +
            ";jaas:groupadd " + managerUser + " " + managerGroup +
            ";jaas:grouproleadd " + managerGroup + " viewer" +
            ";jaas:grouproleadd " + managerGroup + " manager" +
            ";jaas:useradd " + viewerUser + " " + viewerUser +
            ";jaas:roleadd " + viewerUser + " viewer" +
            ";jaas:update" +
            ";jaas:manage --realm karaf" +
            ";jaas:users", new RolePrincipal("admin")));

        try {
            getJMXConnector("admingroup", "group");
            fail("Login with a group name should have failed");
        } catch (SecurityException se) {
            // good
        }
        try {
            getJMXConnector("_g_:admingroup", "group");
            fail("Login with a group name should have failed");
        } catch (SecurityException se) {
            // good
        }
        try {
            getJMXConnector(managerGroup, "group");
            fail("Login with a group name should have failed");
        } catch (SecurityException se) {
            // good
        }
        try {
            getJMXConnector("_g_:" + managerGroup, "group");
            fail("Login with a group name should have failed");
        } catch (SecurityException se) {
            // good
        }
    }

    private void testJMXSecurityMBean(MBeanServerConnection connection, boolean isManager, boolean isAdmin)
            throws MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        ObjectName securityMBean = new ObjectName("org.apache.karaf:type=security,area=jmx,name=root");

        ObjectName systemMBean = new ObjectName("org.apache.karaf:type=system,name=root");
        assertTrue((Boolean) connection.invoke(securityMBean, "canInvoke",
                new Object [] {systemMBean.toString()},
                new String [] {String.class.getName()}));

        assertTrue((Boolean) connection.invoke(securityMBean, "canInvoke",
                new Object [] {systemMBean.toString(), "getStartLevel"},
                new String [] {String.class.getName(), String.class.getName()}));
        assertEquals(isAdmin, connection.invoke(securityMBean, "canInvoke",
                new Object [] {systemMBean.toString(), "setStartLevel"},
                new String [] {String.class.getName(), String.class.getName()}));
        assertEquals(isAdmin, connection.invoke(securityMBean, "canInvoke",
                new Object [] {systemMBean.toString(), "halt"},
                new String [] {String.class.getName(), String.class.getName()}));

        ObjectName serviceMBean = new ObjectName("org.apache.karaf:type=services,name=root");
        assertTrue((Boolean) connection.invoke(securityMBean, "canInvoke",
                new Object [] {serviceMBean.toString(), "getServices", new String [] {boolean.class.getName()}},
                new String [] {String.class.getName(), String.class.getName(), String[].class.getName()}));
        assertEquals(isManager, connection.invoke(securityMBean, "canInvoke",
                new Object [] {serviceMBean.toString(), "getServices", new String [] {long.class.getName()}},
                new String [] {String.class.getName(), String.class.getName(), String[].class.getName()}));
        assertEquals(isAdmin, connection.invoke(securityMBean, "canInvoke",
                new Object [] {serviceMBean.toString(), "getServices", new String [] {long.class.getName(), boolean.class.getName()}},
                new String [] {String.class.getName(), String.class.getName(), String[].class.getName()}));

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        TabularData td = (TabularData) connection.invoke(securityMBean, "canInvoke", new Object [] {map}, new String [] {Map.class.getName()});
        assertEquals(0, td.size());

        Map<String, List<String>> map2 = new HashMap<String, List<String>>();
        map2.put(systemMBean.toString(), Collections.<String>emptyList());
        map2.put(serviceMBean.toString(), Arrays.asList("getServices(boolean)", "getServices(long)", "getServices(long,boolean)", "getServices()"));
        TabularData td2 = (TabularData) connection.invoke(securityMBean, "canInvoke", new Object [] {map2}, new String [] {Map.class.getName()});
        assertEquals(5, td2.size());

        CompositeData cd1 = td2.get(new Object [] {serviceMBean.toString(), "getServices(boolean)"});
        assertEquals(serviceMBean.toString(), cd1.get("ObjectName"));
        assertEquals("getServices(boolean)", cd1.get("Method"));
        assertTrue((Boolean) cd1.get("CanInvoke"));

        CompositeData cd2 = td2.get(new Object [] {serviceMBean.toString(), "getServices(long)"});
        assertEquals(serviceMBean.toString(), cd2.get("ObjectName"));
        assertEquals("getServices(long)", cd2.get("Method"));
        assertEquals(isManager, cd2.get("CanInvoke"));

        CompositeData cd3 = td2.get(new Object [] {serviceMBean.toString(), "getServices(long,boolean)"});
        assertEquals(serviceMBean.toString(), cd3.get("ObjectName"));
        assertEquals("getServices(long,boolean)", cd3.get("Method"));
        assertEquals(isAdmin, cd3.get("CanInvoke"));

        CompositeData cd5 = td2.get(new Object [] {systemMBean.toString(), ""});
        assertEquals(systemMBean.toString(), cd5.get("ObjectName"));
        assertEquals("", cd5.get("Method"));
        assertTrue((Boolean) cd5.get("CanInvoke"));

        Map<String, List<String>> map3 = new HashMap<String, List<String>>();
        map3.put(serviceMBean.toString(), Collections.singletonList("getServices"));
        TabularData td3 = (TabularData) connection.invoke(securityMBean, "canInvoke", new Object [] {map3}, new String [] {Map.class.getName()});
        assertEquals(1, td3.size());

        CompositeData cd6 = td3.get(new Object [] {serviceMBean.toString(), "getServices"});
        assertEquals(serviceMBean.toString(), cd6.get("ObjectName"));
        assertEquals("getServices", cd6.get("Method"));
        assertTrue((Boolean) cd6.get("CanInvoke"));

        Map<String, List<String>> map4 = new HashMap<String, List<String>>();
        map4.put(systemMBean.toString(), Collections.singletonList("halt"));
        TabularData td4 = (TabularData) connection.invoke(securityMBean, "canInvoke", new Object [] {map4}, new String [] {Map.class.getName()});
        assertEquals(1, td4.size());

        CompositeData cd7 = td4.get(new Object [] {systemMBean.toString(), "halt"});
        assertEquals(systemMBean.toString(), cd7.get("ObjectName"));
        assertEquals("halt", cd7.get("Method"));
        assertEquals(isAdmin, cd7.get("CanInvoke"));
    }

    private void testKarafConfigAdminMBean(MBeanServerConnection connection, boolean isManager, boolean isAdmin)
            throws MalformedObjectNameException, NullPointerException, InstanceNotFoundException, MBeanException, ReflectionException, IOException, AttributeNotFoundException {
        testKarafConfigAdminMBean(connection, "foo.bar", isManager);
        testKarafConfigAdminMBean(connection, "jmx.acl", isAdmin);
        testKarafConfigAdminMBean(connection, "org.apache.karaf.command.acl", isAdmin);
        testKarafConfigAdminMBean(connection, "org.apache.karaf.service.acl", isAdmin);
        testKarafConfigAdminMBean(connection, "org.apache.karaf.somethingelse", isManager);
    }

    private void testKarafConfigAdminMBean(MBeanServerConnection connection, String pidPrefix, boolean shouldSucceed)
            throws MalformedObjectNameException, InstanceNotFoundException, MBeanException, ReflectionException, IOException,
            AttributeNotFoundException {
        ObjectName mbean = new ObjectName("org.apache.karaf:type=config,name=root");

        String suffix = "." + System.currentTimeMillis() + "_" + counter.incrementAndGet();
        String pid = pidPrefix + suffix;

        assertJmxInvoke(shouldSucceed, connection, mbean, "create", new Object [] {pid}, new String [] {String.class.getName()});
        assertJmxInvoke(shouldSucceed, connection, mbean, "setProperty", new Object [] {pid, "x", "y"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        Map<?, ?> m1 = (Map<?, ?>) connection.invoke(mbean, "listProperties", new Object [] {pid}, new String [] {String.class.getName()});
        if (shouldSucceed)
            assertEquals("y", m1.get("x"));
        else
            assertNull(m1.get("x"));
        assertJmxInvoke(shouldSucceed, connection, mbean, "appendProperty", new Object [] {pid, "x", "z"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        Map<?, ?> m2 = (Map<?, ?>) connection.invoke(mbean, "listProperties", new Object [] {pid}, new String [] {String.class.getName()});
        if (shouldSucceed)
            assertEquals("yz", m2.get("x"));
        else
            assertNull(m2.get("x"));

        assertJmxInvoke(shouldSucceed, connection, mbean, "appendProperty", new Object [] {pid, "a.b.c", "abc"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        assertJmxInvoke(shouldSucceed, connection, mbean, "appendProperty", new Object [] {pid, "d.e.f", "def"}, new String [] {String.class.getName(), String.class.getName(), String.class.getName()});
        assertJmxInvoke(shouldSucceed, connection, mbean, "deleteProperty", new Object [] {pid, "d.e.f"}, new String [] {String.class.getName(), String.class.getName()});
        Map<?, ?> m3 = (Map<?, ?>) connection.invoke(mbean, "listProperties", new Object [] {pid}, new String [] {String.class.getName()});
        if (shouldSucceed) {
            assertEquals("abc", m3.get("a.b.c"));
            assertNull(m3.get("d.e.f"));
            assertTrue(((List<?>) connection.getAttribute(mbean, "Configs")).contains(pid));
        } else {
            assertNull(m3.get("a.b.c"));
        }
        assertJmxInvoke(shouldSucceed, connection, mbean, "delete", new Object [] {pid}, new String [] {String.class.getName()});
        assertFalse(((List<?>) connection.getAttribute(mbean, "Configs")).contains(pid));
    }

    private void testOSGiConfigAdminMBean(MBeanServerConnection connection, boolean isManager, boolean isAdmin) throws Exception {
        boolean found = false;

        // Find the OSGi Config Admin MBean(s) based on the Object Name pattern
        for (ObjectName name : connection.queryNames(new ObjectName("osgi.compendium:service=cm,*"), null)) {
            found = true;
            testOSGiConfigAdminMBean(connection, name, "foo.bar", isManager, isAdmin);
            testOSGiConfigAdminMBean(connection, name, "jmx.acl", isAdmin, isAdmin);
            testOSGiConfigAdminMBean(connection, name, "org.apache.karaf.command.acl", isAdmin, isAdmin);
            testOSGiConfigAdminMBean(connection, name, "org.apache.karaf.service.acl", isAdmin, isAdmin);
            testOSGiConfigAdminMBean(connection, name, "org.apache.karaf.somethingelse", isManager, isAdmin);
        }
        assertTrue("Should be at least one ConfigAdmin MBean", found);
    }

    private void testOSGiConfigAdminMBean(MBeanServerConnection connection, ObjectName mbean, String pidPrefix, boolean shouldSucceed, boolean isAdmin) throws Exception {
        String infix = "." + System.currentTimeMillis();
        String suffix = infix + "_" + counter.incrementAndGet();
        String pid = pidPrefix + suffix;

        CompositeType ct = new CompositeType("PROPERTY", "X",
                new String[] {"Key", "Value", "Type"},
                new String[] {"X", "X", "X"},
                new OpenType<?>[] {SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tt = new TabularType("PROPERTIES", "X", ct, new String [] {"Key"});

        TabularDataSupport tds = new TabularDataSupport(tt);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("Key", "foo");
        data.put("Value", "bar");
        data.put("Type", "String");
        CompositeDataSupport cds = new CompositeDataSupport(ct, data);
        tds.put(cds);

        assertJmxInvoke(shouldSucceed, connection, mbean, "update", new Object [] {pid, tds}, new String [] {String.class.getName(), TabularData.class.getName()});
        TabularData td = (TabularData) connection.invoke(mbean, "getProperties", new Object [] {pid}, new String [] {String.class.getName()});
        if (shouldSucceed) {
            assertEquals("bar", td.get(new Object[] {"foo"}).get("Value"));
        }

        String[][] configs = (String[][]) connection.invoke(mbean, "getConfigurations", new Object [] {"(service.pid=" + pid + ")"}, new String [] {String.class.getName()});
        if (shouldSucceed) {
            assertEquals(1, configs.length);
            assertEquals(pid, configs[0][0]);
        }
        assertJmxInvoke(shouldSucceed, connection, mbean, "delete", new Object [] {pid}, new String [] {String.class.getName()});

        TabularDataSupport tds2 = new TabularDataSupport(tt);
        Map<String, Object> data2 = new HashMap<String, Object>();
        data2.put("Key", "a.b.c");
        data2.put("Value", "d.e.f");
        data2.put("Type", "String");
        CompositeDataSupport cds2 = new CompositeDataSupport(ct, data2);
        tds2.put(cds2);

        String suffix2 = infix + "_" + counter.incrementAndGet();
        String pid2 = pidPrefix + suffix2;
        String location2 = "mylocation" + System.currentTimeMillis();
        assertJmxInvoke(shouldSucceed, connection, mbean, "updateForLocation", new Object [] {pid2, location2, tds2}, new String [] {String.class.getName(), String.class.getName(), TabularData.class.getName()});
        TabularData td2 = (TabularData) connection.invoke(mbean, "getPropertiesForLocation", new Object [] {pid2, location2}, new String [] {String.class.getName(), String.class.getName()});
        if (shouldSucceed) {
            assertEquals("d.e.f", td2.get(new Object[] {"a.b.c"}).get("Value"));
        }
        assertJmxInvoke(shouldSucceed, connection, mbean, "deleteForLocation", new Object [] {pid2, location2}, new String [] {String.class.getName(), String.class.getName()});

        if (isAdmin) {
            String suffix3 = infix + "_" + counter.incrementAndGet();
            String pid3 = pidPrefix + suffix3;

            TabularDataSupport tds3 = new TabularDataSupport(tt);
            assertJmxInvoke(shouldSucceed, connection, mbean, "update", new Object [] {pid3, tds3}, new String [] {String.class.getName(), TabularData.class.getName()});
            String[][] configs2 = (String[][]) connection.invoke(mbean, "getConfigurations", new Object [] {"(service.pid=" + pidPrefix + infix + "*)"}, new String [] {String.class.getName()});
            assertEquals(1, configs2.length);
            assertEquals(pid3, configs2[0][0]);
            String location3 = "my.other.location." + System.currentTimeMillis();
            assertJmxInvoke(shouldSucceed, connection, mbean, "setBundleLocation", new Object [] {pid3, location3}, new String [] {String.class.getName(), String.class.getName()});
            String[][] configs3 = (String[][]) connection.invoke(mbean, "getConfigurations", new Object [] {"(service.pid=" + pidPrefix + infix + "*)"}, new String [] {String.class.getName()});
            assertEquals(1, configs3.length);
            assertEquals(pid3, configs3[0][0]);
            connection.invoke(mbean, "deleteConfigurations", new Object [] {"(service.pid=" + pid3 + ")"}, new String [] {String.class.getName()});
            assertEquals(0, ((String[][]) connection.invoke(mbean, "getConfigurations", new Object [] {"(service.pid=" + pidPrefix + infix + "*)"}, new String [] {String.class.getName()})).length);
        }
    }

    private void assertGetAttributeRetry(String explanation, Object expected, MBeanServerConnection connection, ObjectName mbean, String attrName)
            throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        int count = 5;
        while (count > 0) {
            count--;
            try {
                assertEquals(explanation, expected, connection.getAttribute(mbean, attrName));
                return;
            } catch (AssertionError ae) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
            }
        }
        assertEquals(explanation, expected, connection.getAttribute(mbean, attrName));
    }

    private Object assertJmxInvoke(boolean expectSuccess, MBeanServerConnection connection, ObjectName mbean, String method,
            Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        try {
            Object result = connection.invoke(mbean, method, params, signature);
            assertTrue(expectSuccess);
            return result;
        } catch (SecurityException se) {
            assertFalse(expectSuccess);
            return null;
        }
    }

    private void assertSetAttributeSecEx(MBeanServerConnection connection, ObjectName mbeanObjectName,
            Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
        try {
            connection.setAttribute(mbeanObjectName, attribute);
            fail("Expecting a SecurityException");
        } catch (SecurityException se) {
            // good
        }
    }

    private void assertInvokeSecEx(MBeanServerConnection connection, ObjectName mbeanObjectName,
            String method) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        try {
            connection.invoke(mbeanObjectName, method, new Object[] {}, new String [] {});
            fail("Expecting a SecurityException");
        } catch (SecurityException se) {
            // good
        }
    }
}