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
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class WireAdminEventToEventAdminBridge implements WireAdminListener
{
    private final BundleContext m_context;

    public WireAdminEventToEventAdminBridge(final BundleContext context)
    {
        m_context = context;

        m_context
            .registerService(WireAdminListener.class.getName(), this, null);
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

                eventAdmin.postEvent(new Event(topic, new Hashtable()
                {
                    {
                        put(EventConstants.EVENT, event);

                        put("wire", event.getWire());

                        put("wire.flavors", event.getWire().getFlavors());

                        put("wire.scope", event.getWire().getScope());

                        put("wire.connected", Boolean.valueOf(event.getWire()
                            .isConnected()));

                        put("wire.valid", Boolean.valueOf(event.getWire()
                            .isValid()));

                        final Throwable throwable = event.getThrowable();

                        if(null != throwable)
                        {
                            put(EventConstants.EXCEPTION, throwable);

                            put(EventConstants.EXCEPTION_CLASS, throwable
                                .getClass().getName());

                            final String message = throwable.getMessage();

                            if(null != message)
                            {
                                put(EventConstants.EXCEPTION_MESSAGE, message);
                            }

                            final ServiceReference ref = event
                                .getServiceReference();

                            if(null == ref)
                            {
                                throw new IllegalArgumentException(
                                    "WireAdminEvent.getServiceReference() may not be null");
                            }

                            put(EventConstants.SERVICE, ref);

                            put(EventConstants.SERVICE_ID, ref
                                .getProperty(EventConstants.SERVICE_ID));

                            final Object objectClass = ref
                                .getProperty(Constants.OBJECTCLASS);

                            if(!(objectClass instanceof String[])
                                || !Arrays.asList((String[]) objectClass)
                                    .contains(WireAdmin.class.getName()))
                            {
                                throw new IllegalArgumentException(
                                    "Bad objectclass: " + objectClass);
                            }

                            put(EventConstants.SERVICE_OBJECTCLASS, objectClass);

                            put(EventConstants.SERVICE_PID, ref
                                .getProperty(EventConstants.SERVICE_PID));
                        }
                    }
                }));

                m_context.ungetService(ref);
            }
        }
    }
}
