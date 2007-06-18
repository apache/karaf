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
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;

/**
 * This class registers itself as an UPnPEventListener service with the
 * framework whenever both, at least one EventAdmin and at least one
 * EventHandler is present and subsequently, bridges UPnPEvents received to the
 * EventAdmin service. In order to track EventAdmin services this class
 * registers a ServiceListener for EventAdmin services as well as a
 * ServiceListener for EventHandlers in order to determine EventHandler
 * availability.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
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

    final Object m_lock = new Object();

    // The references to the EventAdmins
    final Set m_adminRefs = new HashSet();

    // The references to the EventHandlers
    final Set m_handlerRefs = new HashSet();

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

    // The set contains the last used filter parts. It will be null in case the
    // last
    // time we registered with a null property. It will be an empty HashSet in
    // case we have been unregistered previously
    private Set last = new HashSet();

    // Registers itself as an UPnPEventListener with the framework in case there
    // is both, at least one EventAdmin (i.e., !m_adminRefs.isEmpty()) and at
    // least one EventHandler (i.e., !m_handlerRefs.isEmpty()) present and it is
    // not already registers. Respectively, it unregisters itself in case one of
    // the above is false.
    void check()
    {
        // do we need to be registered?
        if(m_adminRefs.isEmpty() || m_handlerRefs.isEmpty())
        {
            // no we don't but do we need to unregister?
            if(null != m_reg)
            {
                // yes
                m_reg.unregister();
                m_reg = null;
                last = new HashSet();
            }
        }
        else
        // yes we need to be registered
        {
            final Set parts = new HashSet();
            final StringBuffer result = new StringBuffer().append("(|");

            for(Iterator iter = m_handlerRefs.iterator(); iter.hasNext();)
            {
                final String filter = (String) ((ServiceReference) iter.next())
                    .getProperty(EventConstants.EVENT_FILTER);

                // if any filter is not set we need to register with a null and
                // can
                // return
                if(null == filter)
                {
                    // but only if we are not currently registered with a null
                    if(last != null)
                    {
                        last = null;
                        change(null);
                    }
                    return;
                }

                // if we don't already have this filter part we need to check if
                // it is a valid filter
                if(!parts.contains(filter))
                {
                    try
                    {
                        m_context.createFilter(filter);
                        parts.add(filter);
                        result.append(filter);
                    } catch(InvalidSyntaxException e)
                    {
                        // and it is not a valid filter - hence, drop it
                        e.printStackTrace();
                    }
                }
            }

            // parts will only be empty if there is no handler with a valid
            // filter and we only need to register with the new filter if it 
            // doesn't equals the last filter
            if(!parts.isEmpty() && !parts.equals(last))
            {
                last = parts;

                try
                {
                    final Hashtable properties = new Hashtable();
                    
                    properties.put(UPnPEventListener.UPNP_FILTER, 
                        m_context.createFilter(replaceAll(replaceAll(
                        result.append(")").toString().toCharArray(),
                        serviceChars, UPnPService.ID).toCharArray(), 
                        deviceChars, UPnPDevice.ID)));
                    
                    change(properties);
                } catch(InvalidSyntaxException e)
                {
                    // This will never happen
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static final char[] serviceChars = new char[]{'u','p','n','p','.','s','e','r','v','i','c','e','i','d'};
    private static final char[] deviceChars = new char[]{'u','p','n','p','.','d','e','v','i','c','e','i','d'};
    
    private String replaceAll(final char[] source, final char[] pattern, final String target)
    {
        StringBuffer result = new StringBuffer();
        
        int pos = 0, matchPos = 0;
        
        while(true)
        {   
            if(pattern[matchPos] == Character.toLowerCase(source[pos]))
            {
                matchPos++;
                if(matchPos == pattern.length)
                {
                    result.append(target);
                    matchPos = 0;
                }
            }
            else if(matchPos > 0 )
            {
                result.append(source, pos - matchPos, matchPos + 1);
                matchPos = 0;
            }
            else
            {
                result.append(source[pos]);
            }
            
            pos++;
            
            if(pos >= source.length)
            {
                if(matchPos > 0)
                {
                    result.append(source, pos - matchPos, matchPos);
                }
                
                break;
            }
        }
        
        return result.toString();
    }

    private void change(final Dictionary filter)
    {
        if(null == m_reg)
        {
            m_reg = m_context.registerService(
                UPnPEventListener.class.getName(), this, filter);
        }
        else
        {
            m_reg.setProperties(filter);
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

                final Hashtable properties = new Hashtable();
                
                properties.put(UPnPDevice.ID, deviceId);
                properties.put(UPnPService.ID, serviceId);
                properties.put("upnp.serviceId", serviceId);
                properties.put("upnp.deviceId", deviceId);
                properties.put("upnp.events", immutableEvents);
                
                eventAdmin.postEvent(
                    new Event("org/osgi/service/upnp/UPnPEvent", properties));

                m_context.ungetService(ref);
            }
        }
    }
}
