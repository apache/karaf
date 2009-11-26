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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.ScrConfiguration;
import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.XmlHandler;
import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The BundleComponentActivator is helper class to load and unload Components of
 * a single bundle. It will read information from the metadata.xml file
 * descriptors and create the corresponding managers.
 */
public class BundleComponentActivator implements Logger
{
    // global component registration
    private ComponentRegistry m_componentRegistry;

    // The bundle context owning the registered component
    private BundleContext m_context = null;

    // This is a list of component instance managers that belong to a particular bundle
    private List m_managers = new ArrayList();

    // The Configuration Admin tracker providing configuration for components
    private ServiceTracker m_logService;

    // thread acting upon configurations
    private ComponentActorThread m_componentActor;

    // true as long as the dispose method is not called
    private boolean m_active;

    // the configuration
    private ScrConfiguration m_configuration;


    /**
     * Called upon starting of the bundle. This method invokes initialize() which
     * parses the metadata and creates the instance managers
     *
     * @param componentRegistry The <code>ComponentRegistry</code> used to
     *      register components with to ensure uniqueness of component names
     *      and to ensure configuration updates.
     * @param   context  The bundle context owning the components
     *
     * @throws ComponentException if any error occurrs initializing this class
     */
    BundleComponentActivator( ComponentRegistry componentRegistry,
        ComponentActorThread componentActor, BundleContext context, ScrConfiguration configuration ) throws ComponentException
    {
        // keep the parameters for later
        m_componentRegistry = componentRegistry;
        m_componentActor = componentActor;
        m_context = context;

        // mark this instance active
        m_active = true;

        // have the LogService handy (if available)
        m_logService = new ServiceTracker( context, Activator.LOGSERVICE_CLASS, null );
        m_logService.open();
        m_configuration = configuration;

        // Get the Metadata-Location value from the manifest
        String descriptorLocations = ( String ) m_context.getBundle().getHeaders().get( "Service-Component" );
        if ( descriptorLocations == null )
        {
            throw new ComponentException( "Service-Component entry not found in the manifest" );
        }

        initialize( descriptorLocations );
    }


    /**
     * Gets the MetaData location, parses the meta data and requests the processing
     * of binder instances
     *
     * @param descriptorLocations A comma separated list of locations of
     *      component descriptors. This must not be <code>null</code>.
     *
     * @throws IllegalStateException If the bundle has already been uninstalled.
     */
    private void initialize( String descriptorLocations )
    {

        // 112.4.1: The value of the the header is a comma separated list of XML entries within the Bundle
        StringTokenizer st = new StringTokenizer( descriptorLocations, ", " );

        while ( st.hasMoreTokens() )
        {
            String descriptorLocation = st.nextToken();

            URL[] descriptorURLs = findDescriptors( m_context.getBundle(), descriptorLocation );
            if ( descriptorURLs.length == 0 )
            {
                // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
                // fragments, SCR must log an error message with the Log Service, if present, and continue.
                log( LogService.LOG_ERROR, "Component descriptor entry ''{0}'' not found", new Object[]
                    { descriptorLocation }, null, null );
                continue;
            }

            // load from the descriptors
            for ( int i = 0; i < descriptorURLs.length; i++ )
            {
                loadDescriptor( descriptorURLs[i] );
            }
        }
    }


    /**
     * Finds component descriptors based on descriptor location.
     *
     * @param bundle bundle to search for descriptor files
     * @param descriptorLocation descriptor location
     * @return array of descriptors or empty array if none found
     */
    static URL[] findDescriptors( final Bundle bundle, final String descriptorLocation )
    {
        if ( bundle == null || descriptorLocation == null || descriptorLocation.trim().length() == 0 )
        {
            return new URL[0];
        }

        // for compatibility with previoues versions (<= 1.0.8) use getResource() for non wildcarded entries
        if ( descriptorLocation.indexOf( "*" ) == -1 )
        {
            final URL descriptor = bundle.getResource( descriptorLocation );
            if ( descriptor == null )
            {
                return new URL[0];
            }
            return new URL[]
                { descriptor };
        }

        // split pattern and path
        final int lios = descriptorLocation.lastIndexOf( "/" );
        final String path;
        final String filePattern;
        if ( lios > 0 )
        {
            path = descriptorLocation.substring( 0, lios );
            filePattern = descriptorLocation.substring( lios + 1 );
        }
        else
        {
            path = "/";
            filePattern = descriptorLocation;
        }

        // find the entries
        final Enumeration entries = bundle.findEntries( path, filePattern, false );
        if ( entries == null || !entries.hasMoreElements() )
        {
            return new URL[0];
        }

        // create the result list
        List urls = new ArrayList();
        while ( entries.hasMoreElements() )
        {
            urls.add( entries.nextElement() );
        }
        return ( URL[] ) urls.toArray( new URL[urls.size()] );
    }


    private void loadDescriptor( final URL descriptorURL )
    {
        // simple path for log messages
        final String descriptorLocation = descriptorURL.getPath();

        InputStream stream = null;
        try
        {
            stream = descriptorURL.openStream();

            BufferedReader in = new BufferedReader( new InputStreamReader( stream ) );
            XmlHandler handler = new XmlHandler( m_context.getBundle(), this );
            KXml2SAXParser parser;

            parser = new KXml2SAXParser( in );

            parser.parseXML( handler );

            // 112.4.2 Component descriptors may contain a single, root component element
            // or one or more component elements embedded in a larger document
            Iterator i = handler.getComponentMetadataList().iterator();
            while ( i.hasNext() )
            {
                ComponentMetadata metadata = ( ComponentMetadata ) i.next();
                try
                {
                    // check and reserve the component name
                    m_componentRegistry.checkComponentName( metadata.getName() );

                    // validate the component metadata
                    metadata.validate( this );

                    // Request creation of the component manager
                    ComponentHolder holder = m_componentRegistry.createComponentHolder( this, metadata );

                    // register the component after validation
                    m_componentRegistry.registerComponent( metadata.getName(), holder );
                    m_managers.add( holder );

                    // enable the component
                    if ( metadata.isEnabled() )
                    {
                        holder.enableComponents();
                    }
                }
                catch ( Throwable t )
                {
                    // There is a problem with this particular component, we'll log the error
                    // and proceed to the next one
                    log( LogService.LOG_ERROR, "Cannot register Component", metadata, t );

                    // make sure the name is not reserved any more
                    m_componentRegistry.unregisterComponent( metadata.getName() );
                }
            }
        }
        catch ( IOException ex )
        {
            // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
            // fragments, SCR must log an error message with the Log Service, if present, and continue.

            log( LogService.LOG_ERROR, "Problem reading descriptor entry ''{0}''", new Object[]
                { descriptorLocation }, null, ex );
        }
        catch ( Exception ex )
        {
            log( LogService.LOG_ERROR, "General problem with descriptor entry ''{0}''", new Object[]
                { descriptorLocation }, null, ex );
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
    }


    /**
    * Dispose of this component activator instance and all the component
    * managers.
    */
    void dispose( int reason )
    {
        if ( m_context == null )
        {
            return;
        }

        // mark instance inactive (no more component activations)
        m_active = false;

        log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] will destroy {1} instances", new Object[]
            { new Long( m_context.getBundle().getBundleId() ), new Integer( m_managers.size() ) }, null, null );

        while ( m_managers.size() != 0 )
        {
            ComponentHolder holder = ( ComponentHolder ) m_managers.get( 0 );
            try
            {
                m_managers.remove( holder );
                holder.disposeComponents( reason );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, "BundleComponentActivator : Exception invalidating", holder
                    .getComponentMetadata(), e );
            }
            finally
            {
                m_componentRegistry.unregisterComponent( holder.getComponentMetadata().getName() );
            }

        }

        log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [{0}] STOPPED", new Object[]
            { new Long( m_context.getBundle().getBundleId() ) }, null, null );

        if (m_logService != null) {
            m_logService.close();
            m_logService = null;
        }

        m_componentActor = null;
        m_componentRegistry = null;
        m_context = null;
    }


    /**
     * Returns <true> if this instance is active, that is if components may be
     * activated for this component. As soon as the {@link #dispose()} is called
     * which means this instance is being shutdown. After the call to <code>dispose</code>
     * this method always returns <code>false</code>.
     */
    boolean isActive()
    {
        return m_active;
    }


    /**
    * Returns the BundleContext
    *
    * @return the BundleContext
    */
    public BundleContext getBundleContext()
    {
        return m_context;
    }


    public ScrConfiguration getConfiguration()
    {
        return m_configuration;
    }


    /**
     * Implements the <code>ComponentContext.enableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * then starting a thread to actually enable all components found.
     * <p>
     * If no component matching the given name is found the thread is not
     * started and the method does nothing.
     *
     * @param name The name of the component to enable or <code>null</code> to
     *      enable all components.
     */
    public void enableComponent( String name )
    {
        final ComponentHolder[] holder = getSelectedComponents( name );
        if ( holder == null )
        {
            return;
        }

        for ( int i = 0; i < holder.length; i++ )
        {
            try
            {
                holder[i].enableComponents();
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Cannot enable component", holder[i].getComponentMetadata(), t );
            }
        }
    }


    /**
     * Implements the <code>ComponentContext.disableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * then starting a thread to actually disable all components found.
     * <p>
     * If no component matching the given name is found the thread is not
     * started and the method does nothing.
     *
     * @param name The name of the component to disable or <code>null</code> to
     *      disable all components.
     */
    public void disableComponent( String name )
    {
        final ComponentHolder[] holder = getSelectedComponents( name );
        if ( holder == null )
        {
            return;
        }

        for ( int i = 0; i < holder.length; i++ )
        {
            try
            {
                log( LogService.LOG_DEBUG, "Disabling Component", holder[i].getComponentMetadata(), null );
                holder[i].disableComponents();
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Cannot disable component", holder[i].getComponentMetadata(), t );
            }
        }
    }


    /**
     * Returns an array of {@link ComponentManager} instances which match the
     * <code>name</code>. If the <code>name</code> is <code>null</code> an
     * array of all currently known component managers is returned. Otherwise
     * an array containing a single component manager matching the name is
     * returned if one is registered. Finally, if no component manager with the
     * given name is registered, <code>null</code> is returned.
     *
     * @param name The name of the component manager to return or
     *      <code>null</code> to return an array of all component managers.
     *
     * @return An array containing one or more component managers according
     *      to the <code>name</code> parameter or <code>null</code> if no
     *      component manager with the given name is currently registered.
     */
    private ComponentHolder[] getSelectedComponents( String name )
    {
        // if all components are selected
        if ( name == null )
        {
            return ( ComponentHolder[] ) m_managers.toArray( new ComponentHolder[m_managers.size()] );
        }

        if ( m_componentRegistry.getComponent( name ) != null )
        {
            // otherwise just find it
            Iterator it = m_managers.iterator();
            while ( it.hasNext() )
            {
                ComponentHolder cm = ( ComponentHolder ) it.next();
                if ( name.equals( cm.getComponentMetadata().getName() ) )
                {
                    return new ComponentHolder[]
                        { cm };
                }
            }
        }

        // if the component is not known
        return null;
    }


    //---------- Component ID support

    public long registerComponentId(AbstractComponentManager componentManager) {
        return m_componentRegistry.registerComponentId(componentManager);
    }

    public void unregisterComponentId(AbstractComponentManager componentManager) {
        m_componentRegistry.unregisterComponentId(componentManager.getId());
    }

    //---------- Asynchronous Component Handling ------------------------------

    /**
     * Schedules the given <code>task</code> for asynchrounous execution or
     * synchronously runs the task if the thread is not running. If this instance
     * is {@link #isActive() not active}, the task is not executed.
     *
     * @param task The component task to execute
     */
    public void schedule( ComponentActivatorTask task )
    {
        if ( isActive() )
        {
            ComponentActorThread cat = m_componentActor;
            if ( cat != null )
            {
                cat.schedule( task );
            }
            else
            {
                log( LogService.LOG_INFO, "Component Actor Thread not running, calling synchronously", null, null );
                try
                {
                    synchronized ( this )
                    {
                        task.run();
                    }
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_INFO, "Unexpected problem executing task", null, t );
                }
            }
        }
        else
        {
            log( LogService.LOG_INFO, "BundleComponentActivator is not active; not scheduling {0}", new Object[]
                { task }, null, null );
        }
    }


    /**
     * Returns <code>true</code> if logging for the given level is enabled.
     */
    public boolean isLogEnabled( int level )
    {
        return m_configuration.getLogLevel() >= level;
    }


    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level to log the message at
     * @param pattern The <code>java.text.MessageFormat</code> message format
     *      string for preparing the message
     * @param arguments The format arguments for the <code>pattern</code>
     *      string.
     * @param ex An optional <code>Throwable</code> whose stack trace is written,
     *      or <code>null</code> to not log a stack trace.
     */
    public void log( int level, String pattern, Object[] arguments, ComponentMetadata metadata, Throwable ex )
    {
        if ( isLogEnabled( level ) )
        {
            final String message = MessageFormat.format( pattern, arguments );
            log( level, message, metadata, ex );
        }
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
    public void log( int level, String message, ComponentMetadata metadata, Throwable ex )
    {
        if ( isLogEnabled( level ) )
        {
            // prepend the metadata name to the message
            if ( metadata != null )
            {
                message = "[" + metadata.getName() + "] " + message;
            }

            Object logger = m_logService.getService();
            if ( logger == null )
            {
                Activator.log( level, getBundleContext().getBundle(), message, ex );
            }
            else
            {
                ( ( LogService ) logger ).log( level, message, ex );
            }
        }
    }

}
