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
package org.apache.karaf.management;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import junit.framework.TestCase;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.easymock.EasyMock;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class KarafMBeanServerGuardTest extends TestCase {

    public void testRequiredRolesMethodNameOnly() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit", "master");
        configuration.put("fryit", "editor,viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("master"),
                guard.getRequiredRoles(on, "doit", new Object[]{}, new String[]{}));
        assertEquals(Arrays.asList("editor", "viewer"),
                guard.getRequiredRoles(on, "fryit", new Object[]{"blah"}, new String[]{"java.lang.String"}));
    }

    @SuppressWarnings("unchecked")
    public void testRequiredRolesMethodNameEmpty() throws Exception {
        Dictionary<String, Object> conf1 = new Hashtable<String, Object>();
        conf1.put("doit", "");
        conf1.put("fryit", "editor, viewer");
        conf1.put(Constants.SERVICE_PID, "jmx.acl.foo.bar.Test");
        Dictionary<String, Object> conf2 = new Hashtable<String, Object>();
        conf2.put("doit", "editor");
        conf2.put(Constants.SERVICE_PID, "jmx.acl.foo.bar");
        ConfigurationAdmin ca = getMockConfigAdmin2(conf1, conf2);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.emptyList(), guard.getRequiredRoles(on, "doit", new Object[]{}, new String[]{}));
        assertEquals(Arrays.asList("editor", "viewer"),
                guard.getRequiredRoles(on, "fryit", new Object[]{"blah"}, new String[]{"java.lang.String"}));
    }

    public void testRequiredRolesSignature() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("testit", "master");
        configuration.put("testit(java.lang.String)", "viewer");
        configuration.put("testit(java.lang.String, java.lang.String)", "editor");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("editor"),
                guard.getRequiredRoles(on, "testit", new Object[]{"test", "toast"}, new String[]{"java.lang.String", "java.lang.String"}));
    }

    public void testRequiredRolesSignatureEmpty() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("testit", "master");
        configuration.put("testit(java.lang.String)", "viewer");
        configuration.put("testit(java.lang.String, java.lang.String)", "");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "testit", new Object[]{"test", "toast"}, new String[]{"java.lang.String", "java.lang.String"}));
    }

    public void testRequiredRolesExact() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("testit", "master");
        configuration.put("testit(java.lang.String)", "viewer");
        configuration.put("testit(java.lang.String, java.lang.String)", "editor");
        configuration.put("testit(java.lang.String) [\"ab\"]", "manager");
        configuration.put("testit(java.lang.String)[\"a b\" ]", "admin");
        configuration.put("testit(java.lang.String)[ \"cd\"]  ", "tester");
        configuration.put("testit(java.lang.String)[\"cd/\"]", "monkey");
        configuration.put("testit(java.lang.String)[\"cd\"\"]", "donkey");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("manager"),
                guard.getRequiredRoles(on, "testit", new Object[]{"ab"}, new String[]{"java.lang.String"}));
        assertEquals(Collections.singletonList("admin"),
                guard.getRequiredRoles(on, "testit", new Object[]{" a b "}, new String[]{"java.lang.String"}));
        assertEquals("The arguments are trimmed before checking",
                Collections.singletonList("admin"),
                guard.getRequiredRoles(on, "testit", new Object[]{"a b"}, new String[]{"java.lang.String"}));
        assertEquals(Collections.singletonList("tester"),
                guard.getRequiredRoles(on, "testit", new Object[]{"cd"}, new String[]{"java.lang.String"}));
        assertEquals(Collections.singletonList("monkey"),
                guard.getRequiredRoles(on, "testit", new Object[]{"cd/"}, new String[]{"java.lang.String"}));
        assertEquals(Collections.singletonList("donkey"),
                guard.getRequiredRoles(on, "testit", new Object[]{"cd\""}, new String[]{"java.lang.String"}));
    }

    public void testRequiredRolesExact2() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("foo(java.lang.String,java.lang.String)[\"a\",\",\"]", "editor #this is the editor rule");
        configuration.put("foo(java.lang.String,java.lang.String)[\",\" , \"a\"]", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("editor"),
                guard.getRequiredRoles(on, "foo", new Object[]{"a", ","}, new String[]{"java.lang.String", "java.lang.String"}));
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "foo", new Object[]{",", "a"}, new String[]{"java.lang.String", "java.lang.String"}));
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "foo", new Object[]{"a", "a"}, new String[]{"java.lang.String", "java.lang.String"}));
    }

    public void testRequiredRolesNumeric() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("bar(int)[\"17\"]", "editor #this is the editor rule");
        configuration.put("bar", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("editor"),
                guard.getRequiredRoles(on, "bar", new Object[]{new Integer(17)}, new String[]{"int"}));
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "bar", new Object[]{new Integer(18)}, new String[]{"int"}));
    }

    public void testRequiredRolesExactNobody() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("foo(java.lang.String)[\"a\"]", "");
        configuration.put("foo(java.lang.String)[\"aa\"]", "#hello");
        configuration.put("foo", "test");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "foo", new Object[]{"a"}, new String[]{"java.lang.String"}));
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "foo", new Object[]{"aa"}, new String[]{"java.lang.String"}));
    }

    public void testRequiredRolesRegExp() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("  testit   (java.lang.String)  [  /ab/]", "manager");
        configuration.put("testit(java.lang.String)[/c\"d/]", "tester");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("manager"),
                guard.getRequiredRoles(on, "testit", new Object[]{"ab"}, new String[]{"java.lang.String"}));
        assertEquals(Collections.singletonList("manager"),
                guard.getRequiredRoles(on, "testit", new Object[]{"    ab  "}, new String[]{"java.lang.String"}));
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "testit", new Object[]{" a b "}, new String[]{"java.lang.String"}));
        assertEquals(Collections.singletonList("tester"),
                guard.getRequiredRoles(on, "testit", new Object[]{" c\"d "}, new String[]{"java.lang.String"}));

    }

    public void testRequiredRolesRegExpNobody() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("testit(java.lang.String)[/ab/]", "");
        configuration.put("test*", "tester");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "testit", new Object[]{"ab"}, new String[]{"java.lang.String"}));
    }

    public void testRequiredRolesRegExp2() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("foo(java.lang.String,java.lang.String)[/a/,/b/]", "editor");
        configuration.put("foo(java.lang.String,java.lang.String)[/[bc]/ , /[^b]/]", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("editor"),
                guard.getRequiredRoles(on, "foo", new Object[]{"a", "b"}, new String[]{"java.lang.String", "java.lang.String"}));
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "foo", new Object[]{"b", "a"}, new String[]{"java.lang.String", "java.lang.String"}));
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "foo", new Object[]{"c", "c"}, new String[]{"java.lang.String", "java.lang.String"}));
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "foo", new Object[]{"b", "b"}, new String[]{"java.lang.String", "java.lang.String"}));
    }

    @SuppressWarnings("unchecked")
    public void testRequiredRolesHierarchy() throws Exception {
        Dictionary<String, Object> conf1 = new Hashtable<String, Object>();
        conf1.put("foo", "editor");
        conf1.put(Constants.SERVICE_PID, "jmx.acl.foo.bar.Test");
        Dictionary<String, Object> conf2 = new Hashtable<String, Object>();
        conf2.put("bar", "viewer");
        conf2.put("foo", "viewer");
        conf2.put(Constants.SERVICE_PID, "jmx.acl.foo.bar");
        Dictionary<String, Object> conf3 = new Hashtable<String, Object>();
        conf3.put("tar", "admin");
        conf3.put(Constants.SERVICE_PID, "jmx.acl.foo");
        Dictionary<String, Object> conf4 = new Hashtable<String, Object>();
        conf4.put("zar", "visitor");
        conf4.put(Constants.SERVICE_PID, "jmx.acl");

        ConfigurationAdmin ca = getMockConfigAdmin2(conf1, conf2, conf3, conf4);
        assertEquals("Precondition", 4, ca.listConfigurations("(service.pid=jmx.acl*)").length);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals("Should only return the most specific definition",
                Collections.singletonList("editor"),
                guard.getRequiredRoles(on, "foo", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "bar", new Object[]{"test"}, new String[]{"java.lang.String"}));
        assertEquals("The top-level is the domain, subsections of the domain should not be searched",
                Collections.emptyList(),
                guard.getRequiredRoles(on, "tar", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("visitor"),
                guard.getRequiredRoles(on, "zar", new Object[]{}, new String[]{}));
    }

    @SuppressWarnings("unchecked")
    public void testRequiredRolesHierarchyWildcard1() throws Exception {
        Dictionary<String, Object> conf1 = new Hashtable<String, Object>();
        conf1.put("foo", "viewer");
        conf1.put(Constants.SERVICE_PID, "jmx.acl._.bar.Test");
        Dictionary<String, Object> conf2 = new Hashtable<String, Object>();
        conf2.put("foo", "editor");
        conf2.put(Constants.SERVICE_PID, "jmx.acl.foo.bar.Test");

        ConfigurationAdmin ca = getMockConfigAdmin2(conf1, conf2);
        assertEquals("Precondition", 2, ca.listConfigurations("(service.pid=jmx.acl*)").length);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on1 = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals("Should only return the most specific definition",
                Collections.singletonList("editor"),
                guard.getRequiredRoles(on1, "foo", new Object[]{}, new String[]{}));
        ObjectName on2 = ObjectName.getInstance("tar.bar:type=Test");
        assertEquals("Should return definition from wildcard PID",
                Collections.singletonList("viewer"),
                guard.getRequiredRoles(on2, "foo", new Object[]{}, new String[]{}));
    }

    @SuppressWarnings("unchecked")
    public void testRequiredRolesHierarchyWildcard2() throws Exception {
        Dictionary<String, Object> conf1 = new Hashtable<String, Object>();
        conf1.put("foo", "viewer");
        conf1.put(Constants.SERVICE_PID, "jmx.acl.foo.bar.Test");
        Dictionary<String, Object> conf2 = new Hashtable<String, Object>();
        conf2.put("foo", "editor");
        conf2.put(Constants.SERVICE_PID, "jmx.acl._.bar.Test");

        ConfigurationAdmin ca = getMockConfigAdmin2(conf1, conf2);
        assertEquals("Precondition", 2, ca.listConfigurations("(service.pid=jmx.acl*)").length);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on1 = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals("Should only return the most specific definition",
                Collections.singletonList("viewer"),
                guard.getRequiredRoles(on1, "foo", new Object[]{}, new String[]{}));
        ObjectName on2 = ObjectName.getInstance("tar.bar:type=Test");
        assertEquals("Should return definition from wildcard PID",
                Collections.singletonList("editor"),
                guard.getRequiredRoles(on2, "foo", new Object[]{}, new String[]{}));
    }

    @SuppressWarnings("unchecked")
    public void testRequiredRolesHierarchyWildcard3() throws Exception {
        Dictionary<String, Object> conf1 = new Hashtable<String, Object>();
        conf1.put("foo", "viewer");
        conf1.put(Constants.SERVICE_PID, "jmx.acl._.bar.Test");
        Dictionary<String, Object> conf2 = new Hashtable<String, Object>();
        conf2.put("foo", "editor");
        conf2.put(Constants.SERVICE_PID, "jmx.acl.foo._.Test");

        ConfigurationAdmin ca = getMockConfigAdmin2(conf1, conf2);
        assertEquals("Precondition", 2, ca.listConfigurations("(service.pid=jmx.acl*)").length);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on1 = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals("Should only return the most specific definition",
                Collections.singletonList("editor"),
                guard.getRequiredRoles(on1, "foo", new Object[]{}, new String[]{}));
        ObjectName on2 = ObjectName.getInstance("foo.tar:type=Test");
        assertEquals(Collections.singletonList("editor"),
                guard.getRequiredRoles(on2, "foo", new Object[]{}, new String[]{}));
        ObjectName on3 = ObjectName.getInstance("boo.bar:type=Test");
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on3, "foo", new Object[]{}, new String[]{}));
    }

    public void testRequiredRolesMethodNameWildcard() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getFoo", "viewer");
        configuration.put("get*", " tester , editor,manager");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "getFoo", new Object[]{}, new String[]{}));
        assertEquals(Arrays.asList("tester", "editor", "manager"),
                guard.getRequiredRoles(on, "getBar", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("admin"),
                guard.getRequiredRoles(on, "test", new Object[]{new Long(17)}, new String[]{"java.lang.Long"}));
    }

    public void testRequiredRolesMethodNameWildcard2() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("ge", "janitor");
        configuration.put("get", "admin");
        configuration.put("get*", "viewer");
        configuration.put("*", "manager");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "getFoo", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("admin"),
                guard.getRequiredRoles(on, "get", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("janitor"),
                guard.getRequiredRoles(on, "ge", new Object[]{}, new String[]{}));
    }

    public void testRequiredRolesMethodNameWildcard3() throws Exception {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("get*", "viewer");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "getFoo", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("viewer"),
                guard.getRequiredRoles(on, "get", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("admin"),
                guard.getRequiredRoles(on, "ge", new Object[]{}, new String[]{}));
    }

    @SuppressWarnings("unchecked")
    public void testRequiredRolesMethodNameWildcardEmpty() throws Exception {
        Dictionary<String, Object> conf1 = new Hashtable<String, Object>();
        conf1.put("get*", " ");
        conf1.put("*", "admin");
        conf1.put(Constants.SERVICE_PID, "jmx.acl.foo.bar.Test");
        Dictionary<String, Object> conf2 = new Hashtable<String, Object>();
        conf2.put("get*", "viewer");
        conf2.put(Constants.SERVICE_PID, "jmx.acl");
        ConfigurationAdmin ca = getMockConfigAdmin2(conf1, conf2);

        KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        assertEquals(Collections.emptyList(),
                guard.getRequiredRoles(on, "getBar", new Object[]{}, new String[]{}));
        assertEquals(Collections.singletonList("admin"),
                guard.getRequiredRoles(on, "test", new Object[]{new Long(17)}, new String[]{"java.lang.Long"}));
    }

    @SuppressWarnings("unchecked")
    private ConfigurationAdmin getMockConfigAdmin(Dictionary<String, Object> configuration) throws IOException, InvalidSyntaxException {
        configuration.put(Constants.SERVICE_PID, "jmx.acl.foo.bar.Test");
        return getMockConfigAdmin2(configuration);
    }

    private ConfigurationAdmin getMockConfigAdmin2(Dictionary<String, Object>... configurations) throws IOException, InvalidSyntaxException {
        List<Configuration> allConfigs = new ArrayList<Configuration>();
        for (Dictionary<String, Object> configuration : configurations) {
            Configuration conf = EasyMock.createMock(Configuration.class);
            EasyMock.expect(conf.getPid()).andReturn((String) configuration.get(Constants.SERVICE_PID)).anyTimes();
            EasyMock.expect(conf.getProperties()).andReturn(configuration).anyTimes();
            EasyMock.replay(conf);
            allConfigs.add(conf);
        }

        ConfigurationAdmin ca = EasyMock.createMock(ConfigurationAdmin.class);
        for (Configuration c : allConfigs) {
            EasyMock.expect(ca.getConfiguration(c.getPid(), null)).andReturn(c).anyTimes();
        }
        EasyMock.expect(ca.listConfigurations(EasyMock.eq("(service.pid=jmx.acl*)"))).andReturn(
                allConfigs.toArray(new Configuration[]{})).anyTimes();
        EasyMock.expect(ca.listConfigurations(EasyMock.eq("(service.pid=jmx.acl.whitelist)"))).andReturn(
                allConfigs.toArray(new Configuration[]{})).anyTimes();
        EasyMock.replay(ca);
        return ca;
    }

    public void testCurrentUserHasRole() throws Exception {
        Subject subject = loginWithTestRoles("test");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                assertTrue(KarafMBeanServerGuard.currentUserHasRole("test"));
                assertFalse(KarafMBeanServerGuard.currentUserHasRole("toast"));
                return null;
            }
        });
    }

    public void testCurrentUserHasCustomRole() throws Exception {
        Subject subject = new Subject();
        LoginModule lm = new TestLoginModule(new TestRolePrincipal("foo"));
        lm.initialize(subject, null, null, null);
        lm.login();
        lm.commit();

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                assertTrue(KarafMBeanServerGuard.currentUserHasRole(TestRolePrincipal.class.getCanonicalName() + ":foo"));
                assertFalse(KarafMBeanServerGuard.currentUserHasRole("foo"));
                return null;
            }
        });
    }

    public void testInvoke() throws Throwable {
        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("someMethod", "editor");
        configuration.put("someOtherMethod", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("editor", "admin");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Method im = MBeanServer.class.getMethod("invoke", ObjectName.class, String.class, Object[].class, String[].class);

                    ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

                    // The following operation should not throw an exception
                    guard.invoke(null, im, new Object[]{on, "someMethod", new Object[]{"test"}, new String[]{"java.lang.String"}});

                    try {
                        guard.invoke(null, im, new Object[]{on, "someOtherMethod", new Object[]{}, new String[]{}});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }

                    try {
                        guard.invoke(null, im, new Object[]{on, "somemethingElse", new Object[]{}, new String[]{}});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }
                    return null;
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void testGetAttributeIs() throws Throwable {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Toast", "boolean", "", true, false, true);
        MBeanAttributeInfo attr2 = new MBeanAttributeInfo("TestAttr", "java.lang.String", "", true, false, false);
        MBeanAttributeInfo attr3 = new MBeanAttributeInfo("Butter", "int", "", true, true, false);

        MBeanInfo mbeanInfo = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(mbeanInfo.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr, attr2, attr3}).anyTimes();
        EasyMock.replay(mbeanInfo);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(mbeanInfo).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getToast", "admin");
        configuration.put("isToast", "editor");
        configuration.put("getTest*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("editor", "admin");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Method im = MBeanServer.class.getMethod("getAttribute", ObjectName.class, String.class);

                    // The following operations should not throw an exception
                    guard.invoke(mbs, im, new Object[]{on, "Toast"});
                    guard.invoke(mbs, im, new Object[]{on, "TestAttr"});

                    try {
                        guard.invoke(mbs, im, new Object[]{on, "Butter"});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }

                    return null;
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void testGetAttributes() throws Throwable {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Toast", "boolean", "", true, false, false);
        MBeanAttributeInfo attr2 = new MBeanAttributeInfo("TestSomething", "java.lang.String", "", true, true, false);
        MBeanAttributeInfo attr3 = new MBeanAttributeInfo("Butter", "int", "", true, true, false);

        MBeanInfo mbeanInfo = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(mbeanInfo.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr, attr2, attr3}).anyTimes();
        EasyMock.replay(mbeanInfo);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(mbeanInfo).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getToast", "editor");
        configuration.put("getTest*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("editor", "admin");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Method im = MBeanServer.class.getMethod("getAttributes", ObjectName.class, String[].class);

                    // The following operations should not throw an exception
                    guard.invoke(mbs, im, new Object[]{on, new String[]{"Toast"}});
                    guard.invoke(mbs, im, new Object[]{on, new String[]{"TestSomething", "Toast"}});

                    try {
                        guard.invoke(mbs, im, new Object[]{on, new String[]{"Butter", "Toast"}});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }

                    return null;
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void testGetAttributes2() throws Throwable {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Toast", "boolean", "", true, false, true);
        MBeanAttributeInfo attr2 = new MBeanAttributeInfo("TestSomething", "boolean", "", true, false, true);
        MBeanAttributeInfo attr3 = new MBeanAttributeInfo("Butter", "boolean", "", true, true, true);

        MBeanInfo mbeanInfo = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(mbeanInfo.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr, attr2, attr3}).anyTimes();
        EasyMock.replay(mbeanInfo);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(mbeanInfo).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("isT*", "editor");
        configuration.put("getToast", "admin");
        configuration.put("getButter", "editor");
        configuration.put("getTest*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("editor", "admin");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Method im = MBeanServer.class.getMethod("getAttributes", ObjectName.class, String[].class);

                    // The following operations should not throw an exception
                    guard.invoke(mbs, im, new Object[]{on, new String[]{"Toast"}});
                    guard.invoke(mbs, im, new Object[]{on, new String[]{"TestSomething", "Toast"}});

                    try {
                        guard.invoke(mbs, im, new Object[]{on, new String[]{"Butter", "Toast"}});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }

                    return null;
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void testSetAttribute() throws Throwable {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo a1 = new MBeanAttributeInfo("Something", "java.lang.String", "Something Attribute", true, true, false);
        MBeanAttributeInfo a2 = new MBeanAttributeInfo("Value", "long", "Value Attribute", true, true, false);
        MBeanAttributeInfo a3 = new MBeanAttributeInfo("Other", "boolean", "Other Attribute", true, true, false);
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[]{a1, a2, a3};

        MBeanInfo mbeanInfo = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(mbeanInfo.getAttributes()).andReturn(attrs).anyTimes();
        EasyMock.replay(mbeanInfo);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(mbeanInfo).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("setSomething", "editor");
        configuration.put("setValue*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("editor", "admin");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Method im = MBeanServer.class.getMethod("setAttribute", ObjectName.class, Attribute.class);

                    // The following operations should not throw an exception
                    guard.invoke(mbs, im, new Object[]{on, new Attribute("Something", "v1")});
                    guard.invoke(mbs, im, new Object[]{on, new Attribute("Value", 42L)});

                    try {
                        guard.invoke(mbs, im, new Object[]{on, new Attribute("Other", Boolean.TRUE)});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }

                    try {
                        guard.invoke(mbs, im, new Object[]{on, new Attribute("NonExistent", "v4")});
                        fail("Should not have found the MBean Declaration");
                    } catch (IllegalStateException ise) {
                        // good
                    }

                    return null;
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void testSetAttributes() throws Throwable {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo mba1 = new MBeanAttributeInfo("Something", "java.lang.String", "Something Attribute", true, true, false);
        MBeanAttributeInfo mba2 = new MBeanAttributeInfo("Value", "long", "Value Attribute", true, true, false);
        MBeanAttributeInfo mba3 = new MBeanAttributeInfo("Other", "boolean", "Other Attribute", true, true, false);
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[]{mba1, mba2, mba3};

        MBeanInfo mbeanInfo = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(mbeanInfo.getAttributes()).andReturn(attrs).anyTimes();
        EasyMock.replay(mbeanInfo);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(mbeanInfo).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("setSomething", "editor");
        configuration.put("setValue*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("editor", "admin");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    Method im = MBeanServer.class.getMethod("setAttributes", ObjectName.class, AttributeList.class);

                    // The following operations should not throw an exception
                    Attribute a1 = new Attribute("Something", "v1");
                    Attribute a2 = new Attribute("Value", 42L);
                    guard.invoke(mbs, im, new Object[]{on, new AttributeList(Arrays.asList(a1))});
                    guard.invoke(mbs, im, new Object[]{on, new AttributeList(Arrays.asList(a2, a1))});

                    Attribute a3 = new Attribute("Other", Boolean.TRUE);
                    try {
                        guard.invoke(mbs, im, new Object[]{on, new AttributeList(Arrays.asList(a1, a3))});
                        fail("Should not have allowed the invocation");
                    } catch (SecurityException se) {
                        // good
                    }

                    try {
                        Attribute a4 = new Attribute("NonExistent", "v4");
                        guard.invoke(mbs, im, new Object[]{on, new AttributeList(Arrays.asList(a4))});
                        fail("Should not have found the MBean Declaration");
                    } catch (IllegalStateException ise) {
                        // good
                    }

                    return null;
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    public void testCanInvokeMBean() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");
        final ObjectName on2 = ObjectName.getInstance("foo.bar:type=Toast");

        MBeanParameterInfo[] sig = new MBeanParameterInfo[]{new MBeanParameterInfo("arg1", "java.lang.String", "")};
        MBeanOperationInfo op = new MBeanOperationInfo("doit", "", sig, "int", MBeanOperationInfo.INFO);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{op}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{}).anyTimes();
        EasyMock.replay(info);
        MBeanInfo info2 = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info2.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info2.getAttributes()).andReturn(new MBeanAttributeInfo[]{}).anyTimes();
        EasyMock.replay(info2);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.expect(mbs.getMBeanInfo(on2)).andReturn(info2).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit(java.lang.String)[/11/]", "admin");
        configuration.put("doit(java.lang.String)", "viewer");
        configuration.put("doit(java.lang.String,java.lang.String)", "viewer");
        configuration.put("doit(int)[\"12\"]", "admin");
        configuration.put("doit", "admin");
        configuration.put("do*", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on));
                    assertFalse(guard.canInvoke(mbs, on2));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMBean2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanParameterInfo[] sig = new MBeanParameterInfo[]{new MBeanParameterInfo("arg1", "java.lang.String", "")};
        MBeanOperationInfo op = new MBeanOperationInfo("doit", "", sig, "int", MBeanOperationInfo.INFO);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{op}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit(java.lang.String)[/11/]", "admin");
        configuration.put("doit(java.lang.String)", "admin");
        configuration.put("doit(java.lang.String,java.lang.String)", "admin");
        configuration.put("doit(int)[\"12\"]", "admin");
        configuration.put("doit", "admin");
        configuration.put("do*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeAnyOverload() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanParameterInfo[] sig = new MBeanParameterInfo[]{new MBeanParameterInfo("arg1", "java.lang.String", "")};
        MBeanOperationInfo op = new MBeanOperationInfo("doit", "", sig, "int", MBeanOperationInfo.INFO);
        MBeanParameterInfo[] sig2 = new MBeanParameterInfo[]{
                new MBeanParameterInfo("arg1", "java.lang.String", ""),
                new MBeanParameterInfo("arg2", "java.lang.String", "")};
        MBeanOperationInfo op2 = new MBeanOperationInfo("doit", "", sig2, "int", MBeanOperationInfo.INFO);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{op, op2}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit(java.lang.String)", "admin");
        configuration.put("doit(java.lang.String,java.lang.String)", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on, "doit"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeAnyOverload2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanParameterInfo[] sig = new MBeanParameterInfo[]{new MBeanParameterInfo("arg1", "java.lang.String", "")};
        MBeanOperationInfo op = new MBeanOperationInfo("foit", "", sig, "int", MBeanOperationInfo.INFO);
        MBeanParameterInfo[] sig2 = new MBeanParameterInfo[]{
                new MBeanParameterInfo("arg1", "java.lang.String", ""),
                new MBeanParameterInfo("arg2", "java.lang.String", "")};
        MBeanOperationInfo op2 = new MBeanOperationInfo("doit", "", sig2, "int", MBeanOperationInfo.INFO);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{op, op2}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("foit(java.lang.String)", "viewer");
        configuration.put("doit(java.lang.String,java.lang.String)", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on, "doit"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeAnyOverload3() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit(java.lang.String)", "admin");
        configuration.put("doit(java.lang.String,java.lang.String)", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on, "doit"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanGetAttributeAnyOverload() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Foo", "int", "", true, true, false);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getFoo(java.lang.String)", "admin");
        configuration.put("getFoo()", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on, "getFoo"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanGetAttributeAnyOverload2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Foo", "int", "", true, true, false);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getFoo(java.lang.String)", "viewer");
        configuration.put("getFoo()", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on, "getFoo"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanGetAttributeAnyOverload3() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Foo", "boolean", "", true, true, true);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getFoo(java.lang.String)", "admin");
        configuration.put("getFoo()", "admin");
        configuration.put("isFoo()", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on, "isFoo"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanGetAttributeAnyOverload4() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Foo", "boolean", "", true, true, true);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getFoo(java.lang.String)", "viewer");
        configuration.put("getFoo()", "viewer");
        configuration.put("isFoo()", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on, "isFoo"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanSetAttributeAnyOverload() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Foo", "boolean", "", true, true, true);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("setFoo(java.lang.String)", "admin");
        configuration.put("setFoo(boolean)", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on, "setFoo"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanSetAttributeAnyOverload2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("Foo", "boolean", "", true, true, true);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("setFoo(java.lang.String)", "viewer");
        configuration.put("setFoo(boolean)", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");
        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on, "setFoo"));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMBeanGetter() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("a1", "boolean", "", true, false, true);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("get*", "admin");
        configuration.put("is*", "viewer");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMBeanGetter2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("a1", "boolean", "", true, false, false);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("get*", "admin");
        configuration.put("is*", "viewer");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMBeanGetter3() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("A1", "boolean", "", true, false, false);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("getA1", "viewer");
        configuration.put("is*", "admin");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMBeanSetter() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("A2", "java.lang.String", "", true, true, false);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("get*", "admin");
        configuration.put("setA2", "viewer");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(mbs, on));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMBeanSetter2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        MBeanAttributeInfo attr = new MBeanAttributeInfo("A2", "java.lang.String", "", true, true, false);

        MBeanInfo info = EasyMock.createMock(MBeanInfo.class);
        EasyMock.expect(info.getOperations()).andReturn(new MBeanOperationInfo[]{}).anyTimes();
        EasyMock.expect(info.getAttributes()).andReturn(new MBeanAttributeInfo[]{attr}).anyTimes();
        EasyMock.replay(info);

        final MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.expect(mbs.getMBeanInfo(on)).andReturn(info).anyTimes();
        EasyMock.replay(mbs);

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("get*", "admin");
        configuration.put("setA2", "admin");
        configuration.put("*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertFalse(guard.canInvoke(mbs, on));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMethod() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit(java.lang.String)[/11/]", "admin");
        configuration.put("doit(java.lang.String)", "viewer");
        configuration.put("doit(java.lang.String,java.lang.String)", "viewer");
        configuration.put("doit(int)[\"12\"]", "admin");
        configuration.put("doit", "admin");
        configuration.put("do*", "viewer");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(null, on, "dodo", new String[]{"java.lang.String"}));
                    assertTrue(guard.canInvoke(null, on, "doit", new String[]{"java.lang.String", "java.lang.String"}));
                    assertTrue(guard.canInvoke(null, on, "doit", new String[]{"java.lang.String"}));
                    assertFalse(guard.canInvoke(null, on, "doit", new String[]{"int"}));
                    assertFalse(guard.canInvoke(null, on, "doit", new String[]{}));
                    assertFalse(guard.canInvoke(null, on, "uuuh", new String[]{"java.lang.String"}));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    public void testCanInvokeMethod2() throws Exception {
        final ObjectName on = ObjectName.getInstance("foo.bar:type=Test");

        Dictionary<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put("doit(java.lang.String)[/11/]", "viewer");
        configuration.put("doit(java.lang.String)", "admin");
        configuration.put("doit(java.lang.String,java.lang.String)", "admin");
        configuration.put("doit(int)[\"12\"]", "viewer");
        configuration.put("doit", "viewer");
        configuration.put("do*", "admin");
        ConfigurationAdmin ca = getMockConfigAdmin(configuration);

        final KarafMBeanServerGuard guard = new KarafMBeanServerGuard();
        guard.setConfigAdmin(ca);

        Subject subject = loginWithTestRoles("viewer");

        Subject.doAs(subject, new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    assertTrue(guard.canInvoke(null, on, "doit", new String[]{"java.lang.String"}));
                    assertTrue(guard.canInvoke(null, on, "doit", new String[]{}));
                    assertTrue(guard.canInvoke(null, on, "doit", new String[]{"int"}));
                    assertFalse(guard.canInvoke(null, on, "doit", new String[]{"java.lang.String", "java.lang.String"}));
                    assertFalse(guard.canInvoke(null, on, "dodo", new String[]{"java.lang.String"}));
                    assertFalse(guard.canInvoke(null, on, "uuuh", new String[]{"java.lang.String"}));

                    return null;
                } catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }
        });
    }

    private Subject loginWithTestRoles(String... roles) throws LoginException {
        Subject subject = new Subject();
        LoginModule lm = new TestLoginModule(roles);
        lm.initialize(subject, null, null, null);
        lm.login();
        lm.commit();
        return subject;
    }

    private static class TestLoginModule implements LoginModule {
        private final Principal[] principals;
        private Subject subject;

        private static Principal[] getPrincipals(String... roles) {
            List<Principal> principals = new ArrayList<Principal>();
            for (String role : roles) {
                principals.add(new RolePrincipal(role));
            }
            return principals.toArray(new Principal[]{});
        }


        public TestLoginModule(String... roles) {
            this(getPrincipals(roles));
        }

        public TestLoginModule(Principal... principals) {
            this.principals = principals;
        }

        public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
            this.subject = subject;
        }

        public boolean login() throws LoginException {
            return true;
        }

        public boolean commit() throws LoginException {
            Set<Principal> sp = subject.getPrincipals();
            sp.addAll(Arrays.asList(principals));
            return true;
        }

        public boolean abort() throws LoginException {
            return true;
        }

        public boolean logout() throws LoginException {
            Set<Principal> sp = subject.getPrincipals();
            sp.removeAll(Arrays.asList(principals));
            return true;
        }
    }

}
