/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.framework.launch.Framework;

public class ServiceRegistryTest extends TestCase 
{
    public void testRegisterEventHookService() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        EventHook hook = new EventHook() 
        {
            public void event(ServiceEvent event, Collection contexts) 
            {
            }            
        };
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {EventHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getEventHooks().size());
        assertTrue(sr.getEventHooks().iterator().next() instanceof ServiceReference);
        assertSame(reg.getReference(), sr.getEventHooks().iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getFindHooks().size());
        assertEquals("Postcondition failed", 0, sr.getListenerHooks().size());
        
        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }

    public void testRegisterEventHookServiceFactory() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {EventHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getEventHooks().size());
        assertSame(reg.getReference(), sr.getEventHooks().iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());        
        assertEquals("Postcondition failed", 0, sr.getFindHooks().size());
        assertEquals("Postcondition failed", 0, sr.getListenerHooks().size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }
    
    public void testRegisterFindHookService() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        FindHook hook = new FindHook() 
        {
            public void find(BundleContext context, String name, String filter,
                boolean allServices, Collection references) 
            {
            }
        };
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {FindHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getFindHooks().size());
        assertSame(reg.getReference(), sr.getFindHooks().iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getEventHooks().size());
        assertEquals("Postcondition failed", 0, sr.getListenerHooks().size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }

    public void testRegisterFindHookServiceFactory() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {FindHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getFindHooks().size());
        assertSame(reg.getReference(), sr.getFindHooks().iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());        
        assertEquals("Postcondition failed", 0, sr.getEventHooks().size());
        assertEquals("Postcondition failed", 0, sr.getListenerHooks().size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }

    public void testRegisterListenerHookService() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        ListenerHook hook = new ListenerHook() 
        {
            public void added(Collection listeners) 
            {
            }

            public void removed(Collection listener) 
            {
            }
        };
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {ListenerHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getListenerHooks().size());
        assertSame(reg.getReference(), sr.getListenerHooks().iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals("Postcondition failed", 0, sr.getEventHooks().size());
        assertEquals("Postcondition failed", 0, sr.getFindHooks().size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }

    public void testRegisterListenerHookServiceFactory() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {ListenerHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getListenerHooks().size());
        assertSame(reg.getReference(), sr.getListenerHooks().iterator().next());
        assertSame(sf, ((ServiceRegistrationImpl) reg).getService());        
        assertEquals("Postcondition failed", 0, sr.getEventHooks().size());
        assertEquals("Postcondition failed", 0, sr.getFindHooks().size());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }

    public void testRegisterCombinedService() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        class CombinedService implements ListenerHook, FindHook, EventHook, Runnable
        {
            public void added(Collection listeners) 
            {
            }

            public void removed(Collection listener) 
            {
            }

            public void find(BundleContext context, String name, String filter,
                    boolean allServices, Collection references) 
            {
            }

            public void event(ServiceEvent event, Collection contexts) 
            {
            }

            public void run() 
            {
            }
            
        }
        CombinedService hook = new CombinedService();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {
            Runnable.class.getName(),
            ListenerHook.class.getName(),
            FindHook.class.getName(),
            EventHook.class.getName()}, hook, new Hashtable());
        assertEquals(1, sr.getListenerHooks().size());
        assertSame(reg.getReference(), sr.getListenerHooks().iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals(1, sr.getEventHooks().size());
        assertSame(reg.getReference(), sr.getEventHooks().iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());
        assertEquals(1, sr.getFindHooks().size());
        assertSame(reg.getReference(), sr.getFindHooks().iterator().next());
        assertSame(hook, ((ServiceRegistrationImpl) reg).getService());

        sr.unregisterService(b, reg);
        assertEquals("Should be no hooks left after unregistration", 0, sr.getEventHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getFindHooks().size());
        assertEquals("Should be no hooks left after unregistration", 0, sr.getListenerHooks().size());
    }

    public void testRegisterPlainService() 
    {
        MockControl control = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) control.getMock();
        control.replay();
        
        ServiceRegistry sr = new ServiceRegistry(new Logger(), null);
        String svcObj = "hello";        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {String.class.getName()}, svcObj, new Hashtable());
        assertEquals("Postcondition failed", 0, sr.getEventHooks().size());
        assertEquals("Postcondition failed", 0, sr.getFindHooks().size());
        assertEquals("Postcondition failed", 0, sr.getListenerHooks().size());

        sr.unregisterService(b, reg);
        assertEquals("Unregistration should have no effect", 0, sr.getEventHooks().size());
        assertEquals("Unregistration should have no effect", 0, sr.getFindHooks().size());
        assertEquals("Unregistration should have no effect", 0, sr.getListenerHooks().size());
    }
    
    public void testInvokeHook() 
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);
        
        final List result = new ArrayList();
        InvokeHookCallback callback = new InvokeHookCallback() 
        {
            public void invokeHook(Object hook) 
            {
                result.add(hook);
            }            
        };
        
        MockControl control = MockControl.createNiceControl(Framework.class);
        Framework fr = (Framework) control.getMock();
        control.replay();
        
        FindHook hook = new FindHook() 
        {
            public void find(BundleContext context, String name, String filter,
                    boolean allServices, Collection references) 
            {
            }            
        };
        ServiceRegistration reg = new ServiceRegistrationImpl(sr, null, null, null, 
            hook, new Hashtable());
        
        assertEquals("Precondition failed", 0, result.size());
        sr.invokeHook(reg.getReference(), fr, callback);
        assertEquals(1, result.size());
        assertSame(hook, result.iterator().next());
    }
    
    public void testInvokeHookFactory() 
    {
        ServiceRegistry sr = new ServiceRegistry(null, null);

        final List result = new ArrayList();
        InvokeHookCallback callback = new InvokeHookCallback() 
        {
            public void invokeHook(Object hook) 
            {
                result.add(hook);
            }            
        };

        MockControl control = MockControl.createNiceControl(Framework.class);
        Framework fr = (Framework) control.getMock();
        control.replay();
        
        final FindHook hook = new FindHook() 
        {
            public void find(BundleContext context, String name, String filter,
                    boolean allServices, Collection references) 
            {
            }            
        };
        
        final List sfGet = new ArrayList();
        final List sfUnget = new ArrayList();
        ServiceFactory sf = new ServiceFactory() 
        {
            public Object getService(Bundle b, ServiceRegistration reg) 
            {
                sfGet.add(reg);
                return hook;
            }

            public void ungetService(Bundle b, ServiceRegistration reg, Object svcObj) 
            {
                sfUnget.add(reg);
                assertSame(svcObj, hook);
            }            
        };

        ServiceRegistration reg = new ServiceRegistrationImpl(sr, null, 
            new String[] {FindHook.class.getName()}, null, 
            sf, new Hashtable());       
    
        assertEquals("Precondition failed", 0, result.size());
        assertEquals("Precondition failed", 0, sfGet.size());
        assertEquals("Precondition failed", 0, sfUnget.size());
        sr.invokeHook(reg.getReference(), fr, callback);
        assertEquals(1, result.size());
        assertSame(hook, result.iterator().next());
        assertEquals(1, sfGet.size());
        assertEquals(1, sfUnget.size());
        assertSame(reg, sfGet.iterator().next());        
        assertSame(reg, sfUnget.iterator().next());        
    } 
}
