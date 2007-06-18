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
package org.apache.felix.eventadmin.bridge.useradmin;

import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class UserAdminEventToEventAdminBridge implements UserAdminListener
{
    private final BundleContext m_context;

    public UserAdminEventToEventAdminBridge(final BundleContext context)
    {
        m_context = context;

        m_context
            .registerService(UserAdminListener.class.getName(), this, null);
    }

    public void roleChanged(final UserAdminEvent event)
    {
        final ServiceReference ref = m_context
            .getServiceReference(EventAdmin.class.getName());

        if(null != ref)
        {
            final EventAdmin eventAdmin = (EventAdmin) m_context.getService(ref);

            if(null != eventAdmin)
            {
                final String topic;

                switch(event.getType())
                {
                    case UserAdminEvent.ROLE_CHANGED:
                        topic = "org/osgi/service/useradmin/UserAdmin/ROLE_CHANGED";
                        break;
                    case UserAdminEvent.ROLE_CREATED:
                        topic = "org/osgi/service/useradmin/UserAdmin/ROLE_CREATED";
                        break;
                    case UserAdminEvent.ROLE_REMOVED:
                        topic = "org/osgi/service/useradmin/UserAdmin/ROLE_REMOVED";
                        break;
                    default:
                        m_context.ungetService(ref);
                        return;
                }

                final Hashtable properties = new Hashtable();
                
                properties.put(EventConstants.EVENT, event);

                properties.put("role", event.getRole());

                properties.put("role.name", event.getRole().getName());

                properties.put("role.type", new Integer(event.getRole().getType()));

                final ServiceReference eventRef = event
                    .getServiceReference();

                if(null == eventRef)
                {
                    throw new IllegalArgumentException(
                        "UserAdminEvent.getServiceReference() may not be null");
                }

                properties.put(EventConstants.SERVICE, eventRef);

                properties.put(EventConstants.SERVICE_ID, eventRef
                    .getProperty(EventConstants.SERVICE_ID));

                final Object objectClass = eventRef
                    .getProperty(Constants.OBJECTCLASS);

                if(!(objectClass instanceof String[])
                    || !Arrays.asList((String[]) objectClass).contains(
                        UserAdmin.class.getName()))
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
