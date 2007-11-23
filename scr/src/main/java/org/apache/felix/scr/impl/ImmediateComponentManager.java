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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
class ImmediateComponentManager extends AbstractComponentManager
{
    // The object that implements the service and that is bound to other services
    private Object m_implementationObject;

    // The context that will be passed to the implementationObject
    private ComponentContext m_componentContext;

    // optional properties provided in the ComponentFactory.newInstance method
    private Dictionary m_factoryProperties;

    // the component properties, also used as service properties
    private Dictionary m_properties;

    // the managed service registration object created in the constructor
    // to receive configuration from the Configuration Admin Service
    // null, if this is a component created by a component factory
    private ServiceRegistration m_managedServiceRegistration;

    // the component properties from the Configuration Admin Service
    // this is null, if none exist or none are provided
    private Dictionary m_configurationProperties;


    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     */
    ImmediateComponentManager( BundleComponentActivator activator, ComponentMetadata metadata, long componentId )
    {
        super( activator, metadata, componentId );

        // only register as ManagedService if not created by a Component Factory
        if ( !getComponentMetadata().isFactory() )
        {
            Dictionary props = new Hashtable();
            props.put( Constants.SERVICE_PID, getComponentMetadata().getName() );
            props.put( Constants.SERVICE_DESCRIPTION, "ManagedService for Component "
                + getComponentMetadata().getName() );
            props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );

            // register an anonymous managed service instance
            ManagedService ms = new ManagedService()
            {
                public void updated( Dictionary properties )
                {
                    reconfigure( properties );
                }
            };

            m_managedServiceRegistration = activator.getBundleContext().registerService(
                ManagedService.class.getName(), ms, props );
        }
    }


    /**
     * Before doing real disposal, we also have to unregister the managed
     * service which was registered when the instance was created.
     */
    public void dispose()
    {
        if ( m_managedServiceRegistration != null )
        {
            try
            {
                m_managedServiceRegistration.unregister();
                m_managedServiceRegistration = null;
            }
            catch ( Throwable t )
            {
                getActivator().log( LogService.LOG_INFO, "Unexpected problem unregistering ManagedService",
                    getComponentMetadata(), t );
            }
        }

        // really dispose off this manager instance
        super.dispose();
    }


    // 1. Load the component implementation class
    // 2. Create the component instance and component context
    // 3. Bind the target services
    // 4. Call the activate method, if present
    // if this method is overwritten, the deleteComponent method should
    // also be overwritten
    protected boolean createComponent()
    {
        ComponentContext tmpContext = new ComponentContextImpl( this );
        Object tmpComponent = createImplementationObject( tmpContext );

        // if something failed creating the component instance, return false
        if ( tmpComponent == null )
        {
            return false;
        }

        // otherwise set the context and component instance and return true
        m_componentContext = tmpContext;
        m_implementationObject = tmpComponent;
        return true;
    }


    protected void deleteComponent()
    {
        disposeImplementationObject( m_implementationObject, m_componentContext );
        m_implementationObject = null;
        m_componentContext = null;
        m_properties = null;
    }


    //**********************************************************************************************************

    /**
    * Get the object that is implementing this descriptor
    *
    * @return the object that implements the services
    */
    public Object getInstance()
    {
        return m_implementationObject;
    }


    protected Object createImplementationObject( ComponentContext componentContext )
    {
        Object implementationObject;

        // 1. Load the component implementation class
        // 2. Create the component instance and component context
        // If the component is not immediate, this is not done at this moment
        try
        {
            // 112.4.4 The class is retrieved with the loadClass method of the component's bundle
            Class c = getActivator().getBundleContext().getBundle().loadClass(
                getComponentMetadata().getImplementationClassName() );

            // 112.4.4 The class must be public and have a public constructor without arguments so component instances
            // may be created by the SCR with the newInstance method on Class
            implementationObject = c.newInstance();
        }
        catch ( Exception ex )
        {
            // failed to instantiate, return null
            getActivator().log( LogService.LOG_ERROR, "Error during instantiation of the implementation object",
                getComponentMetadata(), ex );
            return null;
        }

        // 3. Bind the target services
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            // if a dependency turned unresolved since the validation check,
            // creating the instance fails here, so we deactivate and return
            // null.
            DependencyManager dm = ( DependencyManager ) it.next();
            if ( !dm.bind( implementationObject ) )
            {
                getActivator().log( LogService.LOG_ERROR,
                    "Cannot create component instance due to failure to bind reference " + dm.getName(),
                    getComponentMetadata(), null );

                // make sure, we keep no bindings
                it = getDependencyManagers();
                while ( it.hasNext() )
                {
                    dm = ( DependencyManager ) it.next();
                    dm.unbind( implementationObject );
                }

                return null;
            }
        }

        // 4. Call the activate method, if present
        // Search for the activate method
        try
        {
            Method activateMethod = getMethod( implementationObject.getClass(), "activate", new Class[]
                { ComponentContext.class }, false );
            activateMethod.invoke( implementationObject, new Object[]
                { componentContext } );
        }
        catch ( NoSuchMethodException ex )
        {
            // We can safely ignore this one
            getActivator().log( LogService.LOG_DEBUG, "activate() method is not implemented", getComponentMetadata(),
                null );
        }
        catch ( IllegalAccessException ex )
        {
            // Ignored, but should it be logged?
            getActivator().log( LogService.LOG_DEBUG, "activate() method cannot be called", getComponentMetadata(),
                null );
        }
        catch ( InvocationTargetException ex )
        {
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            getActivator().log( LogService.LOG_ERROR, "The activate method has thrown an exception",
                getComponentMetadata(), ex.getCause() );

            // make sure, we keep no bindings
            it = getDependencyManagers();
            while ( it.hasNext() )
            {
                DependencyManager dm = ( DependencyManager ) it.next();
                dm.unbind( implementationObject );
            }

            return null;
        }

        return implementationObject;
    }


    protected void disposeImplementationObject( Object implementationObject, ComponentContext componentContext )
    {
        // 1. Call the deactivate method, if present
        // Search for the activate method
        try
        {
            Method deactivateMethod = getMethod( implementationObject.getClass(), "deactivate", new Class[]
                { ComponentContext.class }, false );
            deactivateMethod.invoke( implementationObject, new Object[]
                { componentContext } );
        }
        catch ( NoSuchMethodException ex )
        {
            // We can safely ignore this one
            getActivator().log( LogService.LOG_DEBUG, "deactivate() method is not implemented", getComponentMetadata(),
                null );
        }
        catch ( IllegalAccessException ex )
        {
            // Ignored, but should it be logged?
            getActivator().log( LogService.LOG_DEBUG, "deactivate() method cannot be called", getComponentMetadata(),
                null );
        }
        catch ( InvocationTargetException ex )
        {
            // 112.5.12 If the deactivate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and continue
            getActivator().log( LogService.LOG_ERROR, "The deactivate method has thrown an exception",
                getComponentMetadata(), ex.getCause() );
        }

        // 2. Unbind any bound services
        Iterator it = getDependencyManagers();

        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();
            dm.unbind( implementationObject );
        }

        // 3. Release all references
        // nothing to do, we keep no references on per-Bundle services
    }


    /**
     * Returns the service object to be registered if the service element is
     * specified.
     * <p>
     * Extensions of this class may overwrite this method to return a
     * ServiceFactory to register in the case of a delayed or a service
     * factory component.
     */
    protected Object getService()
    {
        return m_implementationObject;
    }


    protected void setFactoryProperties( Dictionary dictionary )
    {
        m_factoryProperties = copyTo( null, dictionary );
    }


    /**
     * Returns the (private copy) of the Component properties to be used
     * for the ComponentContext as well as eventual service registration.
     * <p>
     * Method implements the Component Properties provisioning as described
     * in 112.6, Component Properties.
     *
     * @return a private Hashtable of component properties
     */
    public Dictionary getProperties()
    {

        // TODO: Currently on ManagedService style configuration is supported, ManagedServiceFactory style is missing

        if ( m_properties == null )
        {

            // 1. the properties from the component descriptor
            Dictionary props = copyTo( null, getComponentMetadata().getProperties() );

            // 2. add target properties of references
            // 112.6 Component Properties, target properties (p. 302)
            List depMetaData = getComponentMetadata().getDependencies();
            for ( Iterator di = depMetaData.iterator(); di.hasNext(); )
            {
                ReferenceMetadata rm = ( ReferenceMetadata ) di.next();
                if ( rm.getTarget() != null )
                {
                    props.put( rm.getTargetPropertyName(), rm.getTarget() );
                }
            }

            // 3. overlay with Configuration Admin properties
            copyTo( props, m_configurationProperties );

            // 4. copy any component factory properties, not supported yet
            copyTo( props, m_factoryProperties );

            // 5. set component.name and component.id
            props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
            props.put( ComponentConstants.COMPONENT_ID, new Long( getId() ) );

            m_properties = props;
        }

        return m_properties;
    }


    /**
     * Called by the Configuration Admin Service to update the component with
     * Configuration properties.
     *
     * @param configuration The configuration properties for the component from
     *      the Configuration Admin Service or <code>null</code> if there is
     *      no configuration or if the configuration has just been deleted.
     */
    public void reconfigure( Dictionary configuration )
    {
        // store the properties
        m_configurationProperties = configuration;

        // clear the current properties to force using the configuration data
        m_properties = null;

        // reactivate the component to ensure it is provided with the
        // configuration data
        if ( ( getState() & ( STATE_ACTIVE | STATE_FACTORY | STATE_REGISTERED ) ) != 0 )
        {
            reactivate();
        }
    }
}
