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
package org.apache.felix.webconsole.internal.servlet;


import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.Action;
import org.apache.felix.webconsole.BrandingPlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.Logger;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.WebConsolePluginAdapter;
import org.apache.felix.webconsole.internal.core.BundlesServlet;
import org.apache.felix.webconsole.internal.filter.FilteringResponseWrapper;
import org.apache.felix.webconsole.internal.i18n.ResourceBundleManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>OSGi Manager</code> is the actual Web Console Servlet which
 * is registered with the OSGi Http Service and which maintains registered
 * console plugins.
 */
public class OsgiManager extends GenericServlet
{

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    /**
     * Old name of the request attribute provding the root to the web console.
     * This attribute is no deprecated and replaced by
     * {@link WebConsoleConstants#ATTR_APP_ROOT}.
     *
     * @deprecated use {@link WebConsoleConstants#ATTR_APP_ROOT} instead
     */
    private static final String ATTR_APP_ROOT_OLD = OsgiManager.class.getName() + ".appRoot";

    /**
     * Old name of the request attribute provding the mappings from label to
     * page title. This attribute is no deprecated and replaced by
     * {@link WebConsoleConstants#ATTR_LABEL_MAP}.
     *
     * @deprecated use {@link WebConsoleConstants#ATTR_LABEL_MAP} instead
     */
    private static final String ATTR_LABEL_MAP_OLD = OsgiManager.class.getName() + ".labelMap";

    /**
     * The name and value of a parameter which will prevent redirection to a
     * render after the action has been executed (value is "_noredir_"). This
     * may be used by programmatic action submissions.
     */
    public static final String PARAM_NO_REDIRECT_AFTER_ACTION = "_noredir_";

    static final String PROP_MANAGER_ROOT = "manager.root";

    static final String PROP_DEFAULT_RENDER = "default.render";

    static final String PROP_REALM = "realm";

    static final String PROP_USER_NAME = "username";

    static final String PROP_PASSWORD = "password";

    static final String PROP_ENABLED_PLUGINS = "plugins";

    static final String DEFAULT_PAGE = BundlesServlet.NAME;

    static final String DEFAULT_REALM = "OSGi Management Console";

    static final String DEFAULT_USER_NAME = "admin";

    static final String DEFAULT_PASSWORD = "admin";

    /**
     * The default value for the {@link #PROP_MANAGER_ROOT} configuration
     * property (value is "/system/console").
     */
    static final String DEFAULT_MANAGER_ROOT = "/system/console";

    static final String[] PLUGIN_CLASSES =
        {
            "org.apache.felix.webconsole.internal.compendium.ComponentConfigurationPrinter",
            "org.apache.felix.webconsole.internal.compendium.ComponentsServlet",
            "org.apache.felix.webconsole.internal.compendium.ConfigManager",
            "org.apache.felix.webconsole.internal.compendium.ConfigurationAdminConfigurationPrinter",
            "org.apache.felix.webconsole.internal.compendium.LogServlet",
            "org.apache.felix.webconsole.internal.compendium.PreferencesConfigurationPrinter",
            "org.apache.felix.webconsole.internal.core.BundlesServlet",
            "org.apache.felix.webconsole.internal.core.ServicesServlet",
            "org.apache.felix.webconsole.internal.core.InstallAction",
            "org.apache.felix.webconsole.internal.deppack.DepPackServlet",
            "org.apache.felix.webconsole.internal.misc.LicenseServlet",
            "org.apache.felix.webconsole.internal.misc.ConfigurationRender",
            "org.apache.felix.webconsole.internal.misc.ShellServlet",
            "org.apache.felix.webconsole.internal.obr.BundleRepositoryRender",
            "org.apache.felix.webconsole.internal.system.VMStatPlugin"
        };

    private BundleContext bundleContext;

    private Logger log;

    private ServiceTracker httpServiceTracker;

    private HttpService httpService;

    private ServiceTracker operationsTracker;

    private ServiceTracker pluginsTracker;

    private ServiceTracker brandingTracker;

    private ServiceRegistration configurationListener;

    // map of plugins: indexed by the plugin label (String), values are
    // AbstractWebConsolePlugin instances
    private Map plugins = new HashMap();

    // map of labels to plugin titles: indexed by plugin label (String, values
    // are plugin titles
    private Map labelMap = new HashMap();

    private Map operations = new HashMap();

    private AbstractWebConsolePlugin defaultPlugin;

    private String defaultRenderName;

    private String webManagerRoot;

    // true if the OsgiManager is registered as a Servlet with the HttpService
    private boolean httpServletRegistered;

    // true if the resources have been registered with the HttpService
    private boolean httpResourcesRegistered;

    private Dictionary configuration;

    private Set enabledPlugins;

    private ResourceBundleManager resourceBundleManager;

    public OsgiManager( BundleContext bundleContext )
    {

        this.bundleContext = bundleContext;
        this.log = new Logger( bundleContext );

        updateConfiguration( null );

        try
        {
            this.configurationListener = ConfigurationListener2.create( this );
        }
        catch ( Throwable t2 )
        {
            // might be caused by Metatype API not available
            // try without MetaTypeProvider
            try
            {
                this.configurationListener = ConfigurationListener.create( this );
            }
            catch ( Throwable t )
            {
                // might be caused by CM API not available
            }
        }

        // get at the HttpService first, this should initialize
        // the OSGi Manager and start the initial setup
        httpServiceTracker = new HttpServiceTracker( this );
        httpServiceTracker.open();
    }


    public void dispose()
    {
        // now drop the HttpService and continue with further destroyals
        if ( httpServiceTracker != null )
        {
            httpServiceTracker.close();
            httpServiceTracker = null;
        }

        // stop listening for configuration
        if ( configurationListener != null )
        {
            configurationListener.unregister();
            configurationListener = null;
        }

        if ( log != null )
        {
            log.dispose();
        }

        this.defaultPlugin = null;
        this.bundleContext = null;
    }


    //---------- Servlet API

    public void init()
    {
        // base class initialization not needed, since the GenericServlet.init
        // is an empty method

        // setup the included plugins
        ClassLoader classLoader = getClass().getClassLoader();
        for ( int i = 0; i < PLUGIN_CLASSES.length; i++ )
        {
            String pluginClassName = PLUGIN_CLASSES[i];

            try
            {
                Class pluginClass = classLoader.loadClass( pluginClassName );
                Object plugin = pluginClass.newInstance();

                // check whether enabled by configuration
                if ( isPluginDisabled( pluginClassName, plugin ) )
                {
                    log.log( LogService.LOG_INFO, "Ignoring plugin " + pluginClassName + ": Disabled by configuration" );
                    continue;
                }

                if ( plugin instanceof OsgiManagerPlugin )
                {
                    ( ( OsgiManagerPlugin ) plugin ).activate( bundleContext );
                }
                if ( plugin instanceof AbstractWebConsolePlugin )
                {
                    bindServlet( ( AbstractWebConsolePlugin ) plugin );
                }
                else
                {
                    if ( plugin instanceof Action )
                    {
                        bindOperation( ( Action ) plugin );
                    }
                    if ( plugin instanceof BrandingPlugin )
                    {
                        AbstractWebConsolePlugin.setBrandingPlugin((BrandingPlugin) plugin);
                    }
                }
            }
            catch ( NoClassDefFoundError ncdfe )
            {
                String message = ncdfe.getMessage();
                if ( message == null )
                {
                    // no message, construct it
                    message = "Class definition not found (NoClassDefFoundError)";
                }
                else if ( message.indexOf( ' ' ) < 0 )
                {
                    // message is just a class name, try to be more descriptive
                    message = "Class " + message + " missing";
                }
                log.log( LogService.LOG_INFO, pluginClassName + " not enabled. Reason: " + message );
            }
            catch ( Throwable t )
            {
                log.log( LogService.LOG_INFO, "Failed to instantiate plugin " + pluginClassName + ". Reason: " + t );
            }
        }

        // start tracking external plugins after setting up our own plugins
        operationsTracker = new OperationServiceTracker( this );
        operationsTracker.open();
        pluginsTracker = new PluginServiceTracker( this );
        pluginsTracker.open();
        brandingTracker = new BrandingServiceTracker(this);
        brandingTracker.open();

        // the resource bundle manager
        resourceBundleManager = new ResourceBundleManager( getBundleContext() );
    }

    public void service( ServletRequest req, ServletResponse res ) throws ServletException, IOException
    {

        HttpServletRequest request = ( HttpServletRequest ) req;
        HttpServletResponse response = ( HttpServletResponse ) res;

        // handle the request action, terminate if done
        if ( this.handleAction( request, response ) )
        {
            return;
        }

        // check whether we are not at .../{webManagerRoot}
        if ( request.getPathInfo() == null || request.getPathInfo().equals( "/" ) )
        {
            String path = request.getRequestURI();
            if ( !path.endsWith( "/" ) )
            {
                path = path.concat( "/" );
            }
            path = path.concat( defaultRenderName );
            response.sendRedirect( path );
            return;
        }

        String label = request.getPathInfo();
        int slash = label.indexOf( "/", 1 );
        if ( slash < 2 )
        {
            slash = label.length();
        }

        label = label.substring( 1, slash );
        AbstractWebConsolePlugin plugin = ( AbstractWebConsolePlugin ) plugins.get( label );
        if ( plugin != null )
        {
            // the official request attributes
            req.setAttribute( WebConsoleConstants.ATTR_LABEL_MAP, labelMap );
            req.setAttribute( WebConsoleConstants.ATTR_APP_ROOT, request.getContextPath() + request.getServletPath() );
            req.setAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT, request.getContextPath() + request.getServletPath() + '/' + label);

            // deprecated request attributes
            req.setAttribute( ATTR_LABEL_MAP_OLD, labelMap );
            req.setAttribute( ATTR_APP_ROOT_OLD, request.getContextPath() + request.getServletPath() );

            // wrap the response for localization and template variable replacement
            response = wrapResponse( request, response, plugin );

            plugin.service( request, response );
        }
        else
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

    }

    public void destroy()
    {
        // base class destroy not needed, since the GenericServlet.destroy
        // is an empty method

        // dispose off the resource bundle manager
        if ( resourceBundleManager != null )
        {
            resourceBundleManager.dispose();
            resourceBundleManager = null;
        }

        // stop listening for plugins
        if ( operationsTracker != null )
        {
            operationsTracker.close();
            operationsTracker = null;
        }
        if ( pluginsTracker != null )
        {
            pluginsTracker.close();
            pluginsTracker = null;
        }
        if( brandingTracker != null )
        {
            brandingTracker.close();
            brandingTracker = null;
        }

        // deactivate any remaining plugins
        for ( Iterator pi = plugins.values().iterator(); pi.hasNext(); )
        {
            Object plugin = pi.next();
            if ( plugin instanceof OsgiManagerPlugin )
            {
                ( ( OsgiManagerPlugin ) plugin ).deactivate();
            }
        }

        // simply remove all operations, we should not be used anymore
        this.plugins.clear();
        this.labelMap.clear();
        this.operations.clear();
    }

    //---------- internal

    protected boolean handleAction( HttpServletRequest req, HttpServletResponse resp ) throws IOException, ServletException
    {
        // check action
        String actionName = WebConsoleUtil.getParameter( req, Util.PARAM_ACTION );
        if ( actionName != null )
        {
            Action action = ( Action ) this.operations.get( actionName );
            if ( action != null )
            {
                boolean redirect = true;
                try
                {
                    redirect = action.performAction( req, resp );
                }
                catch ( IOException ioe )
                {
                    log.log( LogService.LOG_WARNING, ioe.getMessage(), ioe );
                }
                catch ( ServletException se )
                {
                    log.log( LogService.LOG_WARNING, se.getMessage(), se.getRootCause() );
                }

                // maybe overwrite redirect
                if ( PARAM_NO_REDIRECT_AFTER_ACTION.equals( WebConsoleUtil.getParameter( req,
                    PARAM_NO_REDIRECT_AFTER_ACTION ) ) )
                {
                    resp.setStatus( HttpServletResponse.SC_OK );
                    resp.setContentType( "text/html" );
                    resp.getWriter().println( "Ok" );
                    return true;
                }

                if ( redirect )
                {
                    String uri = req.getRequestURI();
                    // Object pars =
                    // req.getAttribute(Action.ATTR_REDIRECT_PARAMETERS);
                    // if (pars instanceof String) {
                    // uri += "?" + pars;
                    // }
                    resp.sendRedirect( uri );
                }
                return true;
            }
        }

        return false;
    }


    BundleContext getBundleContext()
    {
        return bundleContext;
    }


    /**
     * Returns the Service PID used to retrieve configuration and to describe
     * the configuration properties.
     */
    String getConfigurationPid()
    {
        return getClass().getName();
    }


    private HttpServletResponse wrapResponse( final HttpServletRequest request, final HttpServletResponse response,
        final AbstractWebConsolePlugin plugin )
    {
        final Locale locale = request.getLocale();
        final ResourceBundle resourceBundle = resourceBundleManager.getResourceBundle( plugin.getBundle(), locale );
        return new FilteringResponseWrapper( response, resourceBundle, request );
    }

    private static class HttpServiceTracker extends ServiceTracker
    {

        private final OsgiManager osgiManager;


        HttpServiceTracker( OsgiManager osgiManager )
        {
            super( osgiManager.getBundleContext(), HttpService.class.getName(), null );
            this.osgiManager = osgiManager;
        }


        public Object addingService( ServiceReference reference )
        {
            Object service = super.addingService( reference );
            if ( service instanceof HttpService )
            {
                osgiManager.bindHttpService( ( HttpService ) service );
            }
            return service;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            if ( service instanceof HttpService )
            {
                osgiManager.unbindHttpService( ( HttpService ) service );
            }

            super.removedService( reference, service );
        }
    }

    private static class OperationServiceTracker extends ServiceTracker
    {

        private final OsgiManager osgiManager;


        OperationServiceTracker( OsgiManager osgiManager )
        {
            super( osgiManager.getBundleContext(), Action.SERVICE, null );
            this.osgiManager = osgiManager;
        }


        public Object addingService( ServiceReference reference )
        {
            Object operation = super.addingService( reference );
            if ( operation instanceof Action )
            {
                osgiManager.bindOperation( ( Action ) operation );
            }
            return operation;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            if ( service instanceof Action )
            {
                osgiManager.bindOperation( ( Action ) service );
            }

            super.removedService( reference, service );
        }
    }

    private static class PluginServiceTracker extends ServiceTracker
    {

        private final OsgiManager osgiManager;


        PluginServiceTracker( OsgiManager osgiManager )
        {
            super( osgiManager.getBundleContext(), WebConsoleConstants.SERVICE_NAME, null );
            this.osgiManager = osgiManager;
        }


        public Object addingService( ServiceReference reference )
        {
            Object label = reference.getProperty( WebConsoleConstants.PLUGIN_LABEL );
            if ( label instanceof String )
            {
                Object operation = super.addingService( reference );
                if ( operation instanceof Servlet )
                {
                    // wrap the servlet if it is not an AbstractWebConsolePlugin
                    // but has a title in the service properties
                    final AbstractWebConsolePlugin plugin;
                    if ( operation instanceof AbstractWebConsolePlugin )
                    {
                        plugin = ( AbstractWebConsolePlugin ) operation;
                    }
                    else
                    {

                        // define the title from the PLUGIN_TITLE registration
                        // property, the servlet name or the servlet "toString"
                        Object title = reference.getProperty( WebConsoleConstants.PLUGIN_TITLE );
                        if ( !( title instanceof String ) )
                        {
                            if ( operation instanceof GenericServlet )
                            {
                                title = ( ( GenericServlet ) operation ).getServletName();
                            }

                            if ( !( title instanceof String ) )
                            {
                                title = operation.toString();
                            }
                        }

                        plugin = new WebConsolePluginAdapter( ( String ) label, ( String ) title,
                            ( Servlet ) operation, reference );

                        // ensure the AbstractWebConsolePlugin is correctly setup
                        Bundle pluginBundle = reference.getBundle();
                        plugin.activate( pluginBundle.getBundleContext() );
                    }

                    osgiManager.bindServlet( plugin );
                }
                return operation;
            }

            return null;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            Object label = reference.getProperty( WebConsoleConstants.PLUGIN_LABEL );
            if ( label instanceof String )
            {
                // TODO: check reference properties !!
                osgiManager.unbindServlet( ( String ) label );

                // check whether the service is a WebConsolePluginAdapter in
                // which case we have to deactivate it here (as we activated it
                // while adding the service
                if ( service instanceof WebConsolePluginAdapter )
                {
                    ( ( WebConsolePluginAdapter ) service ).deactivate();
                }
            }

            super.removedService( reference, service );
        }
    }

    private static class BrandingServiceTracker extends ServiceTracker
    {
        private final OsgiManager osgiManager;

        BrandingServiceTracker( OsgiManager osgiManager ){
            super( osgiManager.getBundleContext(), BrandingPlugin.class.getName(), null );
            this.osgiManager = osgiManager;
        }

        public Object addingService( ServiceReference reference ){
            Object plugin = super.addingService( reference );
            if ( plugin instanceof BrandingPlugin )
            {
                AbstractWebConsolePlugin.setBrandingPlugin((BrandingPlugin) plugin);
            }
            return plugin;
        }

        public void removedService( ServiceReference reference, Object service ){
            if ( service instanceof BrandingPlugin )
            {
                AbstractWebConsolePlugin.setBrandingPlugin(null);
            }
            super.removedService( reference, service );
        }

    }


    protected synchronized void bindHttpService( HttpService httpService )
    {
        // do not bind service, when we are already bound
        if ( this.httpService != null )
        {
            log.log( LogService.LOG_DEBUG,
                "bindHttpService: Already bound to an HTTP Service, ignoring further services" );
            return;
        }

        Dictionary config = getConfiguration();

        // get authentication details
        String realm = this.getProperty( config, PROP_REALM, DEFAULT_REALM );
        String userId = this.getProperty( config, PROP_USER_NAME, DEFAULT_USER_NAME );
        String password = this.getProperty( config, PROP_PASSWORD, DEFAULT_PASSWORD );

        // register the servlet and resources
        try
        {
            HttpContext httpContext = new OsgiManagerHttpContext( httpService, realm, userId, password );

            Dictionary servletConfig = toStringConfig( config );

            // register this servlet and take note of this
            httpService.registerServlet( this.webManagerRoot, this, servletConfig, httpContext );
            httpServletRegistered = true;

            // register resources and take of this
            httpService.registerResources( this.webManagerRoot + "/res", "/res", httpContext );
            httpResourcesRegistered = true;

        }
        catch ( Exception e )
        {
            log.log( LogService.LOG_ERROR, "bindHttpService: Problem setting up", e );
        }

        this.httpService = httpService;
    }


    protected synchronized void unbindHttpService( HttpService httpService )
    {
        if ( this.httpService != httpService )
        {
            log.log( LogService.LOG_DEBUG,
                "unbindHttpService: Ignoring unbind of an HttpService to which we are not registered" );
            return;
        }

        // drop the service reference
        this.httpService = null;

        if ( httpResourcesRegistered )
        {
            try
            {
                httpService.unregister( this.webManagerRoot + "/res" );
            }
            catch ( Throwable t )
            {
                log.log( LogService.LOG_WARNING, "unbindHttpService: Failed unregistering Resources", t );
            }
            httpResourcesRegistered = false;
        }

        if ( httpServletRegistered )
        {
            try
            {
                httpService.unregister( this.webManagerRoot );
            }
            catch ( Throwable t )
            {
                log.log( LogService.LOG_WARNING, "unbindHttpService: Failed unregistering Servlet", t );
            }
            httpServletRegistered = false;
        }
    }


    private void bindServlet( final AbstractWebConsolePlugin plugin )
    {
        final String label = plugin.getLabel();
        final String title = plugin.getTitle();
        try
        {
            plugin.init( getServletConfig() );
            plugins.put( label, plugin );
            labelMap.put( label, title );

            if ( this.defaultPlugin == null )
            {
                this.defaultPlugin = plugin;
            }
            else if ( label.equals( this.defaultRenderName ) )
            {
                this.defaultPlugin = plugin;
            }
        }
        catch ( ServletException se )
        {
            log.log( LogService.LOG_WARNING, "Initialization of plugin '" + title + "' (" + label
                + ") failed; not using this plugin", se );
        }
    }


    private void unbindServlet( String label )
    {
        AbstractWebConsolePlugin plugin = ( AbstractWebConsolePlugin ) plugins.remove( label );
        if ( plugin != null )
        {
            labelMap.remove( label );

            if ( this.defaultPlugin == plugin )
            {
                if ( this.plugins.isEmpty() )
                {
                    this.defaultPlugin = null;
                }
                else
                {
                    this.defaultPlugin = ( AbstractWebConsolePlugin ) plugins.values().iterator().next();
                }
            }

            plugin.destroy();
        }
    }


    protected void bindOperation( Action operation )
    {
        operations.put( operation.getName(), operation );
    }


    protected void unbindOperation( Action operation )
    {
        operations.remove( operation.getName() );
    }


    private Dictionary getConfiguration()
    {
        return configuration;
    }


    synchronized void updateConfiguration( Dictionary config )
    {
        if ( config == null )
        {
            config = new Hashtable();
        }

        configuration = config;

        defaultRenderName = getProperty( config, PROP_DEFAULT_RENDER, DEFAULT_PAGE );
        if ( defaultRenderName != null && plugins.get( defaultRenderName ) != null )
        {
            defaultPlugin = ( AbstractWebConsolePlugin ) plugins.get( defaultRenderName );
        }

        // get the web manager root path
        String newWebManagerRoot = this.getProperty( config, PROP_MANAGER_ROOT, DEFAULT_MANAGER_ROOT );
        if ( !newWebManagerRoot.startsWith( "/" ) )
        {
            newWebManagerRoot = "/" + newWebManagerRoot;
        }

        // get enabled plugins
        Object pluginValue = config.get( PROP_ENABLED_PLUGINS );
        if ( pluginValue == null )
        {
            enabledPlugins = null;
        }
        else if ( pluginValue.getClass().isArray() )
        {
            final Object[] names = ( Object[] ) pluginValue;
            enabledPlugins = new HashSet();
            for ( int i = 0; i < names.length; i++ )
            {
                enabledPlugins.add( String.valueOf( names[i] ) );
            }
        }
        else if ( pluginValue instanceof Collection )
        {
            enabledPlugins = new HashSet();
            enabledPlugins.addAll( ( Collection ) pluginValue );
        }

        // might update http service registration
        if ( !newWebManagerRoot.equals( this.webManagerRoot ) )
        {
            HttpService httpService = this.httpService;
            if ( httpService != null )
            {
                synchronized ( this )
                {
                    // unbind old location first
                    unbindHttpService( httpService );

                    // switch location
                    this.webManagerRoot = newWebManagerRoot;

                    // bind new location now
                    bindHttpService( httpService );
                }
            }
            else
            {
                // just set the configured location (FELIX-2034)
                this.webManagerRoot = newWebManagerRoot;
            }
        }
    }


    /**
     * Returns the named property from the configuration. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    private String getProperty( Dictionary config, String name, String def )
    {
        Object value = config.get( name );
        if ( value instanceof String )
        {
            return ( String ) value;
        }

        if ( value == null )
        {
            return def;
        }

        return String.valueOf( value );
    }


    /**
     * Returns <code>true</code> if the plugin is an
     * {@link AbstractWebConsolePlugin} and a list of enabled plugins is
     * configured but the plugin is not contained in that list.
     * <p>
     * This method is intended to be used only for plugins contained in the
     * web console bundle itself, namely plugins listed in the
     * {@value #PLUGIN_CLASSES} list.
     */
    private boolean isPluginDisabled( String pluginClass, Object plugin )
    {
        return enabledPlugins != null && !enabledPlugins.contains( pluginClass )
            && ( plugin instanceof AbstractWebConsolePlugin );
    }


    private Dictionary toStringConfig( Dictionary config )
    {
        Dictionary stringConfig = new Hashtable();
        for ( Enumeration ke = config.keys(); ke.hasMoreElements(); )
        {
            Object key = ke.nextElement();
            stringConfig.put( key.toString(), String.valueOf( config.get( key ) ) );
        }
        return stringConfig;
    }
}
