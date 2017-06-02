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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;

import java.util.Dictionary;
import java.util.Properties;

public class ActivatorTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testStartActivator() throws Exception {
        // keep the old properties. Note that the Properties 'copy constructor' new Properties(props)
        // doesn't actually copy, hence the awkward setup here...
        Properties oldProps = new Properties();
        oldProps.putAll(System.getProperties());

        try {
            System.setProperty(GuardProxyCatalog.KARAF_SECURED_SERVICES_SYSPROP, "(foo=bar)");
            Bundle b = EasyMock.createMock(Bundle.class);
            EasyMock.expect(b.getBundleId()).andReturn(768L).anyTimes();
            EasyMock.replay(b);

            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
            EasyMock.expect(bc.createFilter(EasyMock.anyObject(String.class))).andAnswer(
                    () -> FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0])).anyTimes();

            EasyMock.expect(bc.registerService(
                    EasyMock.eq(EventListenerHook.class), EasyMock.isA(EventListenerHook.class), EasyMock.isNull(Dictionary.class)))
                    .andReturn(null);
            EasyMock.expect(bc.registerService(
                    EasyMock.eq(FindHook.class), EasyMock.isA(FindHook.class), EasyMock.isNull(Dictionary.class)))
                    .andReturn(null);

            EasyMock.replay(bc);

            Activator a = new Activator();
            a.start(bc);

            assertNotNull(a.guardProxyCatalog);
            assertNotNull(a.guardingEventHook);
            assertNotNull(a.guardingFindHook);

            EasyMock.verify(bc);
        } finally {
            System.setProperties(oldProps);
        }
    }

    @Test
    public void testStartActivatorNoServicesSecured() throws Exception {
        // keep the old properties. Note that the Properties 'copy constructor' new Properties(props)
        // doesn't actually copy, hence the awkward setup here...
        Properties oldProps = new Properties();
        oldProps.putAll(System.getProperties());

        try {
            Properties newProps = removeProperties(System.getProperties(), GuardProxyCatalog.KARAF_SECURED_SERVICES_SYSPROP);
            System.setProperties(newProps);

            BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
            EasyMock.replay(bc);

            Activator a = new Activator();
            a.start(bc);

            assertNull(a.guardProxyCatalog);
        } finally {
            System.setProperties(oldProps);
        }
    }

    @Test
    public void testStopActivator() throws Exception {
        Activator a = new Activator();

        a.guardProxyCatalog = EasyMock.createMock(GuardProxyCatalog.class);
        a.guardProxyCatalog.close();
        EasyMock.expectLastCall().once();
        EasyMock.replay(a.guardProxyCatalog);

        a.stop(EasyMock.createMock(BundleContext.class));

        EasyMock.verify(a.guardProxyCatalog);
    }

    private Properties removeProperties(Properties props, String ... keys) {
        Properties p = new Properties();
        p.putAll(props);
        for (String key : keys) {
            p.remove(key);
        }
        return p;
    }

}
