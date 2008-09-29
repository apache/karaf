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
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.*;
import org.apache.felix.webconsole.internal.*;
import org.apache.felix.webconsole.internal.compendium.*;
import org.apache.felix.webconsole.internal.core.*;
import org.apache.felix.webconsole.internal.misc.*;
import org.apache.felix.webconsole.internal.obr.BundleRepositoryRender;
import org.apache.felix.webconsole.internal.obr.InstallFromRepoAction;
import org.apache.felix.webconsole.internal.obr.RefreshRepoAction;
import org.apache.felix.webconsole.internal.system.*;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>OSGi Manager</code> TODO
 *
 * @scr.component ds="no" label="%manager.name"
 *                description="%manager.description"
 */
public class OsgiManager extends GenericServlet
{

    /** Pseudo class version ID to keep the IDE quite. */
    private static final long serialVersionUID = 1L;

    public static final String ATTR_LABEL_MAP = OsgiManager.class.getName() + ".labelMap";

    public static final String ATTR_APP_ROOT = OsgiManager.class.getName() + ".appRoot";

    /**
     * The name and value of a parameter which will prevent redirection to a
     * render after the action has been executed (value is "_noredir_"). This
     * may be used by programmatic action submissions.
     */
    public static final String PARAM_NO_REDIRECT_AFTER_ACTION = "_noredir_";

    /**
     * @scr.property valueRef="DEFAULT_MANAGER_ROOT"
     */
    private static final String PROP_MANAGER_ROOT = "manager.root";

    /**
     * @scr.property valueRef="DEFAULT_PAGE"
     */
    private static final String PROP_DEFAULT_RENDER = "default.render";

    /**
     * @scr.property valueRef="DEFAULT_REALM"
     */
    private static final String PROP_REALM = "realm";

    /**
     * @scr.property valueRef="DEFAULT_USER_NAME"
     */
    private static final String PROP_USER_NAME = "username";

    /**
     * @scr.property valueRef="DEFAULT_PASSWORD"
     */
    private static final String PROP_PASSWORD = "password";

    private static final String DEFAULT_PAGE = BundlesServlet.NAME;

    private static final String DEFAULT_REALM = "OSGi Management Console";

    private static final String DEFAULT_USER_NAME = "admin";

    private static final String DEFAULT_PASSWORD = "admin";

    /**
     * The default value for the {@link #PROP_MANAGER_ROOT} configuration
     * property (value is "/system/console").
     */
    private static final String DEFAULT_MANAGER_ROOT = "/system/console";

    private static final Class[] PLUGIN_CLASSES =
        { ComponentConfigurationPrinter.class, ComponentsServlet.class, ConfigManager.class, BundlesServlet.class,
            InstallAction.class, SetStartLevelAction.class, ConfigurationRender.class, GCAction.class,
            ShutdownAction.class, ShutdownRender.class, VMStatRender.class, BundleRepositoryRender.class,
            LicenseServlet.class, RefreshRepoAction.class, InstallFromRepoAction.class, ShellServlet.class };

    private BundleContext bundleContext;

    private Logger log;

    private ServiceTracker httpServiceTracker;

    private HttpService httpService;

    private ServiceTracker operationsTracker;

    private ServiceTracker rendersTracker;

    private ServiceTracker pluginsTracker;

    private ServiceRegistration configurationListener;

    private Map plugins = new HashMap();

    private Map labelMap = new HashMap();

    private Map operations = new HashMap();

    private Servlet defaultRender;

    private String defaultRenderName;

    private String webManagerRoot;

    private Dictionary configuration;


    public OsgiManager( BundleContext bundleContext )
    {

        this.bundleContext = bundleContext;
        this.log = new Logger( bundleContext );

        updateConfiguration( null );

        try
        {
            this.configurationListener = ConfigurationListener.create( this );
        }
        catch ( Throwable t )
        {
            // might be caused by CM not available
        }

        // track renders and operations
        operationsTracker = new OperationServiceTracker( this );
        operationsTracker.open();
        rendersTracker = new RenderServiceTracker( this );
        rendersTracker.open();
        pluginsTracker = new PluginServiceTracker( this );
        pluginsTracker.open();
        httpServiceTracker = new HttpServiceTracker( this );
        httpServiceTracker.open();

        for ( int i = 0; i < PLUGIN_CLASSES.length; i++ )
        {
            Class pluginClass = PLUGIN_CLASSES[i];
            try
            {
                Object plugin = pluginClass.newInstance();
                if ( plugin instanceof OsgiManagerPlugin )
                {
                    ( ( OsgiManagerPlugin ) plugin ).activate( bundleContext );
                }
                if ( plugin instanceof AbstractWebConsolePlugin )
                {
                    AbstractWebConsolePlugin amp = ( AbstractWebConsolePlugin ) plugin;
                    bindServlet( amp.getLabel(), amp );
                }
                else
                {
                    if ( plugin instanceof Action )
                    {
                        bindOperation( ( Action ) plugin );
                    }
                    if ( plugin instanceof Render )
                    {
                        bindRender( ( Render ) plugin );
                    }
                }
            }
            catch ( Throwable t )
            {
                // todo: log
            }
        }
    }


    public void dispose()
    {

        if ( configurationListener != null )
        {
            configurationListener.unregister();
            configurationListener = null;
        }

        if ( operationsTracker != null )
        {
            operationsTracker.close();
            operationsTracker = null;
        }

        if ( rendersTracker != null )
        {
            rendersTracker.close();
            rendersTracker = null;
        }

        if ( pluginsTracker != null )
        {
            pluginsTracker.close();
            pluginsTracker = null;
        }

        if ( httpServiceTracker != null )
        {
            httpServiceTracker.close();
            httpServiceTracker = null;
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
        this.defaultRender = null;
        this.plugins.clear();
        this.labelMap.clear();
        this.operations.clear();

        if ( log != null )
        {
            log.dispose();
        }

        this.bundleContext = null;
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
        Servlet plugin = ( Servlet ) plugins.get( label );
        if ( plugin != null )
        {
            req.setAttribute( ATTR_LABEL_MAP, labelMap );
            req.setAttribute( ATTR_APP_ROOT, request.getContextPath() + request.getServletPath() );

            plugin.service( req, res );
        }
        else
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

    }


    protected boolean handleAction( HttpServletRequest req, HttpServletResponse resp ) throws IOException
    {
        // check action
        String actionName = AbstractWebConsolePlugin.getParameter( req, Util.PARAM_ACTION );
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
                    this.log( ioe.getMessage(), ioe );
                }
                catch ( ServletException se )
                {
                    this.log( se.getMessage(), se.getRootCause() );
                }

                // maybe overwrite redirect
                if ( PARAM_NO_REDIRECT_AFTER_ACTION.equals( AbstractWebConsolePlugin.getParameter( req,
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
            Object operation = super.addingService( reference );
            if ( operation instanceof HttpService )
            {
                osgiManager.bindHttpService( ( HttpService ) operation );
            }
            return operation;
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

    private static class RenderServiceTracker extends ServiceTracker
    {

        private final OsgiManager osgiManager;


        RenderServiceTracker( OsgiManager osgiManager )
        {
            super( osgiManager.getBundleContext(), Render.SERVICE, null );
            this.osgiManager = osgiManager;
        }


        public Object addingService( ServiceReference reference )
        {
            Object operation = super.addingService( reference );
            if ( operation instanceof Render )
            {
                osgiManager.bindRender( ( Render ) operation );
            }
            return operation;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            if ( service instanceof Render )
            {
                osgiManager.bindRender( ( Render ) service );
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
                    // TODO: check reference properties !!
                    osgiManager.bindServlet( ( String ) label, ( Servlet ) operation );
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
            }

            super.removedService( reference, service );
        }
    }


    protected synchronized void bindHttpService( HttpService httpService )
    {
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

            httpService.registerServlet( this.webManagerRoot, this, servletConfig, httpContext );
            httpService.registerResources( this.webManagerRoot + "/res", "/res", httpContext );

        }
        catch ( Exception e )
        {
            log.log( LogService.LOG_ERROR, "Problem setting up", e );
        }

        this.httpService = httpService;
    }


    protected synchronized void unbindHttpService( HttpService httpService )
    {
        httpService.unregister( this.webManagerRoot + "/res" );
        httpService.unregister( this.webManagerRoot );

        if ( this.httpService == httpService )
        {
            this.httpService = null;
        }
    }


    protected void bindServlet( String label, Servlet servlet )
    {
        try
        {
            servlet.init( getServletConfig() );
            plugins.put( label, servlet );

            if ( servlet instanceof GenericServlet )
            {
                String title = ( ( GenericServlet ) servlet ).getServletName();
                if ( title != null )
                {
                    labelMap.put( label, title );
                }
            }

            if ( this.defaultRender == null )
            {
                this.defaultRender = servlet;
            }
            else if ( label.equals( this.defaultRenderName ) )
            {
                this.defaultRender = servlet;
            }
        }
        catch ( ServletException se )
        {
            // TODO: log
        }
    }


    protected void unbindServlet( String label )
    {
        Servlet servlet = ( Servlet ) plugins.remove( label );
        if ( servlet != null )
        {
            labelMap.remove( label );

            if ( this.defaultRender == servlet )
            {
                if ( this.plugins.isEmpty() )
                {
                    this.defaultRender = null;
                }
                else
                {
                    this.defaultRender = ( Servlet ) plugins.values().iterator().next();
                }
            }

            servlet.destroy();
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


    protected void bindRender( Render render )
    {
        RenderBridge bridge = new RenderBridge( render );
        bridge.activate( getBundleContext() );
        bindServlet( render.getName(), bridge );
    }


    protected void unbindRender( Render render )
    {
        unbindServlet( render.getName() );
    }


    private Dictionary getConfiguration()
    {
        return configuration;
    }


    void updateConfiguration( Dictionary config )
    {
        if ( config == null )
        {
            config = new Hashtable();
        }

        configuration = config;

        defaultRenderName = getProperty( config, PROP_DEFAULT_RENDER, DEFAULT_PAGE );
        if ( defaultRenderName != null && plugins.get( defaultRenderName ) != null )
        {
            defaultRender = ( Servlet ) plugins.get( defaultRenderName );
        }

        // get the web manager root path
        webManagerRoot = this.getProperty( config, PROP_MANAGER_ROOT, DEFAULT_MANAGER_ROOT );
        if ( !webManagerRoot.startsWith( "/" ) )
        {
            webManagerRoot = "/" + webManagerRoot;
        }

        // might update http service registration
        HttpService httpService = this.httpService;
        if ( httpService != null )
        {
            synchronized ( this )
            {
                unbindHttpService( httpService );
                bindHttpService( httpService );
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
