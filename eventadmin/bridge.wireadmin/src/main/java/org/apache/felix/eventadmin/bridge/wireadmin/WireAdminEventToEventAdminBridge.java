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
package org.apache.felix.eventadmin.bridge.wireadmin;

import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.wireadmin.WireAdmin;
import org.osgi.service.wireadmin.WireAdminEvent;
import org.osgi.service.wireadmin.WireAdminListener;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class WireAdminEventToEventAdminBridge implements WireAdminListener
{
    private final BundleContext m_context;

    public WireAdminEventToEventAdminBridge(final BundleContext context)
    {
        m_context = context;

        m_context.registerService(WireAdminListener.class.getName(), this, null);
    }

    public void wireAdminEvent(final WireAdminEvent event)
    {
        final ServiceReference ref = m_context
            .getServiceReference(EventAdmin.class.getName());

        if(null != ref)
        {
            final EventAdmin eventAdmin = (EventAdmin) m_context
                .getService(ref);

            if(null != eventAdmin)
            {
                final String topic;

                switch(event.getType())
                {
                    case WireAdminEvent.WIRE_CREATED:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/WIRE_CREATED";
                        break;
                    case WireAdminEvent.WIRE_CONNECTED:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/WIRE_CONNECTED";
                        break;
                    case WireAdminEvent.WIRE_UPDATED:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/WIRE_UPDATED";
                        break;
                    case WireAdminEvent.WIRE_TRACE:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/WIRE_TRACE";
                        break;
                    case WireAdminEvent.WIRE_DISCONNECTED:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/WIRE_DISCONNECTED";
                        break;
                    case WireAdminEvent.WIRE_DELETED:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/WIRE_DELETED";
                        break;
                    case WireAdminEvent.PRODUCER_EXCEPTION:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/PRODUCER_EXCEPTION";
                        break;
                    case WireAdminEvent.CONSUMER_EXCEPTION:
                        topic = "org/osgi/service/wireadmin/WireAdminEvent/CONSUMER_EXCEPTION";
                        break;
                    default:
                        m_context.ungetService(ref);
                        return;
                }

                final Hashtable properties = new Hashtable();
                
                properties.put(EventConstants.EVENT, event);

                properties.put("wire", event.getWire());

                properties.put("wire.flavors", event.getWire().getFlavors());

                properties.put("wire.scope", event.getWire().getScope());

                properties.put("wire.connected", (event.getWire().isConnected()) ? 
                    Boolean.TRUE : Boolean.FALSE);

                properties.put("wire.valid", (event.getWire().isValid()) ? 
                    Boolean.TRUE : Boolean.FALSE);

                final Throwable throwable = event.getThrowable();

                if(null != throwable)
                {
                    properties.put(EventConstants.EXCEPTION, throwable);

                    properties.put(EventConstants.EXCEPTION_CLASS, throwable
                        .getClass().getName());

                    final String message = throwable.getMessage();

                    if(null != message)
                    {
                        properties.put(EventConstants.EXCEPTION_MESSAGE, message);
                    }
                }

                final ServiceReference eventRef = event.getServiceReference();

                if(null == eventRef)
                {
                    throw new IllegalArgumentException(
                        "WireAdminEvent.getServiceReference() may not be null");
                }

                properties.put(EventConstants.SERVICE, eventRef);

                properties.put(EventConstants.SERVICE_ID, eventRef
                    .getProperty(EventConstants.SERVICE_ID));

                final Object objectClass = eventRef
                    .getProperty(Constants.OBJECTCLASS);

                if(!(objectClass instanceof String[])
                    || !Arrays.asList((String[]) objectClass)
                        .contains(WireAdmin.class.getName()))
                {
                    throw new IllegalArgumentException(
                        "Bad objectclass: " + objectClass);
                }

                properties.put(EventConstants.SERVICE_OBJECTCLASS, objectClass);

                properties.put(EventConstants.SERVICE_PID, eventRef
                    .getProperty(EventConstants.SERVICE_PID));
                
                eventAdmin.postEvent(new Event(topic, properties));

                m_context.ungetService(ref);
            }
        }
    }
}
