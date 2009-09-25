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

import org.osgi.framework.*;

/**
 * This class handles all optional stuff.
 * It listens for specific services and activates/deactivates
 * features.
 */
public class OptionalFeaturesHandler
    implements ServiceListener
{
    private static final String EVENT_ADMIN_CLASS_NAME = "org.osgi.service.event.EventAdmin";
    private static final String CONFIGURATION_ADMIN_CLASS_NAME = "org.osgi.service.cm.ConfigurationAdmin";
    private static final String EVENT_HANDLER_CLASS_NAME = "org.osgi.service.event.EventHandler";

    private static final String FILTER = "(|(" + Constants.OBJECTCLASS + "=" + EVENT_ADMIN_CLASS_NAME + ")"
                                        +"(" + Constants.OBJECTCLASS + "=" + CONFIGURATION_ADMIN_CLASS_NAME + "))";

    /** Event admin service reference */
    private ServiceReference eventAdminReference;

    /** Registration for the event handler. */
    private ServiceRegistration eventHandlerRegistration;

    /** Configuration admin service reference */
    private ServiceReference configAdminReference;

    /** Registration for the configuration listener. */
    private ServiceRegistration configListenerRegistration;

    /** Bundle context. */
    private final BundleContext bundleContext;

    /** The plugin */
    private final PluginServlet plugin;

    public OptionalFeaturesHandler(final PluginServlet plugin, final BundleContext context)
    {
        this.plugin = plugin;
        this.bundleContext = context;
        // check if event admin is already available
        this.eventAdminReference = null;
        final ServiceReference ref = this.bundleContext.getServiceReference(EVENT_ADMIN_CLASS_NAME);
        if ( ref != null )
        {
            bindEventAdmin(ref);
        }

        // check if config admin is already available
        this.configAdminReference = null;
        final ServiceReference cfaRef = this.bundleContext.getServiceReference(CONFIGURATION_ADMIN_CLASS_NAME);
        if ( cfaRef != null )
        {
            bindConfigAdmin(cfaRef);
        }

        // listen for event and config admin from now on
        try {
            context.addServiceListener(this, FILTER);
        } catch (InvalidSyntaxException ise) {
            // this should never happen as this is a constant, so we ignore it
        }
    }

    /**
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy()
    {
        this.bundleContext.removeServiceListener(this);
        this.unbindEventAdmin(this.eventAdminReference);
        this.unbindConfigAdmin(this.configAdminReference);
    }

    /**
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(final ServiceEvent event)
    {
        final String[] objectClasses =  (String[])event.getServiceReference().getProperty(Constants.OBJECTCLASS);
        if ( objectClasses != null)
        {
            for(int i=0; i<objectClasses.length; i++)
            {
                if ( objectClasses[i].equals(EVENT_ADMIN_CLASS_NAME) )
                {
                    if ( event.getType() == ServiceEvent.REGISTERED )
                    {
                        this.bindEventAdmin(event.getServiceReference());
                    }
                    else if ( event.getType() == ServiceEvent.UNREGISTERING )
                    {
                        this.unbindEventAdmin(event.getServiceReference());
                    }
                }
                else if ( objectClasses[i].equals(CONFIGURATION_ADMIN_CLASS_NAME) )
                {
                    if ( event.getType() == ServiceEvent.REGISTERED )
                    {
                        this.bindConfigAdmin(event.getServiceReference());
                    }
                    else if ( event.getType() == ServiceEvent.UNREGISTERING )
                    {
                        this.unbindConfigAdmin(event.getServiceReference());
                    }
                }
            }
        }
    }

    private void bindEventAdmin(final ServiceReference ref)
    {
        if ( this.eventAdminReference != null)
        {
            this.unbindEventAdmin(this.eventAdminReference);
        }
        this.eventAdminReference = ref;
        final Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Event handler for the Apache Felix Web Console" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        props.put( "event.topics", "*");
        this.plugin.setEventAdminAvailable(true);

        this.eventHandlerRegistration = this.bundleContext.registerService(EVENT_HANDLER_CLASS_NAME,
                new EventHandler(this.plugin.getCollector()), props);
    }

    private void unbindEventAdmin(final ServiceReference ref)
    {
        if ( this.eventAdminReference == ref )
        {
            this.eventAdminReference = null;
            this.plugin.setEventAdminAvailable(false);
            if ( this.eventHandlerRegistration != null )
            {
                this.eventHandlerRegistration.unregister();
                this.eventHandlerRegistration = null;
            }
        }
    }

    private void bindConfigAdmin(final ServiceReference ref)
    {
        if ( this.configAdminReference != null )
        {
            this.unbindConfigAdmin(this.configAdminReference);
        }
        this.plugin.setConfigAdminAvailable(true);
        this.configAdminReference = ref;
        this.configListenerRegistration = ConfigurationListener.create(this.bundleContext, this.plugin);
    }

    private void unbindConfigAdmin(final ServiceReference ref)
    {
        if ( this.configAdminReference == ref )
        {
            this.configAdminReference = null;
            this.plugin.setConfigAdminAvailable(false);
            if ( this.configListenerRegistration != null )
            {
                this.configListenerRegistration.unregister();
                this.configListenerRegistration = null;
            }
        }
    }
}
