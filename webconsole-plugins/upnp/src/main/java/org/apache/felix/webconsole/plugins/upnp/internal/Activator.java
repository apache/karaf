/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.plugins.upnp.internal;

import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Activator is the main starting class.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer
{

    private ServiceTracker tracker;
    private BundleContext context;

    private SimpleWebConsolePlugin plugin;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public final void start(BundleContext context) throws Exception
    {
        this.context = context;
        this.tracker = new ServiceTracker(context, UPnPDevice.class.getName(), this);
        this.tracker.open();
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public final void stop(BundleContext context) throws Exception
    {
        if (tracker != null)
        {
            tracker.close();
            tracker = null;
        }
    }

    // - begin tracker
    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void modifiedService(ServiceReference reference, Object service)
    {/* unused */
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public final Object addingService(ServiceReference reference)
    {
        SimpleWebConsolePlugin plugin = this.plugin;
        if (plugin == null)
        {
            this.plugin = plugin = new WebConsolePlugin(tracker).register(context);
        }
        // delegate event
        ControlServlet controller = ((WebConsolePlugin) plugin).controller;
        if (controller != null)
            controller.addingService(reference);

        return context.getService(reference);
    }

    /**
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
     *      java.lang.Object)
     */
    public final void removedService(ServiceReference reference, Object service)
    {
        SimpleWebConsolePlugin plugin = this.plugin;

        if (plugin != null)
        {
            ControlServlet controller = ((WebConsolePlugin) plugin).controller;
            if (controller != null)
                controller.removedService(reference, service);
        }

        if (tracker.getTrackingCount() == 0 && plugin != null)
        {
            plugin.unregister();
            this.plugin = null;
        }

    }
}
