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
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
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
            final EventAdmin eventAdmin = (EventAdmin) m_context
                .getService(ref);

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

                eventAdmin.postEvent(new Event(topic, new Hashtable()
                {
                    {
                        put(EventConstants.EVENT, event);

                        put("role", event.getRole());

                        put("role.name", event.getRole().getName());

                        put("role.type", new Integer(event.getRole().getType()));

                        final ServiceReference ref = event
                            .getServiceReference();

                        if(null == ref)
                        {
                            throw new IllegalArgumentException(
                                "UserAdminEvent.getServiceReference() may not be null");
                        }

                        put(EventConstants.SERVICE, ref);

                        put(EventConstants.SERVICE_ID, ref
                            .getProperty(EventConstants.SERVICE_ID));

                        final Object objectClass = ref
                            .getProperty(Constants.OBJECTCLASS);

                        if(!(objectClass instanceof String[])
                            || !Arrays.asList((String[]) objectClass).contains(
                                UserAdmin.class.getName()))
                        {
                            throw new IllegalArgumentException(
                                "Bad objectclass: " + objectClass);
                        }

                        put(EventConstants.SERVICE_OBJECTCLASS, objectClass);

                        put(EventConstants.SERVICE_PID, ref
                            .getProperty(EventConstants.SERVICE_PID));
                    }
                }));

                m_context.ungetService(ref);
            }
        }
    }
}
