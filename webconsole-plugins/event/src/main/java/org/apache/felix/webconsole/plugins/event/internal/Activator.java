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

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.osgi.framework.*;

/**
 * Setup the event plugin
 */
public class Activator implements BundleActivator
{
    /** Registration for the plugin. */
    private ServiceRegistration pluginRegistration;

    /** Listener */
    private EventListener eventListener;

    /** The plugin. */
    private PluginServlet plugin;

    /** Optional features handler. */
    private OptionalFeaturesHandler featuresHandler;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(final BundleContext context) throws Exception
    {
        // we start with the plugin
        this.plugin = new PluginServlet();

        // now we create the listener
        this.eventListener = new EventListener(this.plugin, context);

        // and the optional features handler
        this.featuresHandler = new OptionalFeaturesHandler(this.plugin, context);

        // finally we register the plugin
        final Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Event plugin for the Apache Felix Web Console" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        props.put( "felix.webconsole.label", "events");
        props.put( "felix.webconsole.title", "Events");
        props.put( "felix.webconsole.css", "/events/res/ui/events.css");
        this.pluginRegistration = context.registerService(Servlet.class.getName(),
                                plugin,
                                props);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(final BundleContext context) throws Exception
    {
        if ( this.pluginRegistration != null )
        {
            this.pluginRegistration.unregister();
            this.pluginRegistration = null;
        }
        if ( this.eventListener != null )
        {
            this.eventListener.destroy();
            eventListener = null;
        }
        if ( this.featuresHandler != null)
        {
            this.featuresHandler.destroy();
            this.featuresHandler = null;
        }
        if ( this.plugin != null ) {
            this.plugin.destroy();
            this.plugin = null;
        }
    }

}
