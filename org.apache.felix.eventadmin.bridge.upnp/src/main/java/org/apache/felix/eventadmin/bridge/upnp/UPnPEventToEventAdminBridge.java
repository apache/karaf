/*
 *   Copyright 2005 The Apache Software Foundation
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.upnp.UPnPEventListener;

/**
 * This class registers itself as an UPnPEventListener service with the 
 * framework and subsequently, bridges UPnPEvents received to available EventAdmin 
 * services. In order to track EventAdmin services this class registers itself as
 * a ServiceListener for EventAdmin services too.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class UPnPEventToEventAdminBridge implements ServiceListener,
    UPnPEventListener
{
    // The references to the EventAdmins additionally, used as a lock
    private final Set m_refs = new HashSet();
    
    private final BundleContext m_context;
    
    /**
     * Registers itself as an UPnPEventListener service with the given context and
     * in order to track EventAdmin services as a ServicListener too.
     * 
     * @param context The context to register with.
     */
    public UPnPEventToEventAdminBridge(final BundleContext context)
    {
        synchronized(m_refs)
        {
            m_context = context;
            
            try {
                m_context.addServiceListener(this, "(" + Constants.OBJECTCLASS + 
                    "=" + EventAdmin.class.getName() + ")");
                
                final ServiceReference[] refs = m_context.getServiceReferences(
                    EventAdmin.class.getName(), null);
                
                if(null != refs)
                {
                    for(int i = 0;i < refs.length;i++)
                    {
                        m_refs.add(refs[i]);
                    }
                }
            } catch(InvalidSyntaxException e) {
                 // This will never happen
            }
            
            m_context.registerService(UPnPEventListener.class.getName(), this, null);
        }
    }
    
    /**
     * Add newly registered service references.
     * 
     * @param event If event.getType equals REGISTERED the reference is added.
     * 
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(final ServiceEvent event)
    {
        synchronized (m_refs)
        {
            if(ServiceEvent.REGISTERED == event.getType())
            {
                m_refs.add(event.getServiceReference());
            }
        }
    }

    /**
     * Bridge any event to all available EventAdmin services.
     * 
     * @param deviceId Bridged to <tt>upnp.deviceId</tt>
     * @param serviceId Bridged to <tt>upnp.serviceId</tt>
     * @param events Bridged to <tt>upnp.events</tt>
     * 
     * @see org.osgi.service.upnp.UPnPEventListener#notifyUPnPEvent(java.lang.String, java.lang.String, java.util.Dictionary)
     */
    public void notifyUPnPEvent(final String deviceId, final String serviceId, 
        final Dictionary events)
    {
        synchronized (m_refs)
        {
            for (Iterator iter = m_refs.iterator(); iter.hasNext();)
            {
                final ServiceReference ref = (ServiceReference) iter.next();
                
                final EventAdmin eventAdmin = (EventAdmin) m_context.getService(ref);
                
                if(null != eventAdmin)
                {
                    final Dictionary immutableEvents = new Dictionary(){

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
                            throw new IllegalStateException("Event Properties may not be changed");
                        }

                        public Object remove(Object arg0)
                        {
                            throw new IllegalStateException("Event Properties may not be changed");
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
                    
                    eventAdmin.postEvent(new Event("org/osgi/service/upnp/UPnPEvent",
                        new Hashtable(){{
                            put("upnp.deviceId", deviceId);
                            put("upnp.serviceId", serviceId);
                            put("upnp.events", immutableEvents);
                        }}));
                    
                    m_context.ungetService(ref);
                }
                else
                {
                    iter.remove();
                }
            }
        }
    }
}
