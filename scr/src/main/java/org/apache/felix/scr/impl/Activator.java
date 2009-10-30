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
package org.apache.felix.scr.impl;


import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.impl.config.ConfigurationComponentRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * This activator is used to cover requirement described in section 112.8.1 @@ -27,14
 * 37,202 @@ in active bundles.
 *
 */
public class Activator implements BundleActivator, SynchronousBundleListener
{
    //  name of the LogService class (this is a string to not create a reference to the class)
    static final String LOGSERVICE_CLASS = "org.osgi.service.log.LogService";

    // Flag that sets error messages
    private static int m_logLevel = LogService.LOG_ERROR;

    // this bundle's context
    private BundleContext m_context;

    // the log service to log messages to
    private static ServiceTracker m_logService;

    // map of BundleComponentActivator instances per Bundle indexed by Bundle id
    private Map m_componentBundles;

    // registry of managed component
    private ComponentRegistry m_componentRegistry;

    //  thread acting upon configurations
    private ComponentActorThread m_componentActor;


    /**
     * Registers this instance as a (synchronous) bundle listener and loads the
     * components of already registered bundles.
     *
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    public void start( BundleContext context ) throws Exception
    {
        m_context = context;
        m_componentBundles = new HashMap();
        m_componentRegistry = createComponentRegistry( context);

        // require the log service
        m_logService = new ServiceTracker( context, LOGSERVICE_CLASS, null );
        m_logService.open();

        // configure logging from context properties
        m_logLevel = getLogLevel( context );
        if ( "true".equalsIgnoreCase( context.getProperty( "ds.showversion" ) ) )
        {
            log( LogService.LOG_INFO, context.getBundle(), " Version = "
                + context.getBundle().getHeaders().get( Constants.BUNDLE_VERSION ), null );
        }

        // create and start the component actor
        m_componentActor = new ComponentActorThread();
        Thread t = new Thread(m_componentActor, "SCR Component Actor");
        t.setDaemon( true );
        t.start();

        // register for bundle updates
        context.addBundleListener( this );

        // 112.8.2 load all components of active bundles
        loadAllComponents( context );

        // We dynamically import the impl service API, so it
        // might not actually be available, so be ready to catch
        // the exception when we try to register the command service.
        try
        {
            // Register "scr" impl command service as a
            // wrapper for the bundle repository service.
            context.registerService( org.apache.felix.shell.Command.class.getName(), new ScrCommand( m_context,
                m_componentRegistry ), null );
        }
        catch ( Throwable th )
        {
            // Ignore.
        }
    }


    /**
     * Unregisters this instance as a bundle listener and unloads all components
     * which have been registered during the active life time of the SCR
     * implementation bundle.
     *
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    public void stop( BundleContext context ) throws Exception
    {
        // unregister as bundle listener
        context.removeBundleListener( this );

        // 112.8.2 dispose off all active components
        disposeAllComponents();

        // dispose component registry
        m_componentRegistry.dispose();

        // terminate the actor thread
        if ( m_componentActor != null )
        {
            m_componentActor.terminate();
            m_componentActor = null;
        }
    }


    // ---------- BundleListener Interface -------------------------------------

    /**
     * Loads and unloads any components provided by the bundle whose state
     * changed. If the bundle has been started, the components are loaded. If
     * the bundle is about to stop, the components are unloaded.
     *
     * @param event The <code>BundleEvent</code> representing the bundle state
     *      change.
     */
    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.LAZY_ACTIVATION || event.getType() == BundleEvent.STARTED )
        {
            // FELIX-1666 LAZY_ACTIVATION event is sent if the bundle has lazy
            // activation policy and is waiting for class loader access to
            // actually load it; STARTED event is sent if bundle has regular
            // activation policy or if the lazily activated bundle finally is
            // really started. In both cases just try to load the components
            loadComponents( event.getBundle() );
        }
        else if ( event.getType() == BundleEvent.STOPPING )
        {
            disposeComponents( event.getBundle() );
        }
    }


    //---------- Component Management -----------------------------------------

    // Loads the components of all bundles currently active.
    private void loadAllComponents( BundleContext context )
    {
        Bundle[] bundles = context.getBundles();
        for ( int i = 0; i < bundles.length; i++ )
        {
            Bundle bundle = bundles[i];
            if ( ComponentRegistry.isBundleActive( bundle ) )
            {
                loadComponents( bundle );
            }
        }
    }


    /**
     * Loads the components of the given bundle. If the bundle has no
     * <i>Service-Component</i> header, this method has no effect. The
     * fragments of a bundle are not checked for the header (112.4.1).
     * <p>
     * This method calls the {@link #getBundleContext(Bundle)} method to find
     * the <code>BundleContext</code> of the bundle. If the context cannot be
     * found, this method does not load components for the bundle.
     */
    private void loadComponents( Bundle bundle )
    {
        if ( bundle.getHeaders().get( "Service-Component" ) == null )
        {
            // no components in the bundle, abandon
            return;
        }

        // FELIX-1666 method is called for the LAZY_ACTIVATION event and
        // the started event. Both events cause this method to be called;
        // so we have to make sure to not load components twice
        if ( m_componentBundles.containsKey( new Long( bundle.getBundleId() ) ) )
        {
            log( LogService.LOG_DEBUG, m_context.getBundle(), "Components for bundle  " + bundle.getSymbolicName()
                + "/" + bundle.getBundleId() + " already loaded. Nothing to do.", null );
            return;
        }

        // there should be components, load them with a bundle context
        BundleContext context = bundle.getBundleContext();
        if ( context == null )
        {
            log( LogService.LOG_ERROR, m_context.getBundle(), "Cannot get BundleContext of bundle "
                + bundle.getSymbolicName() + "/" + bundle.getBundleId(), null );
            return;
        }

        try
        {
            BundleComponentActivator ga = new BundleComponentActivator( m_componentRegistry, m_componentActor, context,
                m_logLevel );
            m_componentBundles.put( new Long( bundle.getBundleId() ), ga );
        }
        catch ( Exception e )
        {
            if ( e instanceof IllegalStateException && bundle.getState() != Bundle.ACTIVE )
            {
                log(
                    LogService.LOG_INFO,
                    m_context.getBundle(),
                    "Bundle "
                        + bundle.getSymbolicName()
                        + "/"
                        + bundle.getBundleId()
                        + " has been stopped while trying to activate its components. Trying again when the bundles gets startet again.",
                    e );
            }
            else
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while loading components of bundle "
                    + bundle.getSymbolicName() + "/" + bundle.getBundleId(), e );
            }
        }
    }


    /**
     * Unloads components of the given bundle. If no components have been loaded
     * for the bundle, this method has no effect.
     */
    private void disposeComponents( Bundle bundle )
    {
        BundleComponentActivator ga = ( BundleComponentActivator ) m_componentBundles.remove( new Long( bundle
            .getBundleId() ) );
        if ( ga != null )
        {
            try
            {
                ga.dispose( ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while disposing components of bundle "
                    + bundle.getSymbolicName() + "/" + bundle.getBundleId(), e );
            }
        }
    }


    // Unloads all components registered with the SCR
    private void disposeAllComponents()
    {
        for ( Iterator it = m_componentBundles.values().iterator(); it.hasNext(); )
        {
            BundleComponentActivator ga = ( BundleComponentActivator ) it.next();
            try
            {
                ga.dispose( ComponentConstants.DEACTIVATION_REASON_DISPOSED );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, m_context.getBundle(), "Error while disposing components of bundle "
                    + ga.getBundleContext().getBundle().getSymbolicName(), e );
            }
            it.remove();
        }
    }


    public static ComponentRegistry createComponentRegistry( BundleContext bundleContext )
    {
        try
        {
            return new ConfigurationComponentRegistry( bundleContext );
        }
        catch ( Throwable t )
        {
            log( LogService.LOG_INFO, bundleContext.getBundle(),
                "ConfigurationAdmin supporting ComponentRegistry not available, not using ConfigurationAdmin", t );
        }

        return new ComponentRegistry( bundleContext );
    }


    private static int getLogLevel( BundleContext bundleContext )
    {
        String levelString = bundleContext.getProperty( "ds.loglevel" );
        if ( levelString != null )
        {
            try
            {
                return Integer.parseInt( levelString );
            }
            catch ( NumberFormatException nfe )
            {
                // might be a descriptive name
            }

            if ( "debug".equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_DEBUG;
            }
            else if ( "info".equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_INFO;
            }
            else if ( "warn".equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_WARNING;
            }
            else if ( "error".equalsIgnoreCase( levelString ) )
            {
                return LogService.LOG_ERROR;
            }
        }

        // check ds.showtrace property
        levelString = bundleContext.getProperty( "ds.trace" );
        if ( "true".equalsIgnoreCase( bundleContext.getProperty( "ds.showtrace" ) ) )
        {
            return LogService.LOG_DEBUG;
        }

        // next check ds.showerrors property
        if ( "false".equalsIgnoreCase( bundleContext.getProperty( "ds.showerrors" ) ) )
        {
            return -1; // no logging at all !!
        }

        // default log level (errors only)
        return LogService.LOG_ERROR;
    }


    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level to log the message at
     * @param message The message to log
     * @param ex An optional <code>Throwable</code> whose stack trace is written,
     *      or <code>null</code> to not log a stack trace.
     */
    public static void log( int level, Bundle bundle, String message, Throwable ex )
    {
        if ( m_logLevel >= level )
        {
            Object logger = ( m_logService != null ) ? m_logService.getService() : null;
            if ( logger == null )
            {
                // output depending on level
                PrintStream out = ( level == LogService.LOG_ERROR ) ? System.err : System.out;

                // level as a string
                StringBuffer buf = new StringBuffer();
                switch ( level )
                {
                    case ( LogService.LOG_DEBUG     ):
                        buf.append( "DEBUG: " );
                        break;
                    case ( LogService.LOG_INFO     ):
                        buf.append( "INFO : " );
                        break;
                    case ( LogService.LOG_WARNING     ):
                        buf.append( "WARN : " );
                        break;
                    case ( LogService.LOG_ERROR     ):
                        buf.append( "ERROR: " );
                        break;
                    default:
                        buf.append( "UNK  : " );
                        break;
                }

                // bundle information
                if ( bundle != null )
                {
                    buf.append( bundle.getSymbolicName() );
                    buf.append( " (" );
                    buf.append( bundle.getBundleId() );
                    buf.append( "): " );
                }

                // the message
                buf.append( message );

                // keep the message and the stacktrace together
                synchronized ( out)
                {
                    out.println( buf );
                    if ( ex != null )
                    {
                        ex.printStackTrace( out );
                    }
                }
            }
            else
            {
                ( ( LogService ) logger ).log( level, message, ex );
            }
        }
    }
}