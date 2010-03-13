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
package org.apache.felix.webconsole;


import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;


/**
 * SimpleWebConsolePlugin is an utility class that provides default
 * implementation of the {@link AbstractWebConsolePlugin} and supports the
 * following features:
 * <ul>
 * <li>Methods for (un)registering the web console plugin service</li>
 * <li>Default implementation for resource loading</li>
 * </ul>
 */
public abstract class SimpleWebConsolePlugin extends AbstractWebConsolePlugin
{

    // Serializable UID
    private static final long serialVersionUID = 1500463493078823878L;

    // used for standard AbstractWebConsolePlugin implementation
    private final String label;
    private final String title;
    private final String css[];
    private final String labelRes;
    private final int labelResLen;

    // used for service registration
    private final Object regLock = new Object();
    private ServiceRegistration reg;

    // used to obtain services. Structure is: service name -> ServiceTracker
    private final Map services = new HashMap();


    /**
     * Creates new Simple Web Console Plugin.
     *
     * @param label the front label. See
     *          {@link AbstractWebConsolePlugin#getLabel()}
     * @param title the plugin title . See
     *          {@link AbstractWebConsolePlugin#getTitle()}
     * @param css the additional plugin CSS. See
     *          {@link AbstractWebConsolePlugin#getCssReferences()}
     */
    public SimpleWebConsolePlugin( String label, String title, String css[] )
    {
        if ( label == null )
        {
            throw new NullPointerException( "Null label" );
        }
        if ( title == null )
        {
            throw new NullPointerException( "Null title" );
        }
        this.label = label;
        this.title = title;
        this.css = css;
        this.labelRes = '/' + label + '/';
        this.labelResLen = labelRes.length() - 1;
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getLabel()
     */
    public final String getLabel()
    {
        return label;
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getTitle()
     */
    public final String getTitle()
    {
        return title;
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getCssReferences()
     */
    protected final String[] getCssReferences()
    {
        return css;
    }


    /**
     * Called internally by {@link AbstractWebConsolePlugin} to load resources.
     *
     * This particular implementation depends on the label. As example, if the
     * plugin is accessed as <code>/system/console/abc</code>, and the plugin
     * resources are accessed like <code>/system/console/abc/res/logo.gif</code>,
     * the code here will try load resource <code>/res/logo.gif</code> from the
     * bundle, providing the plugin.
     *
     *
     * @param path the path to read.
     * @return the URL of the resource or <code>null</code> if not found.
     */
    protected URL getResource( String path )
    {
        return ( path != null && path.startsWith( labelRes ) ) ? //
        getClass().getResource( path.substring( labelResLen ) )
            : null;
    }


    // -- begin methods for plugin registration/unregistration
    /**
     * This is an utility method. It is used to register the plugin service. Don't
     * forget to call the {@link #unregister()} when the plugin is no longer
     * needed.
     *
     * @param bc the bundle context used for service registration.
     * @return self
     */
    public final SimpleWebConsolePlugin register( BundleContext bc )
    {
        synchronized ( regLock )
        {
            activate( bc ); // don't know why this is needed!

            Hashtable props = new Hashtable();
            props.put( WebConsoleConstants.PLUGIN_LABEL, label );
            props.put( WebConsoleConstants.PLUGIN_TITLE, title );
            reg = bc.registerService( "javax.servlet.Servlet", this, props ); //$NON-NLS-1$
        }
        return this;
    }


    /**
     * An utility method that removes the service, registered by the
     * {@link #register(BundleContext)} method.
     */
    public final void unregister()
    {
        synchronized ( regLock )
        {
            deactivate(); // is this needed?

            if ( reg != null )
                reg.unregister();
            reg = null;
        }
    }


    // -- end methods for plugin registration/unregistration

    // -- begin methods for obtaining services

    /**
     * Gets the service with the specified class name. Will create a new
     * {@link ServiceTracker} if the service is not already got.
     *
     * @param serviceName the service name to obtain
     * @return the service or <code>null</code> if missing.
     */
    public final Object getService( String serviceName )
    {
        ServiceTracker serviceTracker = ( ServiceTracker ) services.get( serviceName );
        if ( serviceTracker == null )
        {
            serviceTracker = new ServiceTracker( getBundleContext(), serviceName, null );
            serviceTracker.open();

            services.put( serviceName, serviceTracker );
        }

        return serviceTracker.getService();
    }


    /**
     * This method will close all service trackers, created by
     * {@link #getService(String)} method. If you override this method, don't
     * forget to call the super.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#deactivate()
     */
    public void deactivate()
    {
        for ( Iterator ti = services.values().iterator(); ti.hasNext(); )
        {
            ServiceTracker tracker = ( ServiceTracker ) ti.next();
            tracker.close();
            ti.remove();
        }
        super.deactivate();
    }

}
