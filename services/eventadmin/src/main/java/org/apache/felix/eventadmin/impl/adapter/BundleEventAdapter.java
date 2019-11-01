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

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * This class registers itself as a listener for bundle events and posts them via
 * the EventAdmin as specified in 113.6.4 OSGi R4 compendium.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleEventAdapter extends AbstractAdapter implements SynchronousBundleListener
{
    /**
     * The constructor of the adapter. This will register the adapter with the given
     * context as a <code>BundleListener</code> and subsequently, will post received
     * events via the given EventAdmin.
     *
     * @param context The bundle context with which to register as a listener.
     * @param admin The <code>EventAdmin</code> to use for posting events.
     */
    public BundleEventAdapter(final BundleContext context, final EventAdmin admin)
    {
        super(admin);
        context.addBundleListener(this);
    }

    @Override
    public void destroy(BundleContext context) {
        context.removeBundleListener(this);
    }

    /**
     * Once a bundle event is received this method assembles and posts an event via
     * the <code>EventAdmin</code> as specified in 113.6.4 OSGi R4 compendium.
     *
     * @param event The event to adapt.
     */
    @Override
    public void bundleChanged(final BundleEvent event)
    {
        final Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(EventConstants.EVENT, event);

        properties.put("bundle.id", Long.valueOf(event.getBundle()
            .getBundleId()));

        final String symbolicName = event.getBundle().getSymbolicName();

        if (null != symbolicName)
        {
            properties.put(EventConstants.BUNDLE_SYMBOLICNAME,
                symbolicName);
        }

        properties.put("bundle", event.getBundle());

        final StringBuilder topic = new StringBuilder(BundleEvent.class
            .getName().replace('.', '/')).append('/');

        switch (event.getType())
        {
            case BundleEvent.INSTALLED:
                topic.append("INSTALLED");
                break;
            case BundleEvent.STARTING:
                topic.append("STARTING");
                break;
            case BundleEvent.STARTED:
                topic.append("STARTED");
                break;
            case BundleEvent.STOPPING:
                topic.append("STOPPING");
                break;
            case BundleEvent.STOPPED:
                topic.append("STOPPED");
                break;
            case BundleEvent.UPDATED:
                topic.append("UPDATED");
                break;
            case BundleEvent.UNINSTALLED:
                topic.append("UNINSTALLED");
                break;
            case BundleEvent.RESOLVED:
                topic.append("RESOLVED");
                break;
            case BundleEvent.UNRESOLVED:
                topic.append("UNRESOLVED");
                break;
            default:
                return; // IGNORE EVENT
        }

        try {
            getEventAdmin().postEvent(new Event(topic.toString(), properties));
        } catch (IllegalStateException e) {
            // This is o.k. - indicates that we are stopped.
        }
    }
}
