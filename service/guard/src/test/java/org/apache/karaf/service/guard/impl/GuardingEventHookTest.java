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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class GuardingEventHookTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testEventHookEvents() throws Exception {
        BundleContext frameworkBC = mockBundleContext(0L);

        Dictionary<String, Object> config = new Hashtable<String, Object>();
        config.put("service.guard", "(service.id=*)");
        BundleContext hookBC = mockConfigAdminBundleContext(frameworkBC, config);
        GuardProxyCatalog gpc = new GuardProxyCatalog(hookBC);

        Filter serviceFilter = FrameworkUtil.createFilter("(foo=bar)");
        GuardingEventHook geh = new GuardingEventHook(hookBC, gpc, serviceFilter);

        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_ID, 13L);
        ServiceReference<?> sref = mockServiceReference(props);

        BundleContext client1BC = mockBundleContext(123L);
        Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
        listeners.put(client1BC, Collections.<ListenerInfo>emptyList());

        // Send the event. It should have no effect because the service doens't match the filter
        assertEquals("Precondition", 0, gpc.proxyMap.size());
        geh.event(new ServiceEvent(ServiceEvent.REGISTERED, sref), listeners);
        assertEquals("No proxy should have been created because the service doesn't match the filter", 0, gpc.proxyMap.size());
        assertEquals("Nothing should have been removed from the listeners", 1, listeners.size());

        long service2ID = 887L;
        Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put(Constants.SERVICE_ID, service2ID);
        props2.put("a", "b");
        props2.put("foo", "bar");
        ServiceReference<?> sref2 = mockServiceReference(props2);
        ServiceEvent se2 = new ServiceEvent(ServiceEvent.REGISTERED, sref2);

        // Send the event to the system bundle and the bundle that contains the hook, should have no effect
        Map<BundleContext, Collection<ListenerInfo>> listeners2 = new Hashtable<BundleContext, Collection<ListenerInfo>>();
        listeners2.put(frameworkBC, Collections.<ListenerInfo>emptyList());
        listeners2.put(hookBC, Collections.<ListenerInfo>emptyList());
        geh.event(se2, listeners2);
        assertEquals("No proxies to be created for the hook bundle or the system bundle", 0, gpc.proxyMap.size());
        assertEquals("Nothing should have been removed from the listeners", 2, listeners2.size());

        // This is the first time that a proxy actually does get created
        Map<BundleContext, Collection<ListenerInfo>> listeners3 = new HashMap<BundleContext, Collection<ListenerInfo>>();
        listeners3.put(client1BC, Collections.<ListenerInfo>emptyList());
        geh.event(se2, listeners3);
        assertEquals("The service should be hidden from these listeners", 0, listeners3.size());
        assertEquals("Proxy should have been created for this client", 1, gpc.proxyMap.size());
        assertEquals(new Long(service2ID), gpc.proxyMap.keySet().iterator().next());

        // Update the service, now an additional client is interested
        props2.put("a", "c"); // Will change the properties of sref
        Map<BundleContext, Collection<ListenerInfo>> listeners4 = new HashMap<BundleContext, Collection<ListenerInfo>>();

        BundleContext client2BC = mockBundleContext(11);
        listeners4.put(client2BC, Collections.<ListenerInfo>emptyList());
        listeners4.put(client1BC, Collections.<ListenerInfo>emptyList());
        geh.event(new ServiceEvent(ServiceEvent.MODIFIED, sref2), listeners4);
        assertEquals("The service should be hidden from these listeners", 0, listeners4.size());
        assertEquals("There should not be an additional proxy for client 2", 1, gpc.proxyMap.size());
        assertNotNull(gpc.proxyMap.get(service2ID));

        long service3ID = 1L;
        Dictionary<String, Object> props3 = new Hashtable<String, Object>();
        props3.put(Constants.SERVICE_ID, service3ID);
        props3.put("foo", "bar");
        ServiceReference<?> sref3 = mockServiceReference(props3);

        // An event for a new service
        Map<BundleContext, Collection<ListenerInfo>> listeners5 = new HashMap<BundleContext, Collection<ListenerInfo>>();
        listeners5.put(client1BC, Collections.<ListenerInfo>emptyList());
        listeners5.put(client1BC, Collections.<ListenerInfo>emptyList()); // Should be ignored
        geh.event(new ServiceEvent(ServiceEvent.REGISTERED, sref3), listeners5);
        assertEquals("There should be an additional procy for client1 to the new service", 2, gpc.proxyMap.size());
        assertEquals("The service should be hidden from these listeners", 0, listeners5.size());
        assertNotNull(gpc.proxyMap.get(service2ID));
        assertNotNull(gpc.proxyMap.get(service3ID));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEventHookProxyEvents() throws Exception {
        BundleContext frameworkBC = mockBundleContext(0L);

        Dictionary<String, Object> config = new Hashtable<String, Object>();
        config.put("service.guard", "(service.id=*)");
        BundleContext hookBC = mockConfigAdminBundleContext(frameworkBC, config);
        GuardProxyCatalog gpc = new GuardProxyCatalog(hookBC);

        Filter serviceFilter = FrameworkUtil.createFilter("(service.id=*)"); // any service will match
        GuardingEventHook geh = new GuardingEventHook(hookBC, gpc, serviceFilter);

        BundleContext client1BC = mockBundleContext(123L);

        // Create a proxy service mock
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_ID, 13L);
        props.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        ServiceReference<?> sref = mockServiceReference(props);
        Map<BundleContext, Collection<ListenerInfo>> listeners = new HashMap<BundleContext, Collection<ListenerInfo>>();
        listeners.put(client1BC, Collections.<ListenerInfo>emptyList());

        // Send the event. It should have no effect because the service is already a proxy for the client
        assertEquals("Precondition", 0, gpc.proxyMap.size());
        geh.event(new ServiceEvent(ServiceEvent.REGISTERED, sref), listeners);
        assertEquals("No changes expected for the proxy map.", 0, gpc.proxyMap.size());
        assertEquals("The event should be delivered to the client", 1, listeners.size());

        // send the event to a different client, this client should also see the event as the proxy Service Factory is shared
        Map<BundleContext, Collection<ListenerInfo>> listeners2 = new HashMap<BundleContext, Collection<ListenerInfo>>();
        listeners2.put(mockBundleContext(51L), Collections.<ListenerInfo>emptyList());
        geh.event(new ServiceEvent(ServiceEvent.REGISTERED, sref), listeners2);
        assertEquals("No changes expected for the proxy map.", 0, gpc.proxyMap.size());
        assertEquals("The event should be delivered to the client", 1, listeners2.size());
    }

    @Test
    public void testEventHookNoFilter() throws Exception {
        BundleContext hookBC = mockBundleContext(5L);
        GuardProxyCatalog gpc = new GuardProxyCatalog(hookBC);

        GuardingEventHook geh = new GuardingEventHook(hookBC, gpc, null);
        geh.event(null, null); // Should do nothing
    }

    private BundleContext mockBundleContext(long id) throws Exception {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(id).anyTimes();

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(new IAnswer<Filter>() {
            @Override
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(bc);

        EasyMock.expect(bundle.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.replay(bundle);

        return bc;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private BundleContext mockConfigAdminBundleContext(BundleContext frameworkContext, Dictionary<String, Object> ... configs) throws IOException,
            InvalidSyntaxException {
        Configuration [] configurations = new Configuration[configs.length];

        for (int i = 0; i < configs.length; i++) {
            Configuration conf = EasyMock.createMock(Configuration.class);
            EasyMock.expect(conf.getProperties()).andReturn(configs[i]).anyTimes();
            EasyMock.expect(conf.getPid()).andReturn((String) configs[i].get(Constants.SERVICE_PID)).anyTimes();
            EasyMock.replay(conf);
            configurations[i] = conf;
        }

        ConfigurationAdmin ca = EasyMock.createMock(ConfigurationAdmin.class);
        EasyMock.expect(ca.listConfigurations("(&(service.pid=org.apache.karaf.service.acl.*)(service.guard=*))")).andReturn(configurations).anyTimes();
        EasyMock.replay(ca);

        final ServiceReference caSR = EasyMock.createMock(ServiceReference.class);
        EasyMock.replay(caSR);

        Bundle sb = EasyMock.createMock(Bundle.class);
        EasyMock.expect(sb.getBundleId()).andReturn(0L).anyTimes();
        EasyMock.expect(sb.getBundleContext()).andReturn(frameworkContext).anyTimes();
        EasyMock.replay(sb);

        Bundle b = EasyMock.createMock(Bundle.class);
        EasyMock.expect(b.getBundleId()).andReturn(877342449L).anyTimes();
        EasyMock.replay(b);

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(b).anyTimes();
        EasyMock.expect(bc.getBundle(0L)).andReturn(sb).anyTimes();
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(new IAnswer<Filter>() {
            @Override
            public Filter answer() throws Throwable {
                return FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
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
        ServiceReference<?> sr = EasyMock.createMock(ServiceReference.class);

        // Make sure the properties are 'live' in that if they change the reference changes too
        EasyMock.expect(sr.getPropertyKeys()).andAnswer(new IAnswer<String[]>() {
            @Override
            public String[] answer() throws Throwable {
                return Collections.list(props.keys()).toArray(new String [] {});
            }
        }).anyTimes();
        EasyMock.expect(sr.getProperty(EasyMock.isA(String.class))).andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                return props.get(EasyMock.getCurrentArguments()[0]);
            }
        }).anyTimes();
        EasyMock.replay(sr);
        return sr;
    }
}
