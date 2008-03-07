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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.felix.scr.impl.parser.KXml2SAXParser;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The BundleComponentActivator is helper class to load and unload Components of
 * a single bundle. It will read information from the metadata.xml file
 * descriptors and create the corresponding managers.
 */
class BundleComponentActivator
{
    // global component registration
    private ComponentRegistry m_componentRegistry;

    // The bundle context owning the registered component
    private BundleContext m_context = null;

    // This is a list of component instance managers that belong to a particular bundle
    private List m_managers = new ArrayList();

    // The Configuration Admin tracker providing configuration for components
    private ServiceTracker m_configurationAdmin;

    // The Configuration Admin tracker providing configuration for components
    private ServiceTracker m_logService;

    // thread acting upon configurations
    private ComponentActorThread m_componentActor;

    // true as long as the dispose method is not called
    private boolean m_active;

    // the logging level
    private int m_logLevel;


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
    BundleComponentActivator( ComponentRegistry componentRegistry, ComponentActorThread componentActor,
        BundleContext context, int logLevel ) throws ComponentException
    {
        // keep the parameters for later
        m_componentRegistry = componentRegistry;
        m_componentActor = componentActor;
        m_context = context;

        // mark this instance active
        m_active = true;

        // have the Configuration Admin Service handy (if available)
        m_configurationAdmin = new ServiceTracker( context, ConfigurationAdmin.class.getName(), null );
        m_configurationAdmin.open();

        // have the LogService handy (if available)
        m_logService = new ServiceTracker( context, Activator.LOGSERVICE_CLASS, null );
        m_logService.open();
        m_logLevel = logLevel;

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

            URL descriptorURL = m_context.getBundle().getResource( descriptorLocation );
            if ( descriptorURL == null )
            {
                // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
                // fragments, SCR must log an error message with the Log Service, if present, and continue.
                log( LogService.LOG_ERROR, "Component descriptor entry '" + descriptorLocation + "' not found", null,
                    null );
                continue;
            }

            InputStream stream = null;
            try
            {
                stream = descriptorURL.openStream();

                BufferedReader in = new BufferedReader( new InputStreamReader( stream ) );
                XmlHandler handler = new XmlHandler( m_context.getBundle() );
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
                        metadata.validate();

                        // Request creation of the component manager
                        ComponentManager manager;
                        if ( metadata.isFactory() )
                        {
                            // 112.2.4 SCR must register a Component Factory
                            // service on behalf ot the component
                            // as soon as the component factory is satisfied
                            manager = new ComponentFactoryImpl( this, metadata, m_componentRegistry );
                        }
                        else
                        {
                            manager = ManagerFactory.createManager( this, metadata, m_componentRegistry
                                .createComponentId() );
                        }

                        // register the component after validation
                        m_componentRegistry.registerComponent( metadata.getName(), manager );
                        m_managers.add( manager );

                        // enable the component
                        if ( metadata.isEnabled() )
                        {
                            manager.enable();
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

                log( LogService.LOG_ERROR, "Problem reading descriptor entry '" + descriptorLocation + "'", null, ex );
            }
            catch ( Exception ex )
            {
                log( LogService.LOG_ERROR, "General problem with descriptor entry '" + descriptorLocation + "'", null,
                    ex );
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
    }


    /**
    * Dispose of this component activator instance and all the component
    * managers.
    */
    void dispose()
    {
        if ( m_context == null )
        {
            return;
        }

        // mark instance inactive (no more component activations)
        m_active = false;

        log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [" + m_context.getBundle().getBundleId()
            + "] will destroy " + m_managers.size() + " instances", null, null );

        while ( m_managers.size() != 0 )
        {
            ComponentManager manager = ( ComponentManager ) m_managers.get( 0 );
            try
            {
                m_managers.remove( manager );
                manager.dispose();
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, "BundleComponentActivator : Exception invalidating", manager
                    .getComponentMetadata(), e );
            }
            finally
            {
                m_componentRegistry.unregisterComponent( manager.getComponentMetadata().getName() );
            }

        }

        // close the Configuration Admin tracker
        if ( m_configurationAdmin != null )
        {
            m_configurationAdmin.close();
        }

        log( LogService.LOG_DEBUG, "BundleComponentActivator : Bundle [" + m_context.getBundle().getBundleId()
            + "] STOPPED", null, null );

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
     * Returns the list of instance references currently associated to this activator
     *
     * @return the list of instance references
     */
    protected List getInstanceReferences()
    {
        return m_managers;
    }


    /**
    * Returns the BundleContext
    *
    * @return the BundleContext
    */
    protected BundleContext getBundleContext()
    {
        return m_context;
    }


    /**
     * Returns the <code>ConfigurationAdmin</code> service used to retrieve
     * configuration data for components managed by this activator or
     * <code>null</code> if no Configuration Admin Service is available in the
     * framework.
     */
    protected ConfigurationAdmin getConfigurationAdmin()
    {
        return ( ConfigurationAdmin ) m_configurationAdmin.getService();
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
    void enableComponent( String name )
    {
        final ComponentManager[] cm = getSelectedComponents( name );
        if ( cm == null )
        {
            return;
        }

        for ( int i = 0; i < cm.length; i++ )
        {
            try
            {
                cm[i].enable();
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Cannot enable component", cm[i].getComponentMetadata(), t );
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
    void disableComponent( String name )
    {
        final ComponentManager[] cm = getSelectedComponents( name );
        if ( cm == null )
        {
            return;
        }

        for ( int i = 0; i < cm.length; i++ )
        {
            try
            {
                log( LogService.LOG_DEBUG, "Disabling Component", cm[i].getComponentMetadata(), null );
                cm[i].disable();
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Cannot disable component", cm[i].getComponentMetadata(), t );
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
    private ComponentManager[] getSelectedComponents( String name )
    {
        // if all components are selected
        if ( name == null )
        {
            return (org.apache.felix.scr.impl.ComponentManager[] ) m_managers.toArray( new ComponentManager[m_managers.size()] );
        }

        if ( m_componentRegistry.getComponent( name ) != null )
        {
            // otherwise just find it
            Iterator it = m_managers.iterator();
            while ( it.hasNext() )
            {
                ComponentManager cm = ( ComponentManager ) it.next();
                if ( name.equals( cm.getComponentMetadata().getName() ) )
                {
                    return new ComponentManager[]
                        { cm };
                }
            }
        }

        // if the component is not known
        return null;
    }


    //---------- Asynchronous Component Handling ------------------------------

    /**
     * Schedules the given <code>task</code> for asynchrounous execution or
     * synchronously runs the task if the thread is not running. If this instance
     * is {@link #isActive() not active}, the task is not executed.
     *
     * @param task The component task to execute
     */
    void schedule( Runnable task )
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
            log( LogService.LOG_INFO, "BundleComponentActivator is not active; not scheduling " + task, null, null );
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
    void log( int level, String message, ComponentMetadata metadata, Throwable ex )
    {

        if ( m_logLevel >= level )
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
