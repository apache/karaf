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
package org.apache.felix.eventadmin.impl.adapter;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.eventadmin.impl.util.LogWrapper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * This class registers itself as a listener for <tt>LogReaderService</tt> services
 * with the framework and subsequently, a <tt>LogListener</tt> callback with any
 * currently available <tt>LogReaderService</tt>. Any received log event is then 
 * posted via the EventAdmin as specified in 113.6.6 OSGi R4 compendium.
 * Note that this class does not create a hard dependency on the org.osgi.service.log
 * packages. The adaption only takes place if it is present or once it becomes 
 * available hence, combined with a DynamicImport-Package no hard dependency is 
 * needed. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LogEventAdapter implements ServiceListener
{
    // The internal lock for this object used instead synchronized(this)
    private final Object m_lock = new Object();
    
    private BundleContext m_context;
    
    // A singleton instance of the used log listener that is the adapter
    private Object m_logListener;
    
    final EventAdmin m_admin;
    
    /**
     * The constructor of the adapter. This will register the adapter with the
     * given context as a listener for <tt>LogReaderService</tt> services and 
     * subsequently, a <tt>LogListener</tt> callback with any currently available
     * <tt>LogReaderService</tt>. Any received log event is then posted via the given 
     * EventAdmin.
     * 
     * @param context The bundle context with which to register as a listener.
     * @param admin The <tt>EventAdmin</tt> to use for posting events.
     */
    public LogEventAdapter(final BundleContext context, final EventAdmin admin)
    {
        if(null == admin)
        {
            throw new NullPointerException("EventAdmin must not be null");
        }
        
        m_context = context;
        
        m_admin = admin;

        try
        {
            m_context.addServiceListener(this, "(" + Constants.OBJECTCLASS
                + "=org.osgi.service.log.LogReaderService)");

            final ServiceReference[] refs;

            refs = m_context.getServiceReferences(
                "org.osgi.service.log.LogReaderService", null);

            if (null != refs)
            {
                for (int i = 0; i < refs.length; i++)
                {
                    final org.osgi.service.log.LogReaderService logReader = 
                        (org.osgi.service.log.LogReaderService) m_context
                        .getService(refs[i]);

                    if (null != logReader)
                    {
                        logReader.addLogListener((org.osgi.service.log.LogListener) 
                            getLogListener());
                    }
                }
            }
        } catch (InvalidSyntaxException e)
        {
            // This never happens
        }
    }

    /**
     * Once a <tt>LogReaderService</tt> register event is received this method
     * registers a <tt>LogListener</tt> with the received service that assembles 
     * and posts any log event via the <tt>EventAdmin</tt> as specified in 
     * 113.6.6 OSGi R4 compendium. 
     * 
     * @param event The event to adapt.
     */
    public void serviceChanged(final ServiceEvent event)
    {
        if (ServiceEvent.REGISTERED == event.getType())
        {
            final org.osgi.service.log.LogReaderService logReader = 
                (org.osgi.service.log.LogReaderService) m_context
                .getService(event.getServiceReference());

            if (null != logReader)
            {
                logReader.addLogListener((org.osgi.service.log.LogListener) 
                    getLogListener());
            }
        }
    }

    /*
     * Constructs a LogListener that assembles and posts any log event via the 
     * EventAdmin as specified in 113.6.6 OSGi R4 compendium. Note that great
     * care is taken to not create a hard dependency on the org.osgi.service.log
     * package.
     */
    private Object getLogListener()
    {
        synchronized (m_lock)
        {
            if (null != m_logListener)
            {
                return m_logListener;
            }

            m_logListener = new org.osgi.service.log.LogListener()
            {
                public void logged(final org.osgi.service.log.LogEntry entry)
                {
                    // This is where the assembly as specified in 133.6.6 OSGi R4
                    // compendium is taking place (i.e., the log entry is adapted to 
                    // an event and posted via the EventAdmin)
                    
                    final Dictionary properties = new Hashtable();

                    final Bundle bundle = entry.getBundle();

                    if (null != bundle)
                    {
                        properties.put("bundle.id", new Long(bundle
                            .getBundleId()));

                        final String symbolicName = bundle.getSymbolicName();

                        if (null != symbolicName)
                        {
                            properties.put(EventConstants.BUNDLE_SYMBOLICNAME,
                                symbolicName);
                        }

                        properties.put("bundle", bundle);
                    }

                    properties.put("log.level", new Integer(entry.getLevel()));

                    properties.put(EventConstants.MESSAGE, entry.getMessage());

                    properties.put(EventConstants.TIMESTAMP, new Long(
                        entry.getTime()));

                    properties.put("log.entry", entry);

                    final Throwable exception = entry.getException();

                    if (null != exception)
                    {
                        properties.put(EventConstants.EXCEPTION_CLASS,
                            exception.getClass().getName());

                        final String message = exception.getMessage();

                        if (null != message)
                        {
                            properties.put(EventConstants.EXCEPTION_MESSAGE,
                                message);
                        }

                        properties.put(EventConstants.EXCEPTION, exception);
                    }

                    final ServiceReference service = entry
                        .getServiceReference();

                    if (null != service)
                    {
                        properties.put(EventConstants.SERVICE, service);

                        final Object id = service
                            .getProperty(EventConstants.SERVICE_ID);

                        if (null != id)
                        {
                            try
                            {
                                properties.put(EventConstants.SERVICE_ID,
                                    new Long(id.toString()));
                            } catch (NumberFormatException ne)
                            {
                                // LOG and IGNORE
                                LogWrapper.getLogger().log(
                                    entry.getServiceReference(),
                                    LogWrapper.LOG_WARNING, "Exception parsing " + 
                                    EventConstants.SERVICE_ID + "=" + id, ne);
                            }
                        }

                        final Object pid = service.getProperty(
                            EventConstants.SERVICE_PID);

                        if (null != pid)
                        {
                            properties.put(EventConstants.SERVICE_PID, 
                                pid.toString());
                        }

                        final Object objectClass = service.getProperty(
                            Constants.OBJECTCLASS);

                        if (null != objectClass)
                        {
                            if (objectClass instanceof String[])
                            {
                                properties.put(
                                    EventConstants.SERVICE_OBJECTCLASS,
                                    objectClass);
                            }
                            else
                            {
                                properties.put(
                                    EventConstants.SERVICE_OBJECTCLASS,
                                    new String[] { objectClass.toString() });
                            }
                        }
                    }

                    final StringBuffer topic = new StringBuffer(
                        org.osgi.service.log.LogEntry.class.getName().replace(
                            '.', '/')).append('/');

                    switch (entry.getLevel())
                    {
                        case org.osgi.service.log.LogService.LOG_ERROR:
                            topic.append("LOG_ERROR");
                            break;
                        case org.osgi.service.log.LogService.LOG_WARNING:
                            topic.append("LOG_WARNING");
                            break;
                        case org.osgi.service.log.LogService.LOG_INFO:
                            topic.append("LOG_INFO");
                            break;
                        case org.osgi.service.log.LogService.LOG_DEBUG:
                            topic.append("LOG_DEBUG");
                            break;
                        default:
                            topic.append("LOG_OTHER");
                            break;
                    }

                    try {
                        m_admin.postEvent(new Event(topic.toString(), properties));
                    } catch(IllegalStateException e) {
                        // This is o.k. - indicates that we are stopped.
                    }
                }
            };
            
            return m_logListener;
        }
    }
}
