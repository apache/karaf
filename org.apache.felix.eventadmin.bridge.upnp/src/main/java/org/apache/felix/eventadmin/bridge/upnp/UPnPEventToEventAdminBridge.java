/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.eventadmin.bridge.upnp;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.upnp.UPnPEventListener;

/**
 * This class registers itself as an UPnPEventListener service with the
 * framework whenever both, at least one EventAdmin and at least one
 * EventHandler is present and subsequently, bridges UPnPEvents received to the
 * EventAdmin service. In order to track EventAdmin services this class
 * registers a ServiceListener for EventAdmin services as well as a
 * ServiceListener for EventHandlers in order to determine EventHandler
 * availability.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class UPnPEventToEventAdminBridge implements UPnPEventListener
{
    private static final String EVENT_HANDLER_FILTER = "(&("
        + Constants.OBJECTCLASS + "=" + EventHandler.class.getName() + ")(|("
        + EventConstants.EVENT_TOPIC + "=\\*)(" + EventConstants.EVENT_TOPIC
        + "=org/\\*)(" + EventConstants.EVENT_TOPIC + "=org/osgi/\\*)("
        + EventConstants.EVENT_TOPIC + "=org/osgi/service/\\*)("
        + EventConstants.EVENT_TOPIC + "=org/osgi/service/upnp/\\*)("
        + EventConstants.EVENT_TOPIC + "=org/osgi/service/upnp/UPnPEvent)))";

    private final Object m_lock = new Object();

    // The references to the EventAdmins
    private final Set m_adminRefs = new HashSet();

    // The references to the EventHandlers
    private final Set m_handlerRefs = new HashSet();

    private final BundleContext m_context;

    private ServiceRegistration m_reg = null;

    /**
     * This class registers itself as an UPnPEventListener service with the
     * framework whenever both, at least one EventAdmin and at least one
     * EventHandler is present and subsequently, bridges UPnPEvents received to
     * the EventAdmin service. In order to track EventAdmin services this class
     * registers a ServiceListener for EventAdmin services as well as a
     * ServiceListener for EventHandlers in order to determine EventHandler
     * availability.
     * 
     * @param context
     *            The context to register with.
     */
    public UPnPEventToEventAdminBridge(final BundleContext context)
    {
        synchronized(m_lock)
        {
            m_context = context;

            try
            {
                m_context.addServiceListener(new ServiceListener()
                {

                    public void serviceChanged(final ServiceEvent event)
                    {
                        synchronized(m_lock)
                        {
                            switch(event.getType())
                            {
                                case ServiceEvent.REGISTERED:
                                    m_adminRefs
                                        .add(event.getServiceReference());
                                    break;
                                case ServiceEvent.UNREGISTERING:
                                    m_adminRefs.remove(event
                                        .getServiceReference());
                                    break;
                            }

                            check();
                        }
                    }
                }, "(" + Constants.OBJECTCLASS + "="
                    + EventAdmin.class.getName() + ")");

                final ServiceReference[] adminRefs = m_context
                    .getServiceReferences(EventAdmin.class.getName(), null);

                if(null != adminRefs)
                {
                    for(int i = 0; i < adminRefs.length; i++)
                    {
                        m_adminRefs.add(adminRefs[i]);
                    }
                }

                m_context.addServiceListener(new ServiceListener()
                {
                    public void serviceChanged(final ServiceEvent event)
                    {
                        synchronized(m_lock)
                        {
                            switch(event.getType())
                            {
                                case ServiceEvent.REGISTERED:
                                    m_handlerRefs.add(event
                                        .getServiceReference());
                                    break;
                                case ServiceEvent.UNREGISTERING:
                                    m_handlerRefs.remove(event
                                        .getServiceReference());
                                    break;
                            }

                            check();
                        }
                    }
                }, EVENT_HANDLER_FILTER);

                final ServiceReference[] handlerRefs = m_context
                    .getServiceReferences(EventHandler.class.getName(),
                        EVENT_HANDLER_FILTER);

                if(null != handlerRefs)
                {
                    for(int i = 0; i < handlerRefs.length; i++)
                    {
                        m_handlerRefs.add(handlerRefs[i]);
                    }
                }
            } catch(InvalidSyntaxException e)
            {
                // This will never happen
            }

            check();
        }
    }

    // Registers itself as an UPnPEventListener with the framework in case there
    // is both, at least one EventAdmin (i.e., !m_adminRefs.isEmpty()) and at
    // least one EventHandler (i.e., !m_handlerRefs.isEmpty()) present and it is not
    // already registers. Respectively, it unregisters itself in case one of the 
    // above is false.
    private void check()
    {
        if(m_adminRefs.isEmpty() || m_handlerRefs.isEmpty())
        {
            if(null != m_reg)
            {
                m_reg.unregister();
                m_reg = null;
            }
        }
        else
        {
            if(null == m_reg)
            {
                m_reg = m_context.registerService(UPnPEventListener.class
                    .getName(), this, null);
            }
        }
    }

    /**
     * Bridge any event to the EventAdmin service.
     * 
     * @param deviceId
     *            Bridged to <tt>upnp.deviceId</tt>
     * @param serviceId
     *            Bridged to <tt>upnp.serviceId</tt>
     * @param events
     *            Bridged to <tt>upnp.events</tt>
     * 
     * @see org.osgi.service.upnp.UPnPEventListener#notifyUPnPEvent(java.lang.String,
     *      java.lang.String, java.util.Dictionary)
     */
    public void notifyUPnPEvent(final String deviceId, final String serviceId,
        final Dictionary events)
    {
        final ServiceReference ref = m_context
            .getServiceReference(EventAdmin.class.getName());

        if(null != ref)
        {
            final EventAdmin eventAdmin = (EventAdmin) m_context
                .getService(ref);

            if(null != eventAdmin)
            {
                final Dictionary immutableEvents = new Dictionary()
                {
                    public int size()
                    {
                        return events.size();
                    }

                    public boolean isEmpty()
                    {
                        return events.isEmpty();
                    }

                    public Enumeration keys()
                    {
                        return events.keys();
                    }

                    public Enumeration elements()
                    {
                        return events.elements();
                    }

                    public Object get(Object arg0)
                    {
                        return events.get(arg0);
                    }

                    public Object put(Object arg0, Object arg1)
                    {
                        throw new IllegalStateException(
                            "Event Properties may not be changed");
                    }

                    public Object remove(Object arg0)
                    {
                        throw new IllegalStateException(
                            "Event Properties may not be changed");
                    }

                    public boolean equals(Object arg0)
                    {
                        return events.equals(arg0);
                    }

                    public int hashCode()
                    {
                        return events.hashCode();
                    }

                    public String toString()
                    {
                        return events.toString();
                    }
                };

                eventAdmin.postEvent(new Event(
                    "org/osgi/service/upnp/UPnPEvent", new Hashtable()
                    {
                        {
                            put("upnp.deviceId", deviceId);
                            put("upnp.serviceId", serviceId);
                            put("upnp.events", immutableEvents);
                        }
                    }));

                m_context.ungetService(ref);
            }
        }
    }
}
