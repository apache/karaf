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
package org.apache.karaf.service.guard.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.aries.proxy.ProxyManager;
import org.apache.aries.proxy.impl.AsmProxyManager;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.service.guard.impl.GuardProxyCatalog.CreateProxyRunnable;
import org.apache.karaf.service.guard.impl.GuardProxyCatalog.ServiceRegistrationHolder;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class GuardProxyCatalogTest {
    // Some assertions fail when run under a code coverage tool, they are skipped when this is set to true
    private static final boolean runningUnderCoverage = false; // set to false before committing any changes

    @Test
    public void testGuardProxyCatalog() throws Exception {
        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getBundleId()).andReturn(9823L).anyTimes();
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        bc.addServiceListener(EasyMock.isA(ServiceListener.class));
        EasyMock.expectLastCall().once();
        String caFilter = "(&(objectClass=org.osgi.service.cm.ConfigurationAdmin)"
                + "(!(" + GuardProxyCatalog.PROXY_SERVICE_KEY + "=*)))";
        EasyMock.expect(bc.createFilter(caFilter)).andReturn(FrameworkUtil.createFilter(caFilter)).anyTimes();
        String pmFilter = "(&(objectClass=org.apache.aries.proxy.ProxyManager)"
                + "(!(" + GuardProxyCatalog.PROXY_SERVICE_KEY + "=*)))";
        EasyMock.expect(bc.createFilter(pmFilter)).andReturn(FrameworkUtil.createFilter(pmFilter)).anyTimes();
        EasyMock.replay(bc);

        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);
        assertTrue("Service Tracker for ConfigAdmin should be opened", gpc.configAdminTracker.getTrackingCount() != -1);
        assertTrue("Service Tracker for ProxyManager should be opened", gpc.proxyManagerTracker.getTrackingCount() != -1);

        EasyMock.verify(bc);

        // Add some more behaviour checks to the bundle context
        EasyMock.reset(bc);
        bc.removeServiceListener(EasyMock.isA(ServiceListener.class));
        EasyMock.expectLastCall().once();
        EasyMock.replay(bc);

        gpc.close();
        assertEquals("Service Tracker for ConfigAdmin should be closed", -1, gpc.configAdminTracker.getTrackingCount());
        assertEquals("Service Tracker for ProxyManager should be closed", -1, gpc.proxyManagerTracker.getTrackingCount());

        EasyMock.verify(bc);
    }

    @Test
    public void testIsProxy() throws Exception {
        BundleContext bc = mockBundleContext();

        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        assertTrue(gpc.isProxy(mockServiceReference(props)));
        assertFalse(gpc.isProxy(mockServiceReference(new Hashtable<>())));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHandleProxificationForHook() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, GuardProxyCatalog.SERVICE_ACL_PREFIX + "foo");
        config.put(GuardProxyCatalog.SERVICE_GUARD_KEY, "(a>=5)");
        BundleContext bc = mockConfigAdminBundleContext(config);
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_ID, 13L);
        props.put("a", "6");
        props.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        ServiceReference<?> sref2 = mockServiceReference(props);
        assertFalse("Should not hide an existing proxy for this client",
                gpc.handleProxificationForHook(sref2));
        assertEquals("No proxy should have been created", 0, gpc.proxyMap.size());

        Dictionary<String, Object> props4 = new Hashtable<>();
        props4.put(Constants.SERVICE_ID, 15L);
        props4.put("a", "7");
        ServiceReference<?> sref4 = mockServiceReference(props4);
        assertTrue("Should hide a service that needs to be proxied",
                gpc.handleProxificationForHook(sref4));
        assertEquals("Should trigger proxy creation", 1, gpc.proxyMap.size());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testHandleServiceUnregistering() throws Exception {
        BundleContext clientBC = openStrictMockBundleContext(mockBundle(12345L));
        BundleContext client2BC = openStrictMockBundleContext(mockBundle(6L));
        EasyMock.replay(clientBC);
        EasyMock.replay(client2BC);

        Hashtable<String, Object> props = new Hashtable<>();
        long originalServiceID = 12345678901234L;
        props.put(Constants.SERVICE_ID, Long.valueOf(originalServiceID));
        props.put("foo", "bar");
        ServiceReference<?> originalRef = mockServiceReference(props);

        Hashtable<String, Object> props2 = new Hashtable<>();
        long anotherServiceID = 5123456789012345L;
        props2.put(Constants.SERVICE_ID, anotherServiceID);
        ServiceReference<?> anotherRef = mockServiceReference(props2);

        GuardProxyCatalog gpc = new GuardProxyCatalog(mockBundleContext());

        ServiceRegistration<?> proxyReg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.expect(proxyReg.getReference()).andReturn((ServiceReference)
                mockServiceReference(props)).anyTimes();
        proxyReg.unregister();
        EasyMock.expectLastCall().once();
        EasyMock.replay(proxyReg);
        ServiceRegistrationHolder srh = new GuardProxyCatalog.ServiceRegistrationHolder();
        srh.registration = proxyReg;

        ServiceRegistration<?> proxy2Reg = EasyMock.createMock(ServiceRegistration.class);
        EasyMock.replay(proxy2Reg);
        ServiceRegistrationHolder srh2 = new GuardProxyCatalog.ServiceRegistrationHolder();
        srh2.registration = proxy2Reg;

        gpc.proxyMap.put(originalServiceID, srh);
        gpc.proxyMap.put(anotherServiceID, srh2);
        assertEquals("Precondition", 2, gpc.proxyMap.size());

        gpc.createProxyQueue.put(new MockCreateProxyRunnable(originalServiceID));
        gpc.createProxyQueue.put(new MockCreateProxyRunnable(777));
        assertEquals("Precondition", 2, gpc.createProxyQueue.size());

        gpc.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, originalRef));
        assertEquals("Registered events should be ignored", 2, gpc.proxyMap.size());
        assertEquals("Registered events should be ignored", 2, gpc.createProxyQueue.size());

        Hashtable<String, Object> proxyProps = new Hashtable<>(props);
        proxyProps.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        ServiceReference<?> proxyRef = mockServiceReference(proxyProps);
        gpc.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, proxyRef));
        assertEquals("Unregistering the proxy should be ignored by the listener", 2, gpc.proxyMap.size());
        assertEquals("Unregistering the proxy should be ignored by the listener", 2, gpc.createProxyQueue.size());

        gpc.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, originalRef));
        assertEquals("The proxy for this service should have been removed", 1, gpc.proxyMap.size());
        assertEquals(anotherRef.getProperty(Constants.SERVICE_ID), gpc.proxyMap.keySet().iterator().next());
        assertEquals("The create proxy job for this service should have been removed", 1, gpc.createProxyQueue.size());
        assertEquals(777, gpc.createProxyQueue.iterator().next().getOriginalServiceID());

        EasyMock.verify(proxyReg);
        EasyMock.verify(proxy2Reg);
    }

    @Test
    public void testCreateProxy() throws Exception {
        // This method tests proxy creation for various service implementation types.

        testCreateProxy(TestServiceAPI.class, new TestService());
        testCreateProxy(TestServiceAPI.class, new DescendantTestService());
        testCreateProxy(TestServiceAPI.class, new PrivateTestService());
        testCreateProxy(TestServiceAPI.class, new PrivateTestServiceNoDirectInterfaces());
        testCreateProxy(TestServiceAPI.class, new FinalTestService());
        testCreateProxy(TestObjectWithoutInterface.class, new TestObjectWithoutInterface());
        testCreateProxy(TestServiceAPI.class, new CombinedTestService());
        testCreateProxy(PrivateTestService.class, new PrivateTestService());
        testCreateProxy(PrivateTestServiceNoDirectInterfaces.class, new PrivateTestServiceNoDirectInterfaces());
        testCreateProxy(Object.class, new TestService());
        testCreateProxy(Object.class, new DescendantTestService());
        testCreateProxy(Object.class, new PrivateTestService());
        testCreateProxy(Object.class, new TestObjectWithoutInterface());
        testCreateProxy(Object.class, new CombinedTestService());
        testCreateProxy(Object.class, new FinalTestService());
        testCreateProxy(TestServiceAPI.class, (TestServiceAPI) () -> "Doing it");
        testCreateProxy(Object.class, new ClassWithFinalMethod());
        testCreateProxy(Object.class, new ClassWithPrivateMethod());
    }

    @Test
    public void testCreateProxyMultipleObjectClasses() throws Exception {
        testCreateProxy(new Class [] {TestServiceAPI.class, TestService.class}, new TestService());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAssignRoles() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, "foobar");
        config.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        config.put("somemethod", "a,b");
        config.put("someOtherMethod(int)", "c");
        config.put("someOtherMethod(int)[/12/]", "d");
        config.put("someOtherMethod(int)[\"42\"]", "e");
        config.put("someOtherMethod[/.*[x][y][z].*/]", "f");
        config.put("someFoo*", "g");

        BundleContext bc = mockConfigAdminBundleContext(config);

        Dictionary<String, Object> proxyProps = testCreateProxy(bc, TestServiceAPI.class, new TestService());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g")),
                new HashSet<>((Collection<String>) proxyProps.get(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAssignRoles2() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, "foobar");
        config.put("service.guard", "(objectClass=" + TestServiceAPI2.class.getName() + ")");
        config.put("doit", "X");

        BundleContext bc = mockConfigAdminBundleContext(config);

        Dictionary<String, Object> proxyProps = testCreateProxy(bc, TestServiceAPI.class, new TestService());
        assertNull("No security defined for this API, so no roles should be specified at all",
                proxyProps.get(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testAssignRoles3() throws Exception {
        abstract class MyAbstractClass implements TestServiceAPI, TestServiceAPI2 {}

        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, "foobar");
        config.put("service.guard", "(objectClass=" + TestServiceAPI2.class.getName() + ")");
        config.put("doit", "X");

        BundleContext bc = mockConfigAdminBundleContext(config);

        Map<ServiceReference, Object> serviceMap = new HashMap<>();
        testCreateProxy(bc, new Class [] {TestServiceAPI.class, TestServiceAPI2.class}, new MyAbstractClass() {
            @Override
            public String doit() {
                return null;
            }

            @Override
            public String doit(String s) {
                return null;
            }
        }, serviceMap);
        assertEquals(1, serviceMap.size());
        assertEquals(Collections.singleton("X"), serviceMap.keySet().iterator().next().
                getProperty(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAssignRoles4() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, "foobar");
        config.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        config.put("somemethod", "b");
        config.put("someOtherMethod", "b");
        config.put("somethingelse", "*");

        BundleContext bc = mockConfigAdminBundleContext(config);

        Dictionary<String, Object> proxyProps = testCreateProxy(bc, TestServiceAPI.class, new TestService());
        Collection<String> result = (Collection<String>) proxyProps.get(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY);
        assertEquals(1, result.size());
        assertEquals("b", result.iterator().next());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking1() throws Exception {
        Dictionary<String, Object> c1 = new Hashtable<>();
        c1.put(Constants.SERVICE_PID, "foobar");
        c1.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        c1.put("doit", "a,b");
        Dictionary<String, Object> c2 = new Hashtable<>();
        c2.put(Constants.SERVICE_PID, "barfoobar");
        c2.put("service.guard", "(objectClass=" + TestObjectWithoutInterface.class.getName() + ")");
        c2.put("compute", "c");

        BundleContext bc = mockConfigAdminBundleContext(c1, c2);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI.class, TestObjectWithoutInterface.class}, new CombinedTestService());

        // Run with the right credentials so we can test the expected roles
        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("b"));
        Subject.doAs(subject, (PrivilegedAction<Object>) () -> {
            assertEquals("Doing it", ((TestServiceAPI) proxy).doit());
            if (!runningUnderCoverage) {
                try {
                    ((TestObjectWithoutInterface) proxy).compute(44L);
                    fail("Should have been blocked");
                } catch (SecurityException se) {
                    // good
                }
            }

            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking2() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, "barfoobar");
        config.put("service.guard", "(objectClass=" + TestObjectWithoutInterface.class.getName() + ")");
        config.put("compute(long)[\"42\"]", "b");
        config.put("compute(long)", "c");

        BundleContext bc = mockConfigAdminBundleContext(config);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI.class, TestObjectWithoutInterface.class}, new CombinedTestService());

        // Run with the right credentials so we can test the expected roles
        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("b"));
        Subject.doAs(subject, (PrivilegedAction<Object>) () -> {
            if (!runningUnderCoverage) {
                assertEquals(-42L, ((TestObjectWithoutInterface) proxy).compute(42L));
                try {
                    ((TestObjectWithoutInterface) proxy).compute(44L);
                    fail("Should have been blocked");
                } catch (SecurityException se) {
                    // good
                }
            }

            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking3() throws Exception {
        class MyService implements TestServiceAPI, TestServiceAPI2 {
            public String doit(String s) {
                return new StringBuilder(s).reverse().toString();
            }

            public String doit() {
                return "Doing it";
            }
        }

        Dictionary<String, Object> c1 = new Hashtable<>();
        c1.put(Constants.SERVICE_PID, "foobar");
        c1.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        c1.put("do*", "c");
        Dictionary<String, Object> c2 = new Hashtable<>();
        c2.put(Constants.SERVICE_PID, "foobar2");
        c2.put("service.guard", "(objectClass=" + TestServiceAPI2.class.getName() + ")");
        c2.put("doit(java.lang.String)[/[tT][a]+/]", "b,d # a regex rule");
        c2.put("doit(java.lang.String)", "a");

        BundleContext bc = mockConfigAdminBundleContext(c1, c2);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI.class, TestServiceAPI2.class}, new MyService());

        // Run with the right credentials so we can test the expected roles
        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("c"));
        Subject.doAs(subject, (PrivilegedAction<Object>) () -> {
            assertEquals("Doing it", ((TestServiceAPI) proxy).doit());
            return null;
        });

        Subject subject2 = new Subject();
        subject2.getPrincipals().add(new RolePrincipal("b"));
        subject2.getPrincipals().add(new RolePrincipal("f"));
        Subject.doAs(subject2, (PrivilegedAction<Object>) () -> {
            try {
                assertEquals("Doing it", ((TestServiceAPI) proxy).doit());
                fail("Should have been blocked");
            } catch (SecurityException se) {
                // good
            }
            assertEquals("aaT", ((TestServiceAPI2) proxy).doit("Taa"));
            try {
                ((TestServiceAPI2) proxy).doit("t");
                fail("Should have been blocked");
            } catch (SecurityException se) {
                // good
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking4() throws Exception {
        BundleContext bc = mockConfigAdminBundleContext();

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI.class, TestObjectWithoutInterface.class}, new CombinedTestService());

        // Run with the right credentials so we can test the expected roles
        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("b"));
        Subject.doAs(subject, (PrivilegedAction<Object>) () -> {
            assertEquals("Doing it", ((TestServiceAPI) proxy).doit());
            if (!runningUnderCoverage) {
                assertEquals(42L, ((TestObjectWithoutInterface) proxy).compute(-42L));
                assertEquals(-44L, ((TestObjectWithoutInterface) proxy).compute(44L));
            }

            return null;
        });
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking5() throws Exception {
        Dictionary<String, Object> c1 = new Hashtable<>();
        c1.put(Constants.SERVICE_PID, "foobar");
        c1.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        c1.put("doit", "a,b");

        BundleContext bc = mockConfigAdminBundleContext(c1);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI2.class},
                (TestServiceAPI2) String::toUpperCase);

        // Invoke the service with role 'c'.
        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("c"));
        Subject.doAs(subject, (PrivilegedAction<Object>) () -> {
            assertEquals("The invocation under role 'c' should be ok, as there are no rules specified "
                    + "for this service at all.", "HELLO", ((TestServiceAPI2) proxy).doit("hello"));
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking6() throws Exception {
        Dictionary<String, Object> c1 = new Hashtable<>();
        c1.put(Constants.SERVICE_PID, "foobar");
        c1.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        c1.put("doit", "a,b");
        Dictionary<String, Object> c2 = new Hashtable<>();
        c2.put(Constants.SERVICE_PID, "foobar2");
        c2.put("service.guard", "(objectClass=" + TestServiceAPI2.class.getName() + ")");
        c2.put("bar", "c");

        BundleContext bc = mockConfigAdminBundleContext(c1, c2);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI2.class},
                (TestServiceAPI2) String::toUpperCase);

        // Invoke the service with role 'c'.
        Subject subject = new Subject();
        subject.getPrincipals().add(new RolePrincipal("a"));
        subject.getPrincipals().add(new RolePrincipal("b"));
        subject.getPrincipals().add(new RolePrincipal("c"));
        Subject.doAs(subject, (PrivilegedAction<Object>) () -> {
            try {
                ((TestServiceAPI2) proxy).doit("hello");
                fail("The invocation should not process as the 'doit' operation has no roles associated with it");
            } catch (SecurityException se) {
                // good
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInvocationBlocking7() throws Exception {
        Dictionary<String, Object> c1 = new Hashtable<>();
        c1.put(Constants.SERVICE_PID, "foobar");
        c1.put("service.guard", "(objectClass=" + TestServiceAPI3.class.getName() + ")");
        c1.put("foo()", "a");
        c1.put("bar", "b");
        c1.put("*", "*");

        BundleContext bc = mockConfigAdminBundleContext(c1);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI3.class}, new TestService3());

        Subject s1 = new Subject();
        Subject.doAs(s1, (PrivilegedAction<Object>) () -> {
            TestServiceAPI3 obj = (TestServiceAPI3) proxy;
            assertEquals("Should have allowed this invocation for any (or no) role", -7, obj.foo(7));
            try {
                obj.foo();
                fail("Should have been blocked");
            } catch (SecurityException se) {
                // good
            }
            try {
                obj.bar();
                fail("Should have been blocked");
            } catch (SecurityException se) {
                // good
            }

            return null;
        });

        Subject s2 = new Subject();
        s2.getPrincipals().add(new RolePrincipal("a"));
        s2.getPrincipals().add(new RolePrincipal("b"));
        s2.getPrincipals().add(new RolePrincipal("d"));
        Subject.doAs(s2, (PrivilegedAction<Object>) () -> {
            TestServiceAPI3 obj = (TestServiceAPI3) proxy;
            assertEquals(42, obj.foo());
            assertEquals(99, obj.bar());
            assertEquals(-32767, obj.foo(32767));
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCustomRole() throws Exception {
        class MyRolePrincipal implements Principal {
            @Override
            public String getName() {
                return "role1";
            }
        }

        Dictionary<String, Object> c1 = new Hashtable<>();
        c1.put(Constants.SERVICE_PID, "foobar");
        c1.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        c1.put("doit", MyRolePrincipal.class.getName() + ":role1");
        BundleContext bc = mockConfigAdminBundleContext(c1);

        final Object proxy = testCreateProxy(bc, new Class [] {TestServiceAPI.class}, new TestService());

        Subject s1 = new Subject();
        s1.getPrincipals().add(new RolePrincipal("role1"));
        Subject.doAs(s1, (PrivilegedAction<Object>) () -> {
            try {
                ((TestServiceAPI) proxy).doit();
                fail("Should have prevented this invocation as the custom role is required");
            } catch (SecurityException se) {
                // good
            }
            return null;
        });


        Subject s2 = new Subject();
        s2.getPrincipals().add(new MyRolePrincipal());
        Subject.doAs(s2, (PrivilegedAction<Object>) () -> {
            ((TestServiceAPI) proxy).doit(); // Should work, the custom role is there
            return null;
        });

        Subject s3 = new Subject();
        s3.getPrincipals().add(new MyRolePrincipal());
        s3.getPrincipals().add(new RolePrincipal("role1"));
        Subject.doAs(s3, (PrivilegedAction<Object>) () -> {
            ((TestServiceAPI) proxy).doit(); // Should work, the custom role is there
            return null;
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testProxyCreationThread() throws Exception {
        ProxyManager proxyManager = getProxyManager();

        ConfigurationAdmin ca = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(ca.listConfigurations(EasyMock.anyObject(String.class))).andReturn(null).anyTimes();
        EasyMock.replay(ca);

        ServiceReference pmSref = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(pmSref);
        ServiceReference pmSref2 = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(pmSref2);
        ServiceReference cmSref = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(cmSref);

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getBundleId()).andReturn(23992734L).anyTimes();
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(
                () -> FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0])).anyTimes();
        final ServiceListener [] pmListenerHolder = new ServiceListener [1];
        String pmFilter = "(&(objectClass=" + ProxyManager.class.getName() + ")" +
                "(!(" + GuardProxyCatalog.PROXY_SERVICE_KEY + "=*)))";
        bc.addServiceListener(EasyMock.isA(ServiceListener.class), EasyMock.eq(pmFilter));
        EasyMock.expectLastCall().andAnswer(() -> {
            pmListenerHolder[0] = (ServiceListener) EasyMock.getCurrentArguments()[0];
            return null;
        }).anyTimes();
        EasyMock.expect(bc.getServiceReferences(EasyMock.anyObject(String.class),
                EasyMock.contains(ConfigurationAdmin.class.getName()))).andReturn(new ServiceReference[] {cmSref}).anyTimes();
        EasyMock.expect(bc.getService(pmSref)).andReturn(proxyManager).anyTimes();
        EasyMock.expect(bc.getService(pmSref2)).andReturn(proxyManager).anyTimes();
        EasyMock.expect(bc.getService(cmSref)).andReturn(ca).anyTimes();
        EasyMock.replay(bc);

        // This should put a ServiceListener in the pmListenerHolder, the ServiceTracker does that
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        // The service being proxied has these properties
        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {TestServiceAPI.class.getName()});
        serviceProps.put(Constants.SERVICE_ID, 162L);

        final Map<ServiceReference<?>, Object> serviceMap = new HashMap<>();

        // The mock bundle context for the bundle providing the service is set up here
        BundleContext providerBC = EasyMock.createMock(BundleContext.class);
        // These are the expected service properties of the proxy registration. Note the proxy marker...
        final Hashtable<String, Object> expectedProxyProps = new Hashtable<>(serviceProps);
        expectedProxyProps.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        EasyMock.expect(providerBC.registerService(
                EasyMock.isA(String[].class),
                EasyMock.anyObject(),
                EasyMock.isA(Dictionary.class))).andAnswer((IAnswer) () -> {
                    Dictionary<String,Object> props = (Dictionary<String, Object>) EasyMock.getCurrentArguments()[2];
                    ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);
                    ServiceReference sr = mockServiceReference(props);
                    EasyMock.expect(reg.getReference()).andReturn(sr).anyTimes();
                    reg.unregister();
                    EasyMock.expectLastCall().once();
                    EasyMock.replay(reg);

                    serviceMap.put(sr, EasyMock.getCurrentArguments()[1]);

                    return reg;
                }).once();
        EasyMock.expect(providerBC.getService(EasyMock.isA(ServiceReference.class))).andAnswer(
                () -> serviceMap.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(providerBC);

        // In some cases the proxy-creating code is looking for a classloader (e.g. when run through
        // a coverage tool such as EclEmma). This will satisfy that.
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
        EasyMock.replay(bw);

        // The mock bundle that provides the original service (and also the proxy is registered with this)
        Bundle providerBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(providerBC).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        EasyMock.replay(providerBundle);

        ServiceReference sr = mockServiceReference(providerBundle, serviceProps);

        assertEquals("Precondition", 0, gpc.proxyMap.size());
        assertEquals("Precondition", 0, gpc.createProxyQueue.size());
        // Create the proxy for the service
        gpc.proxyIfNotAlreadyProxied(sr);
        assertEquals(1, gpc.proxyMap.size());
        assertEquals(1, gpc.createProxyQueue.size());

        // The actual proxy creation is done asynchronously.
        GuardProxyCatalog.ServiceRegistrationHolder holder = gpc.proxyMap.get(162L);
        assertNull("The registration shouldn't have happened yet", holder.registration);
        assertEquals(1, gpc.createProxyQueue.size());

        Thread[] tarray = new Thread[Thread.activeCount()];
        Thread.enumerate(tarray);
        for (Thread t : tarray) {
            if (t != null) {
                assertTrue(!GuardProxyCatalog.PROXY_CREATOR_THREAD_NAME.equals(t.getName()));
            }
        }

        // make the proxy manager appear
        pmListenerHolder[0].serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, pmSref));
        Thread.sleep(400); // give the system some time to send the events...

        Thread ourThread = null;
        Thread[] tarray2 = new Thread[Thread.activeCount()];
        Thread.enumerate(tarray2);
        for (Thread t : tarray2) {
            if (t != null) {
                if (t.getName().equals(GuardProxyCatalog.PROXY_CREATOR_THREAD_NAME)) {
                    ourThread = t;
                }
            }
        }
        assertNotNull(ourThread);
        assertTrue(ourThread.isDaemon());
        assertTrue(ourThread.isAlive());
        assertNotNull(holder.registration);

        assertEquals(0, gpc.createProxyQueue.size());

        int numProxyThreads = 0;
        pmListenerHolder[0].serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, pmSref2));
        Thread.sleep(300); // give the system some time to send the events...

        Thread[] tarray3 = new Thread[Thread.activeCount()];
        Thread.enumerate(tarray3);
        for (Thread t : tarray3) {
            if (t != null) {
                if (t.getName().equals(GuardProxyCatalog.PROXY_CREATOR_THREAD_NAME)) {
                    numProxyThreads++;
                }
            }
        }
        assertEquals("Maximum 1 proxy thread, even if there is more than 1 proxy service", 1, numProxyThreads);

        // Clean up thread
        pmListenerHolder[0].serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, pmSref));
        Thread.sleep(300); // Give the system some time to stop the threads...
        Thread[] tarray4 = new Thread[Thread.activeCount()];
        Thread.enumerate(tarray4);
        for (Thread t : tarray4) {
            if (t != null) {
                assertTrue(!GuardProxyCatalog.PROXY_CREATOR_THREAD_NAME.equals(t.getName()));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testHandleServiceModified() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put(Constants.SERVICE_PID, "test.1.2.3");
        config.put("service.guard", "(objectClass=" + TestServiceAPI.class.getName() + ")");
        config.put("doit", "role.1");
        Dictionary<String, Object> config2 = new Hashtable<>();
        config2.put(Constants.SERVICE_PID, "test.1.2.4");
        config2.put("service.guard", "(objectClass=" + TestServiceAPI2.class.getName() + ")");
        config2.put("doit", "role.2");

        BundleContext bc = mockConfigAdminBundleContext(config, config2);
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);
        // The service being proxied has these properties
        long serviceID = 1L;
        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {TestServiceAPI.class.getName(), TestServiceAPI2.class.getName()});
        serviceProps.put(Constants.SERVICE_ID, serviceID);
        serviceProps.put(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY, Collections.singletonList("someone")); // will be overwritten
        Object myObject = new Object();
        serviceProps.put("foo.bar", myObject);

        BundleContext providerBC = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(providerBC.registerService(
                EasyMock.aryEq(new String [] {TestServiceAPI.class.getName(), TestServiceAPI2.class.getName()}),
                EasyMock.anyObject(),
                EasyMock.anyObject(Dictionary.class))).andAnswer((IAnswer) () -> {
                    final Dictionary props = (Dictionary) EasyMock.getCurrentArguments()[2];
                    assertEquals(Boolean.TRUE, props.get(GuardProxyCatalog.PROXY_SERVICE_KEY));

                    ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);
                    ServiceReference sr = mockServiceReference(props);
                    EasyMock.expect(reg.getReference()).andReturn(sr).anyTimes();
                    reg.setProperties(EasyMock.isA(Dictionary.class));
                    EasyMock.expectLastCall().andAnswer(() -> {
                        // Push the update into the service reference
                        ArrayList<String> oldKeys = Collections.list(props.keys());
                        for (String key : oldKeys) {
                            props.remove(key);
                        }
                        Dictionary<String, Object> newProps = (Dictionary<String, Object>) EasyMock.getCurrentArguments()[0];
                        for (String key : Collections.list(newProps.keys())) {
                            props.put(key, newProps.get(key));
                        }
                        return null;
                    }).once();
                    EasyMock.replay(reg);

                    return reg;
                }).anyTimes();

        EasyMock.replay(providerBC);

        // In some cases the proxy-creating code is looking for a classloader (e.g. when run through
        // a coverage tool such as EclEmma). This will satisfy that.
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
        EasyMock.replay(bw);

        // The mock bundle that provides the original service (and also the proxy is registered with this)
        Bundle providerBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(providerBC).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        EasyMock.replay(providerBundle);

        ServiceReference sr = mockServiceReference(providerBundle, serviceProps);

        gpc.proxyIfNotAlreadyProxied(sr);
        GuardProxyCatalog.CreateProxyRunnable runnable = gpc.createProxyQueue.take();
        runnable.run(getProxyManager());

        ServiceRegistrationHolder holder = gpc.proxyMap.get(serviceID);
        ServiceRegistration<?> reg = holder.registration;

        for (String key : serviceProps.keySet()) {
            if (GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY.equals(key)) {
                assertEquals(new HashSet(Arrays.asList("role.1", "role.2")), reg.getReference().getProperty(key));
            } else {
                assertEquals(serviceProps.get(key), reg.getReference().getProperty(key));
            }
        }
        assertEquals(Boolean.TRUE, reg.getReference().getProperty(GuardProxyCatalog.PROXY_SERVICE_KEY));

        // now change the original service and let the proxy react
        serviceProps.put("test", "property");
        assertEquals("Precondition, the mocked reference should have picked up this change",
                "property", sr.getProperty("test"));

        gpc.serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED, sr));
        assertEquals("Changing the service should not change the number of proxies", 1, gpc.proxyMap.size());

        for (String key : serviceProps.keySet()) {
            if (GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY.equals(key)) {
                assertEquals(new HashSet(Arrays.asList("role.1", "role.2")), reg.getReference().getProperty(key));
            } else {
                assertEquals(serviceProps.get(key), reg.getReference().getProperty(key));
            }
        }
        assertEquals("property", reg.getReference().getProperty("test"));
        assertEquals(Boolean.TRUE, reg.getReference().getProperty(GuardProxyCatalog.PROXY_SERVICE_KEY));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testHandleServiceModified2() throws Exception {
        BundleContext bc = mockConfigAdminBundleContext(); // no configuration used in this test...
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        // The service being proxied has these properties
        long serviceID = 1L;
        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {TestServiceAPI.class.getName()});
        serviceProps.put(Constants.SERVICE_ID, serviceID);

        BundleContext providerBC = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(providerBC.registerService(
                EasyMock.aryEq(new String [] {TestServiceAPI.class.getName()}),
                EasyMock.anyObject(),
                EasyMock.anyObject(Dictionary.class))).andAnswer((IAnswer) () -> {
                    final Dictionary props = (Dictionary) EasyMock.getCurrentArguments()[2];
                    assertEquals(Boolean.TRUE, props.get(GuardProxyCatalog.PROXY_SERVICE_KEY));

                    ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);
                    ServiceReference sr = mockServiceReference(props);
                    EasyMock.expect(reg.getReference()).andReturn(sr).anyTimes();
                    reg.setProperties(EasyMock.isA(Dictionary.class));
                    EasyMock.expectLastCall().andAnswer(() -> {
                        // Push the update into the service reference
                        ArrayList<String> oldKeys = Collections.list(props.keys());
                        for (String key : oldKeys) {
                            props.remove(key);
                        }
                        Dictionary<String, Object> newProps = (Dictionary<String, Object>) EasyMock.getCurrentArguments()[0];
                        for (String key : Collections.list(newProps.keys())) {
                            props.put(key, newProps.get(key));
                        }
                        return null;
                    }).once();
                    EasyMock.replay(reg);

                    return reg;
                }).anyTimes();

        EasyMock.replay(providerBC);

        // In some cases the proxy-creating code is looking for a classloader (e.g. when run through
        // a coverage tool such as EclEmma). This will satisfy that.
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
        EasyMock.replay(bw);

        // The mock bundle that provides the original service (and also the proxy is registered with this)
        Bundle providerBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(providerBC).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        EasyMock.replay(providerBundle);

        ServiceReference sr = mockServiceReference(providerBundle, serviceProps);

        gpc.proxyIfNotAlreadyProxied(sr);
        GuardProxyCatalog.CreateProxyRunnable runnable = gpc.createProxyQueue.take();
        runnable.run(getProxyManager());

        ServiceRegistrationHolder holder = gpc.proxyMap.get(serviceID);
        ServiceRegistration<?> reg = holder.registration;

        assertFalse("No roles defined for this service using configuration, so roles property should not be set",
                Arrays.asList(reg.getReference().getPropertyKeys()).contains(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY));
        for (String key : serviceProps.keySet()) {
            assertEquals(serviceProps.get(key), reg.getReference().getProperty(key));
        }
        assertEquals(Boolean.TRUE, reg.getReference().getProperty(GuardProxyCatalog.PROXY_SERVICE_KEY));

        // now change the original service and let the proxy react
        serviceProps.put(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY, "foobar");
        assertEquals("Precondition, the mocked reference should have picked up this change",
                "foobar", sr.getProperty(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY));

        gpc.serviceChanged(new ServiceEvent(ServiceEvent.MODIFIED, sr));
        assertEquals("Changing the service should not change the number of proxies", 1, gpc.proxyMap.size());

        assertFalse("The roles property set on the modified service should have been removed",
                Arrays.asList(reg.getReference().getPropertyKeys()).contains(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY));
        assertEquals(Boolean.TRUE, reg.getReference().getProperty(GuardProxyCatalog.PROXY_SERVICE_KEY));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testServiceFactoryBehaviour() throws Exception {
        final Map<ServiceReference, Object> serviceMap = new HashMap<>();
        TestServiceAPI testService = new TestService();

        BundleContext bc = mockConfigAdminBundleContext();
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        // The service being proxied has these properties
        long serviceID = 117L;
        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {TestServiceAPI.class.getName()});
        serviceProps.put(Constants.SERVICE_ID, serviceID);
        serviceProps.put("bar", 42L);

        BundleContext providerBC = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(providerBC.registerService(
                EasyMock.isA(String[].class),
                EasyMock.anyObject(),
                EasyMock.isA(Dictionary.class))).andAnswer((IAnswer) () -> {
                    Dictionary<String,Object> props = (Dictionary<String, Object>) EasyMock.getCurrentArguments()[2];
                    ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);
                    ServiceReference sr = mockServiceReference(props);
                    EasyMock.expect(reg.getReference()).andReturn(sr).anyTimes();
                    EasyMock.replay(reg);

                    serviceMap.put(sr, EasyMock.getCurrentArguments()[1]);

                    return reg;
                }).once();
        EasyMock.expect(providerBC.getService(EasyMock.isA(ServiceReference.class))).andAnswer(
                () -> serviceMap.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(providerBC);

        // In some cases the proxy-creating code is looking for a classloader (e.g. when run through
        // a coverage tool such as EclEmma). This will satisfy that.
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
        EasyMock.replay(bw);

        // The mock bundle that provides the original service (and also the proxy is registered with this)
        Bundle providerBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(providerBC).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        EasyMock.replay(providerBundle);

        ServiceReference sr = mockServiceReference(providerBundle, serviceProps);

        // The mock bundle context for the client bundle
        BundleContext clientBC = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(clientBC.getService(sr)).andReturn(testService).anyTimes();
        EasyMock.replay(clientBC);

        // The mock bundle that consumes the service
        Bundle clientBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(clientBundle.getBundleContext()).andReturn(clientBC).anyTimes();
        EasyMock.replay(clientBundle);

        gpc.proxyIfNotAlreadyProxied(sr);

        // The actual proxy creation is done asynchronously.
        GuardProxyCatalog.ServiceRegistrationHolder holder = gpc.proxyMap.get(serviceID);
        // Mimic the thread that works the queue to create the proxy
        GuardProxyCatalog.CreateProxyRunnable runnable = gpc.createProxyQueue.take();
        assertEquals(117L, runnable.getOriginalServiceID());

        ProxyManager pm = getProxyManager();
        runnable.run(pm);

        // The runnable should have put the actual registration in the holder
        ServiceReference<?> proxySR = holder.registration.getReference();

        // Check that the proxy registration was done on the original provider bundle's context
        EasyMock.verify(providerBC);

        // Test that the actual proxy invokes the original service...
        ServiceFactory proxyServiceSF = (ServiceFactory) serviceMap.get(proxySR);
        TestServiceAPI proxyService = (TestServiceAPI) proxyServiceSF.getService(clientBundle, null);

        assertNotSame("The proxy should not be the same object as the original service", testService, proxyService);
        assertEquals("Doing it", proxyService.doit());

        EasyMock.reset(clientBC);
        EasyMock.expect(clientBC.ungetService(sr)).andReturn(true).once();
        EasyMock.replay(clientBC);

        proxyServiceSF.ungetService(clientBundle, null, proxyService);

        EasyMock.verify(clientBC);
    }

    @SuppressWarnings("unchecked")
    public Dictionary<String, Object> testCreateProxy(Class<?> intf, Object testService) throws Exception {
        return testCreateProxy(mockConfigAdminBundleContext(), intf, intf, testService);
    }

    public Dictionary<String, Object> testCreateProxy(BundleContext bc, Class<?> intf, Object testService) throws Exception {
        return testCreateProxy(bc, intf, intf, testService);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Dictionary<String, Object> testCreateProxy(BundleContext bc, Class intf, final Class proxyRegClass, Object testService) throws Exception {
        // Create the object that is actually being tested here
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        // The service being proxied has these properties
        long serviceID = 456L;
        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.OBJECTCLASS, new String [] {intf.getName()});
        serviceProps.put(Constants.SERVICE_ID, serviceID);
        serviceProps.put(".foo", 123L);

        final Map<ServiceReference<?>, Object> serviceMap = new HashMap<>();

        // The mock bundle context for the bundle providing the service is set up here
        BundleContext providerBC = EasyMock.createMock(BundleContext.class);
        // These are the expected service properties of the proxy registration. Note the proxy marker...
        final Hashtable<String, Object> expectedProxyProps = new Hashtable<>(serviceProps);
        expectedProxyProps.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        // This will check that the right proxy is being registered.
        EasyMock.expect(providerBC.registerService(
                EasyMock.isA(String[].class),
                EasyMock.anyObject(),
                EasyMock.isA(Dictionary.class))).andAnswer((IAnswer) () -> {
                    if (!runningUnderCoverage) {
                        // Some of these checks don't work when running under coverage
                        assertArrayEquals(new String [] {proxyRegClass.getName()},
                                (String []) EasyMock.getCurrentArguments()[0]);

                        Object svc = EasyMock.getCurrentArguments()[1];
                        assertTrue(svc instanceof ServiceFactory);
                    }

                    Dictionary<String,Object> props = (Dictionary<String, Object>) EasyMock.getCurrentArguments()[2];
                    for (String key : expectedProxyProps.keySet()) {
                        assertEquals(expectedProxyProps.get(key), props.get(key));
                    }

                    ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);
                    ServiceReference sr = mockServiceReference(props);
                    EasyMock.expect(reg.getReference()).andReturn(sr).anyTimes();
                    reg.unregister();
                    EasyMock.expectLastCall().once();
                    EasyMock.replay(reg);

                    serviceMap.put(sr, EasyMock.getCurrentArguments()[1]);

                    return reg;
                }).once();
        EasyMock.expect(providerBC.getService(EasyMock.isA(ServiceReference.class))).andAnswer(
                () -> serviceMap.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(providerBC);

        // In some cases the proxy-creating code is looking for a classloader (e.g. when run through
        // a coverage tool such as EclEmma). This will satisfy that.
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
        EasyMock.replay(bw);

        // The mock bundle that provides the original service (and also the proxy is registered with this)
        Bundle providerBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(providerBC).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        EasyMock.replay(providerBundle);

        ServiceReference sr = mockServiceReference(providerBundle, serviceProps);

        assertEquals("Precondition", 0, gpc.proxyMap.size());
        assertEquals("Precondition", 0, gpc.createProxyQueue.size());
        // Create the proxy for the service
        gpc.proxyIfNotAlreadyProxied(sr);
        assertEquals(1, gpc.proxyMap.size());

        // The actual proxy creation is done asynchronously.
        GuardProxyCatalog.ServiceRegistrationHolder holder = gpc.proxyMap.get(serviceID);
        assertNull("The registration shouldn't have happened yet", holder.registration);
        assertEquals(1, gpc.createProxyQueue.size());

        // Mimic the thread that works the queue to create the proxy
        GuardProxyCatalog.CreateProxyRunnable runnable = gpc.createProxyQueue.take();
        ProxyManager pm = getProxyManager();
        runnable.run(pm);

        // The runnable should have put the actual registration in the holder
        ServiceReference<?> proxySR = holder.registration.getReference();
        for (String key : expectedProxyProps.keySet()) {
            assertEquals(expectedProxyProps.get(key), proxySR.getProperty(key));
        }

        // Check that the proxy registration was done on the original provider bundle's context
        EasyMock.verify(providerBC);

        // Test that the actual proxy invokes the original service...
        Object proxyService = serviceMap.get(proxySR);
        assertNotSame("The proxy should not be the same object as the original service", testService, proxyService);

        // Attempt to proxy the service again, make sure that no re-proxying happens
        assertEquals("Precondition", 1, gpc.proxyMap.size());
        assertEquals("Precondition", 0, gpc.createProxyQueue.size());
        gpc.proxyIfNotAlreadyProxied(sr);
        assertEquals("No additional proxy should have been created", 1, gpc.proxyMap.size());
        assertEquals("No additional work on the queue is expected", 0, gpc.createProxyQueue.size());

        Dictionary<String, Object> proxyProps = getServiceReferenceProperties(proxySR);

        gpc.close();
        EasyMock.verify(holder.registration); // checks that the unregister call was made

        return proxyProps;
    }

    @SuppressWarnings("unchecked")
    public Object testCreateProxy(Class<?> [] objectClasses, Object testService) throws Exception {
        return testCreateProxy(mockConfigAdminBundleContext(), objectClasses, objectClasses, testService, new HashMap<>());
    }

    public Object testCreateProxy(BundleContext bc, Class<?> [] objectClasses, Object testService) throws Exception {
        return testCreateProxy(bc, objectClasses, objectClasses, testService, new HashMap<>());
    }

    @SuppressWarnings("rawtypes")
    public Object testCreateProxy(BundleContext bc, Class<?> [] objectClasses, Object testService, Map<ServiceReference, Object> serviceMap) throws Exception {
        return testCreateProxy(bc, objectClasses, objectClasses, testService, serviceMap);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object testCreateProxy(BundleContext bc, Class [] objectClasses, final Class [] proxyRegClasses, Object testService, final Map<ServiceReference, Object> serviceMap) throws Exception {
        // A linked hash map to keep iteration order over the keys predictable
        final LinkedHashMap<String, Class> objClsMap = new LinkedHashMap<>();
        for (Class cls : objectClasses) {
            objClsMap.put(cls.getName(), cls);
        }

        // A linked hash map to keep iteration order over the keys predictable
        final LinkedHashMap<String, Class> proxyRegClsMap = new LinkedHashMap<>();
        for (Class cls : proxyRegClasses) {
            proxyRegClsMap.put(cls.getName(), cls);
        }

        // Create the object that is actually being tested here
        GuardProxyCatalog gpc = new GuardProxyCatalog(bc);

        // The service being proxied has these properties
        long serviceID = Long.MAX_VALUE;
        final Hashtable<String, Object> serviceProps = new Hashtable<>();
        serviceProps.put(Constants.OBJECTCLASS, objClsMap.keySet().toArray(new String [] {}));
        serviceProps.put(Constants.SERVICE_ID, serviceID);
        serviceProps.put(GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY, Collections.singletonList("everyone")); // will be overwritten
        serviceProps.put("bar", "foo");

        // The mock bundle context for the bundle providing the service is set up here
        BundleContext providerBC = EasyMock.createMock(BundleContext.class);
        // These are the expected service properties of the proxy registration. Note the proxy marker...
        final Hashtable<String, Object> expectedProxyProps = new Hashtable<>(serviceProps);
        expectedProxyProps.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        // This will check that the right proxy is being registered.
        EasyMock.expect(providerBC.registerService(
                EasyMock.isA(String[].class),
                EasyMock.anyObject(),
                EasyMock.isA(Dictionary.class))).andAnswer((IAnswer) () -> {
                    if (!runningUnderCoverage) {
                        // Some of these checks don't work when running under coverage
                        assertArrayEquals(proxyRegClsMap.keySet().toArray(new String [] {}),
                                (String []) EasyMock.getCurrentArguments()[0]);

                        Object svc = EasyMock.getCurrentArguments()[1];
                        assertTrue(svc instanceof ServiceFactory);
                    }

                    Dictionary<String,Object> props = (Dictionary<String, Object>) EasyMock.getCurrentArguments()[2];
                    for (String key : expectedProxyProps.keySet()) {
                        if (GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY.equals(key)) {
                            assertTrue("The roles property should have been overwritten",
                                    !Collections.singletonList("everyone").equals(props.get(key)));
                        } else {
                            assertEquals(expectedProxyProps.get(key), props.get(key));
                        }
                    }

                    ServiceRegistration reg = EasyMock.createMock(ServiceRegistration.class);
                    ServiceReference sr = mockServiceReference(props);
                    EasyMock.expect(reg.getReference()).andReturn(sr).anyTimes();
                    reg.unregister();
                    EasyMock.expectLastCall().once();
                    EasyMock.replay(reg);

                    serviceMap.put(sr, EasyMock.getCurrentArguments()[1]);

                    return reg;
                }).once();
        EasyMock.expect(providerBC.getService(EasyMock.isA(ServiceReference.class))).andAnswer(
                () -> serviceMap.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(providerBC);

        // In some cases the proxy-creating code is looking for a classloader (e.g. when run through
        // a coverage tool such as EclEmma). This will satisfy that.
        BundleWiring bw = EasyMock.createMock(BundleWiring.class);
        EasyMock.expect(bw.getClassLoader()).andReturn(getClass().getClassLoader()).anyTimes();
        EasyMock.replay(bw);

        // The mock bundle that provides the original service (and also the proxy is registered with this)
        Bundle providerBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(providerBundle.getBundleContext()).andReturn(providerBC).anyTimes();
        EasyMock.expect(providerBundle.adapt(BundleWiring.class)).andReturn(bw).anyTimes();
        EasyMock.replay(providerBundle);

        ServiceReference sr = mockServiceReference(providerBundle, serviceProps);

        // The mock bundle context for the client bundle
        BundleContext clientBC = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(clientBC.getService(sr)).andReturn(testService).anyTimes();
        EasyMock.replay(clientBC);

        // The mock bundle that consumes the service
        Bundle clientBundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(clientBundle.getBundleId()).andReturn(2999L).anyTimes();
        EasyMock.expect(clientBundle.getBundleContext()).andReturn(clientBC).anyTimes();
        EasyMock.expect(clientBundle.loadClass(EasyMock.isA(String.class))).andAnswer(
                (IAnswer) () -> objClsMap.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(clientBundle);

        assertEquals("Precondition", 0, gpc.proxyMap.size());
        assertEquals("Precondition", 0, gpc.createProxyQueue.size());
        // Create the proxy for the service
        gpc.proxyIfNotAlreadyProxied(sr);
        assertEquals(1, gpc.proxyMap.size());

        // The actual proxy creation is done asynchronously.
        GuardProxyCatalog.ServiceRegistrationHolder holder = gpc.proxyMap.get(serviceID);
        assertNull("The registration shouldn't have happened yet", holder.registration);
        assertEquals(1, gpc.createProxyQueue.size());

        // Mimic the thread that works the queue to create the proxy
        GuardProxyCatalog.CreateProxyRunnable runnable = gpc.createProxyQueue.take();
        ProxyManager pm = getProxyManager();
        runnable.run(pm);

        // The runnable should have put the actual registration in the holder
        ServiceReference<?> proxySR = holder.registration.getReference();
        for (String key : expectedProxyProps.keySet()) {
            if (GuardProxyCatalog.SERVICE_GUARD_ROLES_PROPERTY.equals(key)) {
                assertTrue("The roles property should have been overwritten",
                        !Collections.singletonList("everyone").equals(proxySR.getProperty(key)));
            } else {
                assertEquals(expectedProxyProps.get(key), proxySR.getProperty(key));
            }
        }

        // Check that the proxy registration was done on the original provider bundle's context
        EasyMock.verify(providerBC);

        // Test that the actual proxy invokes the original service...
        ServiceFactory proxyServiceSF = (ServiceFactory) serviceMap.get(proxySR);
        Object proxyService = null;
        try {
            proxyService = proxyServiceSF.getService(clientBundle, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        assertNotSame("The proxy should not be the same object as the original service", testService, proxyService);

        return proxyService;
    }

    private ProxyManager getProxyManager() {
        return new AsmProxyManager();
    }

    private Dictionary<String, Object> getServiceReferenceProperties(ServiceReference<?> sr) {
        Dictionary<String, Object> dict = new Hashtable<>();

        for (String key : sr.getPropertyKeys()) {
            dict.put(key, sr.getProperty(key));
        }

        return dict;
    }

    private Bundle mockBundle(long id) {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(id).anyTimes();
        EasyMock.replay(bundle);
        return bundle;
    }

    private BundleContext mockBundleContext() throws InvalidSyntaxException {
        return mockBundleContext(null);
    }

    private BundleContext mockBundleContext(Bundle b) throws InvalidSyntaxException {
        if (b == null) {
            b = EasyMock.createNiceMock(Bundle.class);
            EasyMock.expect(b.getBundleId()).andReturn(89334L).anyTimes();
            EasyMock.replay(b);
        }

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(
                () -> FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.replay(bc);
        return bc;
    }

    private BundleContext openStrictMockBundleContext(Bundle b) throws InvalidSyntaxException {
        BundleContext bc = EasyMock.createMock(BundleContext.class);
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(
                () -> FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0])).anyTimes();
        if (b != null) {
            EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        }
        return bc;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private BundleContext mockConfigAdminBundleContext(Dictionary<String, Object> ... configs) throws IOException,
            InvalidSyntaxException {
        Configuration [] configurations = new Configuration[configs.length];

        for (int i = 0; i < configs.length; i++) {
            Configuration conf = EasyMock.createMock(Configuration.class);
            EasyMock.expect(conf.getProcessedProperties(null)).andReturn(configs[i]).anyTimes();
            EasyMock.expect(conf.getProperties()).andReturn(configs[i]).anyTimes();
            EasyMock.expect(conf.getPid()).andReturn((String) configs[i].get(Constants.SERVICE_PID)).anyTimes();
            EasyMock.replay(conf);
            configurations[i] = conf;
        }
        if (configurations.length == 0) {
            configurations = null;
        }

        ConfigurationAdmin ca = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(ca.listConfigurations("(&(service.pid=org.apache.karaf.service.acl.*)(service.guard=*))")).andReturn(configurations).anyTimes();
        EasyMock.replay(ca);

        final ServiceReference caSR = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(caSR);

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getBundleId()).andReturn(877342449L).anyTimes();
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(
                () -> FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0])).anyTimes();
        String cmFilter = "(&(objectClass=" + ConfigurationAdmin.class.getName() + ")"
                + "(!(" + GuardProxyCatalog.PROXY_SERVICE_KEY + "=*)))";
        bc.addServiceListener(EasyMock.isA(ServiceListener.class), EasyMock.eq(cmFilter));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(bc.getServiceReferences(EasyMock.anyObject(String.class), EasyMock.eq(cmFilter))).
                andReturn(new ServiceReference<?> [] {caSR}).anyTimes();
        EasyMock.expect(bc.getService(caSR)).andReturn(ca).anyTimes();
        EasyMock.replay(bc);
        return bc;
    }

    private ServiceReference<?> mockServiceReference(final Dictionary<String, Object> props) {
        return mockServiceReference(props, Object.class);
    }

    @SuppressWarnings("unchecked")
    private <T> ServiceReference<T> mockServiceReference(Dictionary<String, Object> props, Class<T> cls) {
        return (ServiceReference<T>) mockServiceReference(null, props);
    }

    private ServiceReference<?> mockServiceReference(Bundle providerBundle,
                                                     final Dictionary<String, Object> serviceProps) {
        ServiceReference<?> sr = EasyMock.createMock(ServiceReference.class);

        // Make sure the properties are 'live' in that if they change the reference changes too
        EasyMock.expect(sr.getPropertyKeys()).andAnswer(
                () -> Collections.list(serviceProps.keys()).toArray(new String [] {})).anyTimes();
        EasyMock.expect(sr.getProperty(EasyMock.isA(String.class))).andAnswer(
                () -> serviceProps.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        if (providerBundle != null) {
            EasyMock.expect(sr.getBundle()).andReturn(providerBundle).anyTimes();
        }
        EasyMock.replay(sr);
        return sr;
    }

    class MockCreateProxyRunnable implements CreateProxyRunnable {
        private final long orgServiceID;

        public MockCreateProxyRunnable(long serviceID) {
            orgServiceID = serviceID;
        }

        @Override
        public long getOriginalServiceID() {
            return orgServiceID;
        }

        @Override
        public void run(ProxyManager pm) throws Exception {}
    }

    public interface TestServiceAPI {
        String doit();
    }

    public class TestService implements TestServiceAPI {
        @Override
        public String doit() {
            return "Doing it";
        }
    }

    public interface TestServiceAPI2 {
        String doit(String s);
    }

    public interface TestServiceAPI3 {
        int foo();
        int foo(int f);
        int bar();
    }

    class TestService3 implements TestServiceAPI3 {
        public int foo() {
            return 42;
        }

        public int foo(int f) {
            return -f;
        }

        public int bar() {
            return 99;
        }
    }

    public class TestObjectWithoutInterface {
        public long compute(long l) {
            return -l;
        }
    }

    public class CombinedTestService extends TestObjectWithoutInterface implements TestServiceAPI {
        @Override
        public String doit() {
            return "Doing it";
        }
    }

    private abstract class AbstractService implements TestServiceAPI {
        @Override
        public String doit() {
            return "Doing it";
        }
    }

    public class EmptyPublicTestService extends AbstractService {}

    public class DescendantTestService extends EmptyPublicTestService {}

    private class PrivateTestService implements TestServiceAPI {
        @Override
        public String doit() {
            return "Doing it";
        }
    }

    private class PrivateTestServiceNoDirectInterfaces extends PrivateTestService {}

    public final class FinalTestService extends AbstractService implements TestServiceAPI {}

    public class ClassWithFinalMethod {
        public void foo() {}
        public final String bar() { return "Bar"; }
    }

    public class ClassWithPrivateMethod {
        public void foo() {}
        @SuppressWarnings("unused")
        private void bar() {}
    }
}
