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
package org.apache.felix.webconsole.plugins.event.internal;

import org.apache.felix.webconsole.plugins.event.internal.converter.*;
import org.osgi.framework.*;

/**
 * This class listens for all known framework events:
 * - bundle events
 * - framework events
 * - service events
 */
public class EventListener
    implements BundleListener, FrameworkListener, ServiceListener{

    private final EventCollector collector;

    private final BundleContext bundleContext;

    public EventListener(final PluginServlet plugin, final BundleContext context)
    {
        this.collector = plugin.getCollector();
        this.bundleContext = context;
        context.addBundleListener(this);
        context.addFrameworkListener(this);
        context.addServiceListener(this);
    }

    public void destroy()
    {
        this.bundleContext.removeBundleListener(this);
        this.bundleContext.removeFrameworkListener(this);
        this.bundleContext.removeServiceListener(this);
    }

    /**
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent event)
    {
        final EventInfo info = ServiceEventConverter.getInfo(event);
        this.collector.add(info);
    }

    /**
     * @see org.osgi.framework.FrameworkListener#frameworkEvent(org.osgi.framework.FrameworkEvent)
     */
    public void frameworkEvent(FrameworkEvent event)
    {
        final EventInfo info = FrameworkEventConverter.getInfo(event);
        this.collector.add(info);
    }

    /**
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    public void bundleChanged(BundleEvent event)
    {
        final EventInfo info = BundleEventConverter.getInfo(event);
        this.collector.add(info);
    }
}
