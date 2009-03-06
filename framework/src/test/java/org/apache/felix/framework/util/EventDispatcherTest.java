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
package org.apache.felix.framework.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.ServiceRegistry;
import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventHook;

public class EventDispatcherTest extends TestCase
{
    public void testFireServiceEvent()
    {
        final Bundle b1 = getMockBundle();
        final Bundle b2 = getMockBundle();
        final Bundle b3 = getMockBundle();

        final Set calledHooks = new HashSet();
        final EventHook ph1 = new EventHook()
        {
            public void event(ServiceEvent event, Collection contexts)
            {
                calledHooks.add(this);
            }
        };
        final EventHook ph2 = new EventHook()
        {
            public void event(ServiceEvent event, Collection contexts)
            {
                calledHooks.add(this);
                for (Iterator it = contexts.iterator(); it.hasNext();)
                {
                    BundleContext bc = (BundleContext) it.next();
                    if (bc.getBundle() == b1)
                    {
                        it.remove();
                    }
                    if (bc.getBundle() == b2)
                    {
                        it.remove();
                    }
                }
            }
        };

        Logger logger = new Logger();
        ServiceRegistry registry = new ServiceRegistry(logger)
        {
            public List getEventHooks()
            {
                return Collections.unmodifiableList(
                    Arrays.asList(new EventHook[]
                        {
                            ph1, ph2
                        }));
            }
        };

        // -- Set up event dispatcher

        EventDispatcher ed = EventDispatcher.start(logger);
        EventDispatcher.shutdown(); // stop the thread - would be nicer if we could create one without a thread for testing
        ed.setServiceRegistry(registry);

        // -- Register some listeners
        final List fired = Collections.synchronizedList(new ArrayList());
        ServiceListener sl1 = new ServiceListener()
        {
            public void serviceChanged(ServiceEvent arg0)
            {
                fired.add(this);
                System.out.println("*** sl1");
            }
        };
        ed.addListener(b1, ServiceListener.class, sl1, null);

        ServiceListener sl2 = new ServiceListener()
        {
            public void serviceChanged(ServiceEvent arg0)
            {
                fired.add(this);
                System.out.println("*** sl2");
            }
        };
        ed.addListener(b2, ServiceListener.class, sl2, null);

        ServiceListener sl3 = new ServiceListener()
        {
            public void serviceChanged(ServiceEvent arg0)
            {
                fired.add(this);
                System.out.println("*** sl3");
            }
        };
        ed.addListener(b3, ServiceListener.class, sl3, null);

        // --- make the invocation
        MockControl control = MockControl.createNiceControl(ServiceReference.class);
        ServiceReference sr = (ServiceReference) control.getMock();
        sr.getProperty(Constants.OBJECTCLASS);
        control.setReturnValue(new String[]
            {
                "java.lang.String"
            }, MockControl.ZERO_OR_MORE);
        sr.isAssignableTo(b1, String.class.getName());
        control.setReturnValue(true, MockControl.ZERO_OR_MORE);
        sr.isAssignableTo(b2, String.class.getName());
        control.setReturnValue(true, MockControl.ZERO_OR_MORE);
        sr.isAssignableTo(b3, String.class.getName());
        control.setReturnValue(true, MockControl.ZERO_OR_MORE);
        control.replay();

        ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, sr);

        assertEquals("Precondition failed", 0, fired.size());
        ed.fireServiceEvent(event);
        assertEquals(1, fired.size());
        assertSame(sl3, fired.iterator().next());

        assertEquals(2, calledHooks.size());
        assertTrue(calledHooks.contains(ph1));
        assertTrue(calledHooks.contains(ph2));
    }

    private Bundle getMockBundle()
    {
        MockControl bcControl = MockControl.createNiceControl(BundleContext.class);
        BundleContext bc = (BundleContext) bcControl.getMock();

        MockControl bControl = MockControl.createNiceControl(Bundle.class);
        Bundle b = (Bundle) bControl.getMock();
        b.getBundleContext();
        bControl.setReturnValue(bc, MockControl.ZERO_OR_MORE);
        b.getState();
        bControl.setReturnValue(Bundle.ACTIVE, MockControl.ZERO_OR_MORE);

        bc.getBundle();
        bcControl.setReturnValue(b, MockControl.ZERO_OR_MORE);

        bcControl.replay();
        bControl.replay();

        return b;
    }
}