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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class GuardingFindHookTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testFindHook() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put("service.guard", "(|(moo=foo)(foo=*))");

        BundleContext hookBC = mockConfigAdminBundleContext(config);
        GuardProxyCatalog gpc = new GuardProxyCatalog(hookBC);

        Filter serviceFilter = FrameworkUtil.createFilter("(foo=*)");
        GuardingFindHook gfh = new GuardingFindHook(hookBC, gpc, serviceFilter);

        BundleContext clientBC = mockBundleContext(31L);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_ID, 16L);
        props.put("moo", "foo");
        ServiceReference<?> sref = mockServiceReference(props);

        Collection<ServiceReference<?>> refs = new ArrayList<>();
        refs.add(sref);

        assertEquals("Precondition", 0, gpc.proxyMap.size());
        gfh.find(clientBC, null, null, true, refs);
        assertEquals("The service doesn't match the filter so should have no effect", 0, gpc.proxyMap.size());
        assertEquals("The service doesn't match the filter so should be presented to the client",
                Collections.singletonList(sref), refs);

        long service2ID = 17L;
        Dictionary<String, Object> props2 = new Hashtable<>();
        props2.put(Constants.SERVICE_ID, service2ID);
        props2.put("foo", new Object());
        ServiceReference<?> sref2 = mockServiceReference(props2);

        Collection<ServiceReference<?>> refs2 = new ArrayList<>();
        refs2.add(sref2);

        gfh.find(clientBC, null, null, true, refs2);
        assertEquals("The service should be hidden from the client", 0, refs2.size());
        assertEquals("The service should have caused a proxy creation", 1, gpc.proxyMap.size());
        assertEquals("A proxy creation job should have been created", 1, gpc.createProxyQueue.size());
        assertEquals(sref2.getProperty(Constants.SERVICE_ID), gpc.proxyMap.keySet().iterator().next());

        Collection<ServiceReference<?>> refs3 = new ArrayList<>();
        refs3.add(sref2);

        // Ensure that the hook bundle has nothing hidden
        gfh.find(hookBC, null, null, true, refs3);
        assertEquals("The service should not be hidden from the hook bundle", Collections.singletonList(sref2), refs3);
        assertEquals("No proxy creation caused in this case", 1, gpc.proxyMap.size());
        assertEquals("No change expected", sref2.getProperty(Constants.SERVICE_ID), gpc.proxyMap.keySet().iterator().next());

        // Ensure that the system bundle has nothing hidden
        gfh.find(mockBundleContext(0L), null, null, true, refs3);
        assertEquals("The service should not be hidden from the framework bundle", Collections.singletonList(sref2), refs3);
        assertEquals("No proxy creation caused in this case", 1, gpc.proxyMap.size());
        assertEquals("No change expected", sref2.getProperty(Constants.SERVICE_ID), gpc.proxyMap.keySet().iterator().next());

        // Ensure that if we ask for the same client again, it will not create another proxy
        gpc.createProxyQueue.clear(); // Manually empty the queue
        gfh.find(clientBC, null, null, true, refs3);
        assertEquals("The service should be hidden from the client", 0, refs3.size());
        assertEquals("There is already a proxy for this client, no need for an additional one", 1, gpc.proxyMap.size());
        assertEquals("No additional jobs should have been scheduled", 0, gpc.createProxyQueue.size());
        assertEquals("No change expected", sref2.getProperty(Constants.SERVICE_ID), gpc.proxyMap.keySet().iterator().next());

        Collection<ServiceReference<?>> refs4 = new ArrayList<>();
        refs4.add(sref2);

        // another client should not get another proxy
        BundleContext client2BC = mockBundleContext(32768L);
        gfh.find(client2BC, null, null, true, refs4);
        assertEquals("The service should be hidden for this new client", 0, refs4.size());
        assertEquals("No proxy creation job should have been created", 0, gpc.createProxyQueue.size());
        assertEquals("No proxy creation caused in this case", 1, gpc.proxyMap.size());
        assertEquals("No change expected", sref2.getProperty(Constants.SERVICE_ID), gpc.proxyMap.keySet().iterator().next());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindHookProxyServices() throws Exception {
        Dictionary<String, Object> config = new Hashtable<>();
        config.put("service.guard", "(service.id=*)");

        BundleContext hookBC = mockConfigAdminBundleContext(config);
        GuardProxyCatalog gpc = new GuardProxyCatalog(hookBC);

        Filter serviceFilter = FrameworkUtil.createFilter("(service.id=*)"); // any service
        GuardingFindHook gfh = new GuardingFindHook(hookBC, gpc, serviceFilter);

        BundleContext clientBC = mockBundleContext(31L);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_ID, 16L);
        props.put(GuardProxyCatalog.PROXY_SERVICE_KEY, Boolean.TRUE);
        ServiceReference<?> sref = mockServiceReference(props);

        Collection<ServiceReference<?>> refs = new ArrayList<>();
        refs.add(sref);
        gfh.find(clientBC, null, null, false, refs);
        assertEquals("No proxy should have been created for the proxy find", 0, gpc.proxyMap.size());
        assertEquals("As the proxy is for this bundle is should be visible and remain on the list",
                Collections.singletonList(sref), refs);
    }

    @Test
    public void testNullFilter() throws Exception {
        BundleContext hookBC = mockBundleContext(5L);
        GuardProxyCatalog gpc = new GuardProxyCatalog(hookBC);

        GuardingFindHook gfh = new GuardingFindHook(hookBC, gpc, null);
        gfh.find(null, null, null, true, null); // should just do nothing
    }

    private BundleContext mockBundleContext(long id) throws Exception {
        Bundle bundle = EasyMock.createNiceMock(Bundle.class);
        EasyMock.expect(bundle.getBundleId()).andReturn(id).anyTimes();

        BundleContext bc = EasyMock.createNiceMock(BundleContext.class);
        EasyMock.expect(bc.getBundle()).andReturn(bundle).anyTimes();
        EasyMock.expect(bc.createFilter(EasyMock.isA(String.class))).andAnswer(
                () -> FrameworkUtil.createFilter((String) EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(bc);

        EasyMock.expect(bundle.getBundleContext()).andReturn(bc).anyTimes();
        EasyMock.replay(bundle);

        return bc;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private BundleContext mockConfigAdminBundleContext(Dictionary<String, Object> ... configs) throws IOException,
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

    private ServiceReference<Object> mockServiceReference(final Dictionary<String, Object> props) {
        @SuppressWarnings("unchecked")
        ServiceReference<Object> sr = EasyMock.createMock(ServiceReference.class);

        // Make sure the properties are 'live' in that if they change the reference changes too
        EasyMock.expect(sr.getPropertyKeys()).andAnswer(
                () -> Collections.list(props.keys()).toArray(new String[] {})).anyTimes();
        EasyMock.expect(sr.getProperty(EasyMock.isA(String.class))).andAnswer(
                () -> props.get(EasyMock.getCurrentArguments()[0])).anyTimes();
        EasyMock.replay(sr);
        return sr;
    }
}
