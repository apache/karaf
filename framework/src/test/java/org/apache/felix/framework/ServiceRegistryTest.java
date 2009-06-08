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
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
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
        assertSame(hook, sr.getEventHooks().iterator().next());
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {EventHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getEventHooks().size());
        Object [] arr = (Object[]) sr.getEventHooks().iterator().next();
        assertEquals(2, arr.length);
        assertSame(sf, arr[0]);
        assertTrue(arr[1] instanceof ServiceRegistration);
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
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
        assertSame(hook, sr.getFindHooks().iterator().next());
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {FindHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getFindHooks().size());
        Object [] arr = (Object[]) sr.getFindHooks().iterator().next();
        assertEquals(2, arr.length);
        assertSame(sf, arr[0]);
        assertTrue(arr[1] instanceof ServiceRegistration);
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
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
        assertSame(hook, sr.getListenerHooks().iterator().next());
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
        MockControl sfControl = MockControl.createNiceControl(ServiceFactory.class);
        sfControl.replay();
        ServiceFactory sf = (ServiceFactory) sfControl.getMock();
        
        assertEquals("Precondition failed", 0, sr.getEventHooks().size());
        assertEquals("Precondition failed", 0, sr.getFindHooks().size());
        assertEquals("Precondition failed", 0, sr.getListenerHooks().size());
        ServiceRegistration reg = sr.registerService(b, new String [] {ListenerHook.class.getName()}, sf, new Hashtable());
        assertEquals(1, sr.getListenerHooks().size());
        Object [] arr = (Object[]) sr.getListenerHooks().iterator().next();
        assertEquals(2, arr.length);
        assertSame(sf, arr[0]);
        assertTrue(arr[1] instanceof ServiceRegistration);
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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
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
        assertSame(hook, sr.getListenerHooks().iterator().next());
        assertEquals(1, sr.getEventHooks().size());
        assertSame(hook, sr.getEventHooks().iterator().next());
        assertEquals(1, sr.getFindHooks().size());
        assertSame(hook, sr.getFindHooks().iterator().next());

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
        
        ServiceRegistry sr = new ServiceRegistry(new Logger());
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
        assertSame(hook, ServiceRegistry.getHookRef(hook, null));
        
        assertEquals("Precondition failed", 0, result.size());
        ServiceRegistry.invokeHook(hook, fr, callback);
        assertEquals(1, result.size());
        assertSame(hook, result.iterator().next());
    }
    
    public void testInvokeHookFactory() 
    {
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

        MockControl control2 = MockControl.createNiceControl(ServiceRegistration.class);
        ServiceRegistration reg = (ServiceRegistration) control2.getMock();
        control2.replay();
        
        Object [] arg = new Object[2];
        arg[0] = sf;
        arg[1] = reg;
        Object [] arg2 = (Object[]) ServiceRegistry.getHookRef(sf, reg);
        assertTrue(Arrays.equals(arg, arg2));
    
        assertEquals("Precondition failed", 0, result.size());
        assertEquals("Precondition failed", 0, sfGet.size());
        assertEquals("Precondition failed", 0, sfUnget.size());
        ServiceRegistry.invokeHook(arg, fr, callback);
        assertEquals(1, result.size());
        assertSame(hook, result.iterator().next());
        assertEquals(1, sfGet.size());
        assertEquals(1, sfUnget.size());
        assertSame(reg, sfGet.iterator().next());        
        assertSame(reg, sfUnget.iterator().next());        
    }
}
