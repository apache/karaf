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
package org.apache.felix.webconsole.internal.servlet;


import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.internal.WebConsolePluginAdapter;
import org.apache.felix.webconsole.internal.i18n.ResourceBundleManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;


/**
 * The <code>PluginHolder</code> class implements the maintenance and lazy
 * access to web console plugin services.
 */
class PluginHolder implements ServiceListener
{

    // The Web Console's bundle context to access the plugin services
    private final BundleContext bundleContext;

    // registered plugins (Map<String label, Plugin plugin>)
    private final Map plugins;

    // The servlet context used to initialize plugin services
    private ServletContext servletContext;

    // the label of the default plugin
    private String defaultPluginLabel;


    PluginHolder( final BundleContext context )
    {
        this.bundleContext = context;
        this.plugins = new HashMap();
    }


    //---------- OsgiManager support API

    /**
     * Start using the plugin manager with registration as a service listener
     * and getting references to all plugins already registered in the
     * framework.
     */
    void open()
    {
        try
        {
            bundleContext.addServiceListener( this, "(" + Constants.OBJECTCLASS + "="
                + WebConsoleConstants.SERVICE_NAME + ")" );
        }
        catch ( InvalidSyntaxException ise )
        {
            // not expected, thus fail hard
            throw new InternalError( "Failed registering for Servlet service events: " + ise.getMessage() );
        }

        try
        {
            ServiceReference[] refs = bundleContext.getServiceReferences( WebConsoleConstants.SERVICE_NAME, null );
            if ( refs != null )
            {
                for ( int i = 0; i < refs.length; i++ )
                {
                    serviceAdded( refs[i] );
                }
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // not expected, thus fail hard
            throw new InternalError( "Failed getting existing Servlet services: " + ise.getMessage() );
        }
    }


    /**
     * Stop using the plugin manager by removing as a service listener and
     * releasing all held plugins, which includes ungetting and destroying any
     * held plugin services.
     */
    void close()
    {
        bundleContext.removeServiceListener( this );

        Plugin[] plugin = ( Plugin[] ) plugins.values().toArray( new Plugin[plugins.size()] );
        for ( int i = 0; i < plugin.length; i++ )
        {
            plugin[i].ungetService();
        }

        plugins.clear();
        defaultPluginLabel = null;
    }


    /**
     * Returns label of the default plugin
     * @return label of the default plugin
     */
    String getDefaultPluginLabel()
    {
        return defaultPluginLabel;
    }


    /**
     * Sets the label of the default plugin
     * @param defaultPluginLabel
     */
    void setDefaultPluginLabel( String defaultPluginLabel )
    {
        this.defaultPluginLabel = defaultPluginLabel;
    }


    /**
     * Returns the default plugin as identified by the {@link #getDefaultPlugin()}
     * or any plugin if no plugin is registered with that label
     *
     * @return The default plugin or <code>null</code> if no plugin is
     *      registered at all
     */
    AbstractWebConsolePlugin getDefaultPlugin()
    {
        return getPlugin( defaultPluginLabel );
    }


    /**
     * Adds an internal Web Console plugin
     * @param consolePlugin The internal Web Console plugin to add
     */
    void addOsgiManagerPlugin( final AbstractWebConsolePlugin consolePlugin )
    {
        final String label = consolePlugin.getLabel();
        final Plugin plugin = new Plugin( this, null, label );
        plugin.setTitle( consolePlugin.getTitle() );
        plugin.setConsolePlugin( consolePlugin );
        addPlugin( label, plugin );
    }


    /**
     * Remove the internal Web Console plugin registered under the given label
     * @param label The label of the Web Console internal plugin to remove
     */
    void removeOsgiManagerPlugin( final String label )
    {
        removePlugin( label );
    }


    /**
     * Returns the plugin registered under the given label or <code>null</code>
     * if none is registered under that label. If the label is <code>null</code>
     * or empty, any registered plugin is returned or <code>null</code> if
     * no plugin is registered
     *
     * @param label The label of the plugin to return
     * @return The plugin or <code>null</code> if no plugin is registered with
     *      the given label.
     */
    AbstractWebConsolePlugin getPlugin( final String label )
    {
        if ( label == null || label.length() == 0 )
        {
            if ( !plugins.isEmpty() )
            {
                return ( ( Plugin ) plugins.values().iterator().next() ).getConsolePlugin();
            }
        }
        else
        {
            Plugin plugin = ( Plugin ) plugins.get( label );
            if ( plugin != null )
            {
                return plugin.getConsolePlugin();
            }
        }

        // no such plugin (or not any more)
        return null;
    }


    /**
     * Builds the map of labels to plugin titles to be stored as the
     * <code>felix.webconsole.labelMap</code> request attribute. This map
     * optionally localizes the plugin title using the providing bundle's
     * resource bundle if the first character of the title is a percent
     * sign (%). Titles not prefixed with a percent sign are added to the
     * map unmodified.
     *
     * @param resourceBundleManager The ResourceBundleManager providing
     *      localized titles
     * @param locale The locale to which the titles are to be localized
     *
     * @return The localized map of labels to titles
     */
    Map getLocalizedLabelMap( final ResourceBundleManager resourceBundleManager, final Locale locale )
    {
        final Map map = new HashMap();
        for ( Iterator pi = plugins.values().iterator(); pi.hasNext(); )
        {
            final Plugin plugin = ( Plugin ) pi.next();
            final String label = plugin.getLabel();
            String title = plugin.getTitle();
            if ( title.startsWith( "%" ) )
            {
                try
                {
                    final ResourceBundle resourceBundle = resourceBundleManager.getResourceBundle( plugin.getBundle(),
                        locale );
                    title = resourceBundle.getString( title.substring( 1 ) );
                }
                catch ( Throwable e )
                {
                    /* ignore missing resource - use default title */
                }
            }
            map.put( label, title );
        }

        return map;
    }


    /**
     * Returns the bundle context of the Web Console itself.
     * @return the bundle context of the Web Console itself.
     */
    BundleContext getBundleContext()
    {
        return bundleContext;
    }


    /**
     * Sets the servlet context to be used to initialize plugin services
     * @param servletContext
     */
    void setServletContext( ServletContext servletContext )
    {
        this.servletContext = servletContext;
    }


    /**
     * Returns the servlet context to be used to initialize plugin services
     * @return the servlet context to be used to initialize plugin services
     */
    ServletContext getServletContext()
    {
        return servletContext;
    }


    //---------- ServletListener

    /**
     * Called when plugin services are registered or unregistered (or modified,
     * which is currently ignored)
     */
    public void serviceChanged( ServiceEvent event )
    {
        switch ( event.getType() )
        {
            case ServiceEvent.REGISTERED:
                // add service
                serviceAdded( event.getServiceReference() );
                break;

            case ServiceEvent.UNREGISTERING:
                // remove service
                serviceRemoved( event.getServiceReference() );
                break;

            default:
                // update service
                break;
        }
    }


    private void serviceAdded( final ServiceReference serviceReference )
    {
        final String label = getProperty( serviceReference, WebConsoleConstants.PLUGIN_LABEL );
        if ( label != null )
        {
            addPlugin( label, new Plugin( this, serviceReference, label ) );
        }
    }


    private void serviceRemoved( final ServiceReference serviceReference )
    {
        final String label = getProperty( serviceReference, WebConsoleConstants.PLUGIN_LABEL );
        if ( label != null )
        {
            removePlugin( label );
        }
    }


    private void addPlugin( final String label, final Plugin plugin )
    {
        plugins.put( label, plugin );
    }


    private void removePlugin( final String label )
    {
        final Plugin oldPlugin = ( Plugin ) plugins.remove( label );
        if ( oldPlugin != null )
        {
            oldPlugin.ungetService();
        }
    }


    static String getProperty( final ServiceReference service, final String propertyName )
    {
        final Object property = service.getProperty( propertyName );
        if ( property instanceof String )
        {
            return ( String ) property;
        }

        return null;
    }

    private static final class Plugin implements ServletConfig
    {
        private final PluginHolder holder;
        private final ServiceReference serviceReference;
        private final String label;
        private String title;
        private AbstractWebConsolePlugin consolePlugin;


        Plugin( final PluginHolder holder, final ServiceReference serviceReference, final String label )
        {
            this.holder = holder;
            this.serviceReference = serviceReference;
            this.label = label;
        }


        final Bundle getBundle()
        {
            if ( serviceReference != null )
            {
                return serviceReference.getBundle();
            }
            return holder.getBundleContext().getBundle();
        }


        final String getLabel()
        {
            return label;
        }


        void setTitle( String title )
        {
            this.title = title;
        }


        final String getTitle()
        {
            if ( title == null )
            {
                // assumption: serviceReference is only null for WebConsole
                // internal plugins, for which the title field will always be set

                // check service Reference
                title = getProperty( serviceReference, WebConsoleConstants.PLUGIN_TITLE );
                if ( title == null )
                {
                    // temporarily set the title to a non-null value to prevent
                    // recursion issues if this method or the getServletName
                    // method is called while the servlet is being acquired
                    title = label;

                    // get the service now
                    acquireServlet();

                    // reset the title:
                    // - null if the servlet cannot be loaded
                    // - to the servlet's actual title if the servlet is loaded
                    title = ( consolePlugin != null ) ? consolePlugin.getTitle() : null;
                }
            }
            return title;
        }


        final AbstractWebConsolePlugin getConsolePlugin()
        {
            acquireServlet();
            return consolePlugin;
        }


        void setConsolePlugin( AbstractWebConsolePlugin service )
        {
            try
            {
                service.init( this );
                this.consolePlugin = service;
            }
            catch ( ServletException se )
            {
                // TODO:
                // log( LogService.LOG_WARNING, "Initialization of plugin '" + plugin.getTitle() + "' (" + plugin.getLabel()
                //      + ") failed; not using this plugin", se );
            }

        }


        final void ungetService()
        {
            if ( consolePlugin != null )
            {
                try
                {
                    consolePlugin.destroy();
                }
                catch ( Exception e )
                {
                    // TODO: handle
                }
                consolePlugin = null;

                // service reference may be null for WebConsole internal plugins
                if ( serviceReference != null )
                {
                    holder.getBundleContext().ungetService( serviceReference );
                }
            }
        }


        //---------- ServletConfig interface

        public String getInitParameter( String name )
        {
            if ( serviceReference != null )
            {
                Object property = serviceReference.getProperty( name );
                if ( property != null && !property.getClass().isArray() )
                {
                    return property.toString();
                }
            }

            return null;
        }


        public Enumeration getInitParameterNames()
        {
            final String[] keys = ( serviceReference == null ) ? new String[0] : serviceReference.getPropertyKeys();
            return new Enumeration()
            {
                int idx = 0;


                public boolean hasMoreElements()
                {
                    return idx < keys.length;
                }


                public Object nextElement()
                {
                    if ( hasMoreElements() )
                    {
                        return keys[idx++];
                    }
                    throw new NoSuchElementException();
                }

            };
        }


        public ServletContext getServletContext()
        {
            return holder.getServletContext();
        }


        public String getServletName()
        {
            return getTitle();
        }


        private void acquireServlet()
        {
            if ( consolePlugin == null )
            {
                // assumption: serviceReference is only null for WebConsole
                // internal plugins, for which the consolePlugin field will
                // always be set

                Object service = holder.getBundleContext().getService( serviceReference );
                if ( service instanceof Servlet )
                {
                    final AbstractWebConsolePlugin servlet;
                    if ( service instanceof AbstractWebConsolePlugin )
                    {
                        servlet = ( AbstractWebConsolePlugin ) service;
                    }
                    else
                    {
                        servlet = new WebConsolePluginAdapter( label, ( Servlet ) service, serviceReference );
                    }

                    setConsolePlugin( servlet );
                }
            }
        }
    }
}
