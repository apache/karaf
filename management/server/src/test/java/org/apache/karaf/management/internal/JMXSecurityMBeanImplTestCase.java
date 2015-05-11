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
package org.apache.karaf.management.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import junit.framework.TestCase;

import org.apache.karaf.management.KarafMBeanServerGuard;
import org.easymock.EasyMock;

public class JMXSecurityMBeanImplTestCase extends TestCase {

    public void testMBeanServerAccessors() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        mb.setMBeanServer(mbs);
        assertSame(mbs, mb.getMBeanServer());
    }

    public void testCanInvokeMBean() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        String objectName = "foo.bar.testing:type=SomeMBean";
        KarafMBeanServerGuard testGuard = EasyMock.createMock(KarafMBeanServerGuard.class);
        EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName))).andReturn(true);
        EasyMock.replay(testGuard);

        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        mb.setMBeanServer(mbs);
        mb.setGuard(testGuard);
        assertTrue(mb.canInvoke(objectName));
    }

    public void testCanInvokeMBean2() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        String objectName = "foo.bar.testing:type=SomeMBean";
        KarafMBeanServerGuard testGuard = EasyMock.createMock(KarafMBeanServerGuard.class);
        EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName))).andReturn(false);
        EasyMock.replay(testGuard);

        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        mb.setMBeanServer(mbs);
        mb.setGuard(testGuard);
        assertFalse(mb.canInvoke(objectName));
    }

    public void testCanInvokeMBeanThrowsException() throws Exception {
        try {
            MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
            EasyMock.replay(mbs);

            String objectName = "foo.bar.testing:type=SomeMBean";
            KarafMBeanServerGuard testGuard = EasyMock.createMock(KarafMBeanServerGuard.class);
            EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName))).andThrow(new IOException());
            EasyMock.replay(testGuard);

            JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
            mb.setMBeanServer(mbs);
            mb.setGuard(testGuard);
            mb.canInvoke(objectName);
            fail("Should have thrown an exception");
        } catch (IOException ioe) {
            // good!
        }
    }

    public void testCanInvokeMBeanNoGuard() throws Exception {
        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        assertTrue(mb.canInvoke("foo.bar.testing:type=SomeMBean"));
    }

    public void testCanInvokeMethod() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        String objectName = "foo.bar.testing:type=SomeMBean";
        KarafMBeanServerGuard testGuard = EasyMock.createMock(KarafMBeanServerGuard.class);
        String[] la = new String[]{"long"};
        String[] sa = new String[]{"java.lang.String"};
        String[] sa2 = new String[]{"java.lang.String", "java.lang.String"};
        EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName), "testMethod", la)).andReturn(true);
        EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName), "testMethod", sa)).andReturn(true);
        EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName), "otherMethod", sa2)).andReturn(false);
        EasyMock.replay(testGuard);

        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        mb.setMBeanServer(mbs);
        mb.setGuard(testGuard);
        assertTrue(mb.canInvoke(objectName, "testMethod", la));
        assertTrue(mb.canInvoke(objectName, "testMethod", sa));
        assertFalse(mb.canInvoke(objectName, "otherMethod", sa2));
    }

    public void testCanInvokeMethodException() throws Exception {
        try {
            MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
            EasyMock.replay(mbs);

            String objectName = "foo.bar.testing:type=SomeMBean";
            KarafMBeanServerGuard testGuard = EasyMock.createMock(KarafMBeanServerGuard.class);
            String[] ea = new String[]{};
            EasyMock.expect(testGuard.canInvoke(mbs, new ObjectName(objectName), "testMethod", ea)).andThrow(new IOException());
            EasyMock.replay(testGuard);

            JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
            mb.setMBeanServer(mbs);
            mb.setGuard(testGuard);
            mb.canInvoke(objectName, "testMethod", ea);
            fail("Should have thrown an exception");
        } catch (IOException ioe) {
            // good
        }
    }

    public void testCanInvokeMethodNoGuard() throws Exception {
        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        assertTrue(mb.canInvoke("foo.bar.testing:type=SomeMBean", "someMethod", new String[]{}));
    }

    public void testCanInvokeBulk() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        KarafMBeanServerGuard testGuard = EasyMock.createMock(KarafMBeanServerGuard.class);
        String objectName = "foo.bar.testing:type=SomeMBean";
        final String[] la = new String[]{"long"};
        final String[] sa = new String[]{"java.lang.String"};
        EasyMock.expect(testGuard.canInvoke(EasyMock.eq(mbs), EasyMock.eq(new ObjectName(objectName)), EasyMock.eq("testMethod"), EasyMock.aryEq(la))).andReturn(true).anyTimes();
        EasyMock.expect(testGuard.canInvoke(EasyMock.eq(mbs), EasyMock.eq(new ObjectName(objectName)), EasyMock.eq("testMethod"), EasyMock.aryEq(sa))).andReturn(false).anyTimes();
        EasyMock.expect(testGuard.canInvoke(EasyMock.eq(mbs), EasyMock.eq(new ObjectName(objectName)), EasyMock.eq("otherMethod"))).andReturn(true).anyTimes();
        String objectName2 = "foo.bar.testing:type=SomeOtherMBean";
        EasyMock.expect(testGuard.canInvoke(EasyMock.eq(mbs), EasyMock.eq(new ObjectName(objectName2)))).andReturn(true).anyTimes();
        String objectName3 = "foo.bar.foo.testing:type=SomeOtherMBean";
        EasyMock.expect(testGuard.canInvoke(EasyMock.eq(mbs), EasyMock.eq(new ObjectName(objectName3)))).andReturn(false).anyTimes();
        EasyMock.replay(testGuard);

        JMXSecurityMBeanImpl mb = new JMXSecurityMBeanImpl();
        mb.setMBeanServer(mbs);
        mb.setGuard(testGuard);
        Map<String, List<String>> query = new HashMap<String, List<String>>();
        query.put(objectName, Arrays.asList("otherMethod", "testMethod(long)", "testMethod(java.lang.String)"));
        query.put(objectName2, Collections.<String>emptyList());
        query.put(objectName3, Collections.<String>emptyList());
        TabularData result = mb.canInvoke(query);
        assertEquals(5, result.size());

        CompositeData cd = result.get(new Object[]{objectName, "testMethod(long)"});
        assertEquals(objectName, cd.get("ObjectName"));
        assertEquals("testMethod(long)", cd.get("Method"));
        assertEquals(true, cd.get("CanInvoke"));
        CompositeData cd2 = result.get(new Object[]{objectName, "testMethod(java.lang.String)"});
        assertEquals(objectName, cd2.get("ObjectName"));
        assertEquals("testMethod(java.lang.String)", cd2.get("Method"));
        assertEquals(false, cd2.get("CanInvoke"));
        CompositeData cd3 = result.get(new Object[]{objectName, "otherMethod"});
        assertEquals(objectName, cd3.get("ObjectName"));
        assertEquals("otherMethod", cd3.get("Method"));
        assertEquals(true, cd3.get("CanInvoke"));
        CompositeData cd4 = result.get(new Object[]{objectName2, ""});
        assertEquals(objectName2, cd4.get("ObjectName"));
        assertEquals("", cd4.get("Method"));
        assertEquals(true, cd4.get("CanInvoke"));
        CompositeData cd5 = result.get(new Object[]{objectName3, ""});
        assertEquals(objectName3, cd5.get("ObjectName"));
        assertEquals("", cd5.get("Method"));
        assertEquals(false, cd5.get("CanInvoke"));
    }

}
