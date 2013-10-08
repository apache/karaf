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
package org.apache.karaf.management.boot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.easymock.EasyMock;

public class KarafMBeanServerBuilderTest extends TestCase {

    public void testMBeanServerBuilderBlocking() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        KarafMBeanServerBuilder mbsb = new KarafMBeanServerBuilder();
        MBeanServer kmbs = mbsb.newMBeanServer("test", mbs, null);

        final List<Object> handlerArgs = new ArrayList<Object>();
        InvocationHandler guard = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                handlerArgs.add(proxy);
                handlerArgs.add(method);
                handlerArgs.add(args);

                throw new SecurityException("Access Denied");
            }
        };

        KarafMBeanServerBuilder.setGuard(guard);

        assertEquals("Precondition", 0, handlerArgs.size());

        ObjectName on = ObjectName.getInstance("foo.bar:type=TestObject");

        try {
            // obtain a JMX attribute
            kmbs.getAttribute(on, "myAttr");
            fail("Should have access denied");
        } catch (SecurityException se) {
            // good
            assertEquals(3, handlerArgs.size());
            assertSame(kmbs, handlerArgs.get(0));
            assertEquals("getAttribute", ((Method) handlerArgs.get(1)).getName());
            Object[] args = (Object[]) handlerArgs.get(2);
            assertEquals(2, args.length);
            assertSame(on, args[0]);
            assertEquals("myAttr", args[1]);
        }

        try {
            // obtain a number of JMX attributes
            kmbs.getAttributes(on, new String[]{"foo", "bar"});
            fail("Should have access denied");
        } catch (SecurityException se) {
            // good
        }

        try {
            // set a JMX attribute
            kmbs.setAttribute(on, new Attribute("goo", "far"));
            fail("Should have access denied");
        } catch (SecurityException se) {
            // good
        }

        try {
            // set a number of JMX attributes
            kmbs.setAttributes(on, new AttributeList());
            fail("Should have access denied");
        } catch (SecurityException se) {
            // good
        }

        try {
            // mimic a JMX method invocation
            kmbs.invoke(on, "foo", new Object [] {}, new String [] {});
            fail("Should have access denied");
        } catch (SecurityException se) {
            // good
        }

        // try some MBeanServer operations that are not guarded
        assertTrue(kmbs.getDomains().length > 0);
        assertTrue(kmbs.getMBeanCount() > 0);
        assertTrue(kmbs.getDefaultDomain().length() > 0);
    }

    public void testMBeanServerBuilderNonBlocking() throws Exception {
        MBeanServer mbs = EasyMock.createMock(MBeanServer.class);
        EasyMock.replay(mbs);

        KarafMBeanServerBuilder mbsb = new KarafMBeanServerBuilder();
        MBeanServer kmbs = mbsb.newMBeanServer("test", mbs, null);

        final List<Object> handlerArgs = new ArrayList<Object>();
        InvocationHandler guard = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                handlerArgs.add(proxy);
                handlerArgs.add(method);
                handlerArgs.add(args);
                return null;
            }
        };

        KarafMBeanServerBuilder.setGuard(guard);

        assertEquals("Precondition", 0, handlerArgs.size());

        ObjectName on = ObjectName.getInstance("foo.bar:type=TestObject");

        try {
            kmbs.getAttribute(on, "myAttr");
        } catch (Exception e) {
            Throwable th = getInnermostException(e);
            assertTrue("Expected exception as the object in question is not registered with the MBeanServer", th instanceof InstanceNotFoundException);
            // good
            assertEquals(3, handlerArgs.size());
            assertSame(kmbs, handlerArgs.get(0));
            assertEquals("getAttribute", ((Method) handlerArgs.get(1)).getName());
            Object[] args = (Object[]) handlerArgs.get(2);
            assertEquals(2, args.length);
            assertSame(on, args[0]);
            assertEquals("myAttr", args[1]);
        }
    }

    private Throwable getInnermostException(Throwable th) {
        if (th.getCause() != null) {
            return getInnermostException(th.getCause());
        } else {
            return th;
        }
    }

}
