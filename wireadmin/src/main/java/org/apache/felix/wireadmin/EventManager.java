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

package org.apache.felix.wireadmin;

import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.osgi.service.wireadmin.WireAdminEvent;
import org.osgi.service.wireadmin.WireAdminListener;
import org.osgi.service.wireadmin.WireConstants;

/**
 * Class that tracks listeners and manages event dispatching 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class EventManager implements ServiceListener
{
    // <ServiceReference,WireAdminListener>
    private Map m_wireAdminListeners = new HashMap();

    // The bundle context
    private BundleContext m_bundleContext;
    
    // Asynchronous dispatcher
    private AsyncDispatcher m_eventDispatcher = new AsyncDispatcher();
       
    // Service reference from the WireAdmin service
    private ServiceReference m_ref;
    
    /**
     * The constructor receives the bundle context and a service reference
     * corresponding to the wire admin service
     * 
     * @param ctxt
     * @param ref
     */
    EventManager(BundleContext ctxt)
    {
        m_bundleContext = ctxt;
        ServiceReference [] serviceRefs = null;
        
        try
        {
            m_bundleContext.addServiceListener(this,"(objectClass=org.osgi.service.wireadmin.WireAdminListener)");
            serviceRefs = m_bundleContext.getServiceReferences(WireAdminListener.class.getName(),null);
        }
        catch(Exception ex)
        {
            // Exception never thrown since filter is correct
        }        

        if(serviceRefs != null)
        {
            // lock the producers Map to avoid concurrent modifications due
            // to service events
            synchronized(m_wireAdminListeners)
            {
                for(int i=0;i<serviceRefs.length;i++)
                {
                    ServiceReference currentRef=(ServiceReference)serviceRefs[i];
                    
                    m_wireAdminListeners.put(currentRef,m_bundleContext.getService(currentRef));
                }
            }
        }        
    }
    
    /**
     * When the service reference is set the event dispatching thread is
     * started. This is necessary since events require m_ref to be set
     * 
     * @param ref <tt>ServiceReference</tt> to the wire admin service.
     */
    void setServiceReference(ServiceReference ref)
    {
        m_ref = ref;

        // Activate thread that does asynchronous calls to
        // the producersConnected and consummersConnected methods
        new Thread(m_eventDispatcher).start();
    }
    
    /**
     * Stop the event manager
     *
     */
    void stop()
    {
        m_eventDispatcher.stop();
    }
    
    /**
     * Tracks Listener changes
     */
    public void serviceChanged(ServiceEvent e) 
    {
        ServiceReference serviceRef = e.getServiceReference();
        switch (e.getType()) 
        {
            case ServiceEvent.REGISTERED:
                synchronized(m_wireAdminListeners)
                {
                    m_wireAdminListeners.put(serviceRef,m_bundleContext.getService(serviceRef));
                }
                break;
            case ServiceEvent.UNREGISTERING:
            synchronized(m_wireAdminListeners)
                {
                    m_wireAdminListeners.remove(serviceRef);
                }
                break;
            case ServiceEvent.MODIFIED:
                // Not necessary
                break;
        }
    }
    
    /**
     * Fire an event
     * 
     * @param eventType (see WireAdminEvent)
     * @param wire the wire
     */
    void fireEvent(int eventType,WireImpl wire) 
    {
        fireEvent(eventType, wire, null);
    }

    /**
     * Fire an event that contains an exception
     * 
     * @param eventType
     * @param wire
     * @param exception
     */
    void fireEvent(int eventType,WireImpl wire, Throwable exception) 
    {
        WireAdminEvent evt = new WireAdminEvent(m_ref,eventType,wire,exception);
        m_eventDispatcher.queueEvent(evt);
    }

    /**
     * p. 345 "A WireAdminEvent object can be sent asynchronously but must be
     * ordered for each WireAdminListener service"
     * But the API doc says "WireAdminEvent objects are delivered asynchronously 
     * to all registered WireAdminListener service objects which specify an 
     * interest in the WireAdminEvent type."
     * We choose asynchronous delivery to be safe
     *
    **/
    class AsyncDispatcher implements Runnable
    {
        private boolean m_stop = false;
        private boolean m_empty = true;

        private List m_eventStack = new ArrayList();

        public void run()
        {
            while (m_stop == false || (m_stop == true && m_empty == false))
            {
                WireAdminEvent nextEvent = null;

                synchronized (m_eventStack)
                {
                    while (m_eventStack.size() == 0)
                    {
                        try
                        {
                            m_eventStack.wait();
                        } 
                        catch (InterruptedException ex)
                        {
                            // Ignore.
                        }
                    }
                    nextEvent = (WireAdminEvent) m_eventStack.remove(0);
                    
                    if(m_eventStack.size()==0)
                    {
                        // This allows the queue to be flushed upon termination
                        m_empty = true;
                    }
                }
                
                synchronized (m_wireAdminListeners)
                {
                    Iterator listenerIt = m_wireAdminListeners.keySet().iterator();
                    while(listenerIt.hasNext())
                    {
                        ServiceReference listenerRef = (ServiceReference)listenerIt.next();
                        WireAdminListener listener = (WireAdminListener)m_wireAdminListeners.get(listenerRef);
                        
                        try
                        {
                            Integer evtsInteger = (Integer) listenerRef.getProperty(WireConstants.WIREADMIN_EVENTS);
                            if(evtsInteger != null)
                            {
                                int events = evtsInteger.intValue();
                                if((nextEvent.getType()&events)!=0)
                                {
                                    listener.wireAdminEvent(nextEvent);
                                }                            
                            }
                            else
                            {
                                WireAdminImpl.trace(new Exception("Listener with no WIREADMIN_EVENTS"+listenerRef));
                            }
                        }
                        catch(ClassCastException ex)
                        {
                            WireAdminImpl.trace("Listener returned WIREADMIN_EVENTS of wrong type:"+ex);
                        }
                    }
                }
            }
        }

        /**
         * Queue an event on the stack
         * 
         * @param evt the event
         */
        public void queueEvent(WireAdminEvent evt)
        {
            synchronized (m_eventStack)
            {
                m_eventStack.add(evt);
                m_empty = false;
                m_eventStack.notify();
            }
        }

        /**
         * stop the dispatcher
         *
         */
        void stop()
        {
            m_stop = true;
        }
    }
}
