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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

/**
 * This class registers itself as a listener for framework events and posts them via
 * the EventAdmin as specified in 113.6.3 OSGi R4 compendium.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FrameworkEventAdapter extends AbstractAdapter implements FrameworkListener
{
    /**
     * The constructor of the adapter. This will register the adapter with the
     * given context as a <tt>FrameworkListener</tt> and subsequently, will
     * post received events via the given EventAdmin.
     *
     * @param context The bundle context with which to register as a listener.
     * @param admin The <tt>EventAdmin</tt> to use for posting events.
     */
    public FrameworkEventAdapter(final BundleContext context, final EventAdmin admin)
    {
        super(admin);

        context.addFrameworkListener(this);
    }

    public void destroy(BundleContext context) {
        context.removeFrameworkListener(this);
    }

    /**
     * Once a framework event is received this method assembles and posts an event
     * via the <tt>EventAdmin</tt> as specified in 113.6.3 OSGi R4 compendium.
     *
     * @param event The event to adapt.
     */
    public void frameworkEvent(final FrameworkEvent event)
    {
        final Dictionary properties = new Hashtable();

        properties.put(EventConstants.EVENT, event);

        final Bundle bundle = event.getBundle();

        if (null != bundle)
        {
            properties.put("bundle.id", new Long(bundle.getBundleId()));

            final String symbolicName = bundle.getSymbolicName();

            if (null != symbolicName)
            {
                properties.put(EventConstants.BUNDLE_SYMBOLICNAME,
                    symbolicName);
            }

            properties.put("bundle", bundle);
        }

        final Throwable thrown = event.getThrowable();

        if (null != thrown)
        {
            properties.put(EventConstants.EXCEPTION_CLASS,
                thrown.getClass().getName());

            final String message = thrown.getMessage();

            if (null != message)
            {
                properties.put(EventConstants.EXCEPTION_MESSAGE,
                    message);
            }

            properties.put(EventConstants.EXCEPTION, thrown);
        }

        final StringBuffer topic = new StringBuffer(
            FrameworkEvent.class.getName().replace('.', '/'))
            .append('/');

        switch (event.getType())
        {
            case FrameworkEvent.STARTED:
                topic.append("STARTED");
                break;
            case FrameworkEvent.ERROR:
                topic.append("ERROR");
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                topic.append("PACKAGES_REFRESHED");
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                topic.append("STARTLEVEL_CHANGED");
                break;
            case FrameworkEvent.WARNING:
                topic.append("WARNING");
                break;
            case FrameworkEvent.INFO:
                topic.append("INFO");
                break;
            default:
                return; // IGNORE EVENT
        }

        try {
            getEventAdmin().postEvent(new Event(topic.toString(), properties));
        } catch(IllegalStateException e) {
            // This is o.k. - indicates that we are stopped.
        }
    }
}
