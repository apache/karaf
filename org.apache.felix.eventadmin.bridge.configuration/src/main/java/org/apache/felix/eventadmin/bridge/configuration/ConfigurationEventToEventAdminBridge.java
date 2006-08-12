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
package org.apache.felix.eventadmin.bridge.configuration;

import java.util.Arrays;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ConfigurationEventToEventAdminBridge implements
    ConfigurationListener
{
    private final BundleContext m_context;

    public ConfigurationEventToEventAdminBridge(final BundleContext context)
    {
        m_context = context;

        m_context.registerService(ConfigurationListener.class.getName(), this,
            null);
    }

    public void configurationEvent(final ConfigurationEvent event)
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
                    case ConfigurationEvent.CM_UPDATED:
                        topic = "org/osgi/service/cm/ConfigurationEvent/CM_UPDATED";
                        break;
                    case ConfigurationEvent.CM_DELETED:
                        topic = "org/osgi/service/cm/ConfigurationEvent/CM_DELETED";
                        break;
                    default:
                        m_context.ungetService(ref);
                        return;
                }
                
                final Hashtable properties = new Hashtable();
                
                if(null != event.getFactoryPid())
                {
                    properties.put("cm.factoryPid", event.getFactoryPid());
                }
                
                properties.put("cm.pid", event.getPid());
                
                final ServiceReference eventRef = event.getReference();

                if(null == eventRef)
                {
                    throw new IllegalArgumentException(
                        "ConfigurationEvent.getReference() may not be null");
                }

                properties.put(EventConstants.SERVICE, eventRef);

                properties.put(EventConstants.SERVICE_ID, eventRef.getProperty(
                    EventConstants.SERVICE_ID));

                final Object objectClass = eventRef.getProperty(
                    Constants.OBJECTCLASS);

                if(!(objectClass instanceof String[])
                    || !Arrays.asList((String[]) objectClass).contains(
                    ConfigurationAdmin.class.getName()))
                {
                    throw new IllegalArgumentException(
                        "Bad objectclass: " + objectClass);
                }

                properties.put(EventConstants.SERVICE_OBJECTCLASS, objectClass);

                properties.put(EventConstants.SERVICE_PID, eventRef.getProperty(
                    EventConstants.SERVICE_PID));
                
                eventAdmin.postEvent(new Event(topic, properties));

                m_context.ungetService(ref);
            }
        }
    }
}
