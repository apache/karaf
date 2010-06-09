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
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.BrandingPlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleSecurityProvider;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.core.BundlesServlet;
import org.apache.felix.webconsole.internal.filter.FilteringResponseWrapper;
import org.apache.felix.webconsole.internal.i18n.ResourceBundleManager;
import org.apache.felix.webconsole.internal.misc.ConfigurationRender;
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
     * Old name of the request attribute providing the root to the web console.
     * This attribute is no deprecated and replaced by
     * {@link WebConsoleConstants#ATTR_APP_ROOT}.
     *
     * @deprecated use {@link WebConsoleConstants#ATTR_APP_ROOT} instead
     */
    private static final String ATTR_APP_ROOT_OLD = OsgiManager.class.getName() + ".appRoot";

    /**
     * Old name of the request attribute providing the mappings from label to
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

    /**
     * The name of the cookie storing user-configured locale
     * See https://issues.apache.org/jira/browse/FELIX-2267
     */
    private static final String COOKIE_LOCALE = "felix.webconsole.locale";


    static final String PROP_MANAGER_ROOT = "manager.root";

    static final String PROP_DEFAULT_RENDER = "default.render";

    static final String PROP_REALM = "realm";

    static final String PROP_USER_NAME = "username";

    static final String PROP_PASSWORD = "password";

    static final String PROP_ENABLED_PLUGINS = "plugins";

    static final String PROP_LOG_LEVEL = "loglevel";

    static final String PROP_LOCALE = "locale";

    public static final int DEFAULT_LOG_LEVEL = LogService.LOG_WARNING;

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
        { "org.apache.felix.webconsole.internal.compendium.ComponentConfigurationPrinter",
            "org.apache.felix.webconsole.internal.compendium.ComponentsServlet",
            "org.apache.felix.webconsole.internal.compendium.ConfigManager",
            "org.apache.felix.webconsole.internal.compendium.ConfigurationAdminConfigurationPrinter",
            "org.apache.felix.webconsole.internal.compendium.LogServlet",
            "org.apache.felix.webconsole.internal.compendium.PreferencesConfigurationPrinter",
            "org.apache.felix.webconsole.internal.compendium.WireAdminConfigurationPrinter",
            "org.apache.felix.webconsole.internal.core.BundlesServlet",
            "org.apache.felix.webconsole.internal.core.PermissionsConfigurationPrinter",
            "org.apache.felix.webconsole.internal.core.ServicesConfigurationPrinter",
            "org.apache.felix.webconsole.internal.core.ServicesServlet",
            "org.apache.felix.webconsole.internal.deppack.DepPackServlet",
            "org.apache.felix.webconsole.internal.misc.LicenseServlet",
            "org.apache.felix.webconsole.internal.misc.ShellServlet",
            "org.apache.felix.webconsole.internal.misc.SystemPropertiesPrinter",
            "org.apache.felix.webconsole.internal.misc.ThreadPrinter",
            "org.apache.felix.webconsole.internal.obr.BundleRepositoryRender",
            "org.apache.felix.webconsole.internal.system.VMStatPlugin" };

    private BundleContext bundleContext;

    private ServiceTracker httpServiceTracker;

    private HttpService httpService;

    private PluginHolder holder;

    private ServiceTracker brandingTracker;

    private ServiceTracker securityProviderTracker;

    private ServiceRegistration configurationListener;

    // list of OsgiManagerPlugin instances activated during init. All these
    // instances will have to be deactivated during destroy
    private List osgiManagerPlugins = new ArrayList();

    private String webManagerRoot;

    // true if the OsgiManager is registered as a Servlet with the HttpService
    private boolean httpServletRegistered;

    // true if the resources have been registered with the HttpService
    private boolean httpResourcesRegistered;

    private Dictionary configuration;

    // See https://issues.apache.org/jira/browse/FELIX-2267
    private Locale configuredLocale;

    private Set enabledPlugins;

    ResourceBundleManager resourceBundleManager;

    private int logLevel = DEFAULT_LOG_LEVEL;


    public OsgiManager( BundleContext bundleContext )
    {
        this.bundleContext = bundleContext;
        this.holder = new PluginHolder( bundleContext );

        securityProviderTracker = new ServiceTracker( bundleContext, WebConsoleSecurityProvider.class.getName(), null );
        securityProviderTracker.open();

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

        this.bundleContext = null;
    }


    //---------- Servlet API

    /**
     * @see javax.servlet.GenericServlet#init()
     */
    public void init()
    {
        // base class initialization not needed, since the GenericServlet.init
        // is an empty method

        holder.setServletContext( getServletContext() );

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
                    log( LogService.LOG_INFO, "Ignoring plugin " + pluginClassName + ": Disabled by configuration" );
                    continue;
                }

                if ( plugin instanceof OsgiManagerPlugin )
                {
                    ( ( OsgiManagerPlugin ) plugin ).activate( bundleContext );
                    osgiManagerPlugins.add( plugin );
                }
                if ( plugin instanceof AbstractWebConsolePlugin )
                {
                    holder.addOsgiManagerPlugin( ( AbstractWebConsolePlugin ) plugin );
                }
                else if ( plugin instanceof BrandingPlugin )
                {
                    AbstractWebConsolePlugin.setBrandingPlugin( ( BrandingPlugin ) plugin );
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
                log( LogService.LOG_INFO, pluginClassName + " not enabled. Reason: " + message );
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_INFO, "Failed to instantiate plugin " + pluginClassName + ". Reason: " + t );
            }
        }

        // the resource bundle manager
        resourceBundleManager = new ResourceBundleManager( getBundleContext() );

        // start the configuration render, providing the resource bundle manager
        ConfigurationRender cr = new ConfigurationRender( resourceBundleManager );
        cr.activate( bundleContext );
        osgiManagerPlugins.add( cr );
        holder.addOsgiManagerPlugin( cr );

        // start tracking external plugins after setting up our own plugins
        holder.open();

        // accept new console branding service
        brandingTracker = new BrandingServiceTracker( this );
        brandingTracker.open();
    }


    /**
     * @see javax.servlet.GenericServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void service( final ServletRequest req, final ServletResponse res ) throws ServletException, IOException
    {
        // don't really expect to be called within a non-HTTP environment
        service( ( HttpServletRequest ) req, ( HttpServletResponse ) res );

        // ensure response has been sent back and response is committed
        // (we are authorative for our URL space and no other servlet should interfere)
        res.flushBuffer();
    }


    private void service( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        // check whether we are not at .../{webManagerRoot}
        final String pathInfo = request.getPathInfo();
        if ( pathInfo == null || pathInfo.equals( "/" ) )
        {
            String path = request.getRequestURI();
            if ( !path.endsWith( "/" ) )
            {
                path = path.concat( "/" );
            }
            path = path.concat( holder.getDefaultPluginLabel() );
            response.sendRedirect( path );
            return;
        }

        int slash = pathInfo.indexOf( "/", 1 );
        if ( slash < 2 )
        {
            slash = pathInfo.length();
        }

        final Locale locale = getConfiguredLocale( request );
        final String label = pathInfo.substring( 1, slash );
        AbstractWebConsolePlugin plugin = holder.getPlugin( label );

        if ( plugin == null )
        {
            if ( "install".equals( label ) )
            {
                plugin = holder.getPlugin( BundlesServlet.NAME );
            }
        }

        if ( plugin != null )
        {
            final Map labelMap = holder.getLocalizedLabelMap( resourceBundleManager, locale );

            // the official request attributes
            request.setAttribute( WebConsoleConstants.ATTR_LABEL_MAP, labelMap );
            request.setAttribute( WebConsoleConstants.ATTR_APP_ROOT, request.getContextPath() + request.getServletPath() );
            request.setAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT, request.getContextPath() + request.getServletPath()
                + '/' + label );

            // deprecated request attributes
            request.setAttribute( ATTR_LABEL_MAP_OLD, labelMap );
            request.setAttribute( ATTR_APP_ROOT_OLD, request.getContextPath() + request.getServletPath() );

            // wrap the response for localization and template variable replacement
            request = wrapRequest( request, locale );
            response = wrapResponse( request, response, plugin );

            plugin.service( request, response );
        }
        else
        {
            final String body404 = MessageFormat.format(
                resourceBundleManager.getResourceBundle(bundleContext.getBundle(), locale).getString(
                    "404"), new Object[] {
                    request.getContextPath() + request.getServletPath() + '/' + BundlesServlet.NAME
                });
            response.setCharacterEncoding( "utf-8" );
            response.setContentType( "text/html" );
            response.setStatus( HttpServletResponse.SC_NOT_FOUND );
            response.getWriter().println( body404 );
        }
    }

    // See https://issues.apache.org/jira/browse/FELIX-2267
    private final Locale getConfiguredLocale(HttpServletRequest request)
    {
        Locale locale = null;

        Cookie[] cookies = request.getCookies();
        for (int i = 0; cookies != null && i < cookies.length; i++)
        {
            if (COOKIE_LOCALE.equals(cookies[i].getName()))
            {
                locale = Util.parseLocaleString(cookies[i].getValue());
                break;
            }
        }

        // TODO: check UserAdmin ?

        if (locale == null)
            locale = configuredLocale;
        if (locale == null)
            locale = request.getLocale();

        return locale;
    }

    /**
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy()
    {
        // base class destroy not needed, since the GenericServlet.destroy
        // is an empty method

        // dispose off held plugins
        holder.close();

        // dispose off the resource bundle manager
        if ( resourceBundleManager != null )
        {
            resourceBundleManager.dispose();
            resourceBundleManager = null;
        }

        // stop listening for brandings
        if ( brandingTracker != null )
        {
            brandingTracker.close();
            brandingTracker = null;
        }

        // deactivate any remaining plugins
        for ( Iterator pi = osgiManagerPlugins.iterator(); pi.hasNext(); )
        {
            Object plugin = pi.next();
            ( ( OsgiManagerPlugin ) plugin ).deactivate();
        }

        // simply remove all operations, we should not be used anymore
        this.osgiManagerPlugins.clear();
    }


    //---------- internal

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


    /**
     * Calls the <code>GenericServlet.log(String)</code> method if the
     * configured log level is less than or equal to the given <code>level</code>.
     * <p>
     * Note, that the <code>level</code> parameter is only used to decide whether
     * the <code>GenericServlet.log(String)</code> method is called or not. The
     * actual implementation of the <code>GenericServlet.log</code> method is
     * outside of the control of this method.
     *
     * @param level The log level at which to log the message
     * @param message The message to log
     */
    private void log( int level, String message )
    {
        if ( logLevel >= level )
        {
            log( message );
        }
    }


    /**
     * Calls the <code>GenericServlet.log(String, Throwable)</code> method if
     * the configured log level is less than or equal to the given
     * <code>level</code>.
     * <p>
     * Note, that the <code>level</code> parameter is only used to decide whether
     * the <code>GenericServlet.log(String, Throwable)</code> method is called
     * or not. The actual implementation of the <code>GenericServlet.log</code>
     * method is outside of the control of this method.
     *
     * @param level The log level at which to log the message
     * @param message The message to log
     * @param t The <code>Throwable</code> to log with the message
     */
    private void log( int level, String message, Throwable t )
    {
        if ( logLevel >= level )
        {
            log( message, t );
        }
    }

    private HttpServletRequest wrapRequest( final HttpServletRequest request, final Locale locale ) {
        return new HttpServletRequestWrapper( request ) {
            /**
             * @see javax.servlet.ServletRequestWrapper#getLocale()
             */
            public Locale getLocale()
            {
                return locale;
            }
        };
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

    private static class BrandingServiceTracker extends ServiceTracker
    {
        private final OsgiManager osgiManager;


        BrandingServiceTracker( OsgiManager osgiManager )
        {
            super( osgiManager.getBundleContext(), BrandingPlugin.class.getName(), null );
            this.osgiManager = osgiManager;
        }


        public Object addingService( ServiceReference reference )
        {
            Object plugin = super.addingService( reference );
            if ( plugin instanceof BrandingPlugin )
            {
                AbstractWebConsolePlugin.setBrandingPlugin( ( BrandingPlugin ) plugin );
            }
            return plugin;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            if ( service instanceof BrandingPlugin )
            {
                AbstractWebConsolePlugin.setBrandingPlugin( null );
            }
            super.removedService( reference, service );
        }

    }

    protected synchronized void bindHttpService( HttpService httpService )
    {
        // do not bind service, when we are already bound
        if ( this.httpService != null )
        {
            log( LogService.LOG_DEBUG, "bindHttpService: Already bound to an HTTP Service, ignoring further services" );
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
            HttpContext httpContext = new OsgiManagerHttpContext( httpService, realm, new SecurityProvider( securityProviderTracker, userId, password ) );

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
            log( LogService.LOG_ERROR, "bindHttpService: Problem setting up", e );
        }

        this.httpService = httpService;
    }


    protected synchronized void unbindHttpService( HttpService httpService )
    {
        if ( this.httpService != httpService )
        {
            log( LogService.LOG_DEBUG,
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
                log( LogService.LOG_WARNING, "unbindHttpService: Failed unregistering Resources", t );
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
                log( LogService.LOG_WARNING, "unbindHttpService: Failed unregistering Servlet", t );
            }
            httpServletRegistered = false;
        }
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

        final Object locale = config.get( PROP_LOCALE );
        configuredLocale = locale == null || locale.toString().trim().length() == 0 //
            ? null : Util.parseLocaleString( locale.toString().trim() );

        logLevel = getProperty( config, PROP_LOG_LEVEL, DEFAULT_LOG_LEVEL );
        AbstractWebConsolePlugin.setLogLevel( logLevel );

        // default plugin page configuration
        holder.setDefaultPluginLabel( getProperty( config, PROP_DEFAULT_RENDER, DEFAULT_PAGE ) );

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
        HttpService httpService = this.httpService;
        if ( httpService != null )
        {
            // unbind old location first
            unbindHttpService( httpService );

            // switch location
            this.webManagerRoot = newWebManagerRoot;

            // bind new location now
            bindHttpService( httpService );
        }
        else
        {
            // just set the configured location (FELIX-2034)
            this.webManagerRoot = newWebManagerRoot;
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
     * Returns the named property from the configuration. If the property does
     * not exist, the default value <code>def</code> is returned.
     *
     * @param config The properties from which to returned the named one
     * @param name The name of the property to return
     * @param def The default value if the named property does not exist
     * @return The value of the named property as a string or <code>def</code>
     *         if the property does not exist
     */
    private int getProperty( Dictionary config, String name, int def )
    {
        Object value = config.get( name );
        if ( value instanceof Number )
        {
            return ( ( Number ) value ).intValue();
        }

        // try to convert the value to a number
        if ( value != null )
        {
            try
            {
                return Integer.parseInt( value.toString() );
            }
            catch ( NumberFormatException nfe )
            {
                // don't care
            }
        }

        // not a number, not convertible, not set, use default
        return def;
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

    static class SecurityProvider implements WebConsoleSecurityProvider {

        final ServiceTracker tracker;
        final String username;
        final String password;

        SecurityProvider( ServiceTracker tracker, String username, String password ) {
            this.tracker = tracker;
            this.username = username;
            this.password = password;
        }

        public Object authenticate(String username, String password) {
            WebConsoleSecurityProvider provider = (WebConsoleSecurityProvider) tracker.getService();
            if (provider != null) {
                return provider.authenticate(username, password);
            }
            if (this.username.equals(username) && this.password.equals(password)) {
                return username;
            }
            return null;
        }

        public boolean authorize(Object user, String role) {
            // no op: authorize everything
            return true;
        }
    }

}
