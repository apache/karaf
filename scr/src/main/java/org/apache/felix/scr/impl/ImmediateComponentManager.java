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
import java.util.Iterator;
import java.util.List;

import org.osgi.service.cm.Configuration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 */
class ImmediateComponentManager extends AbstractComponentManager
{

    // The object that implements the service and that is bound to other services
    private Object m_implementationObject;

    // The context that will be passed to the implementationObject
    private ComponentContext m_componentContext;

    // the activate method
    private Method activateMethod = ReflectionHelper.SENTINEL;

    // the deactivate method
    private Method deactivateMethod = ReflectionHelper.SENTINEL;

    // optional properties provided in the ComponentFactory.newInstance method
    private Dictionary m_factoryProperties;

    // the component properties, also used as service properties
    private Dictionary m_properties;

    // the component properties from the Configuration Admin Service
    // this is null, if none exist or none are provided
    private Dictionary m_configurationProperties;


    /**
     * The constructor receives both the activator and the metadata
     *
     * @param activator
     * @param metadata
     */
    ImmediateComponentManager( BundleComponentActivator activator, ComponentMetadata metadata,
        ComponentRegistry componentRegistry )
    {
        super( activator, metadata, componentRegistry );

        // only ask for configuration if not created by a Component Factory, in
        // which case the configuration is provided by the Component Factory
        if ( !getComponentMetadata().isFactory() )
        {
            Configuration cfg = componentRegistry.getConfiguration( activator.getBundleContext(),
                getComponentMetadata().getName() );
            if ( cfg != null )
            {
                m_configurationProperties = cfg.getProperties();
            }
        }
    }


    /**
     * Before doing real disposal, we also have to unregister the managed
     * service which was registered when the instance was created.
     */
    public synchronized void dispose( final int reason )
    {
        // really dispose off this manager instance
        disposeInternal( reason );
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


    protected void deleteComponent( int reason )
    {
        disposeImplementationObject( m_implementationObject, m_componentContext, reason );
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
            log( LogService.LOG_ERROR, "Error during instantiation of the implementation object",
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
            if ( !dm.open( implementationObject ) )
            {
                log( LogService.LOG_ERROR, "Cannot create component instance due to failure to bind reference "
                    + dm.getName(), getComponentMetadata(), null );

                // make sure, we keep no bindings
                it = getDependencyManagers();
                while ( it.hasNext() )
                {
                    dm = ( DependencyManager ) it.next();
                    dm.close();
                }

                return null;
            }
        }

        // get the method
        if ( activateMethod == ReflectionHelper.SENTINEL )
        {
            activateMethod = getMethod( implementationObject, getComponentMetadata().getActivate(),
                ReflectionHelper.ACTIVATE_ACCEPTED_PARAMETERS );
        }

        // 4. Call the activate method, if present
        if ( activateMethod != null && !invokeMethod( activateMethod, implementationObject, componentContext, -1 ) )
        {
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            it = getDependencyManagers();
            while ( it.hasNext() )
            {
                DependencyManager dm = ( DependencyManager ) it.next();
                dm.close();
            }

            implementationObject = null;
        }

        return implementationObject;
    }


    protected void disposeImplementationObject( Object implementationObject, ComponentContext componentContext,
        int reason )
    {

        // get the method
        if ( deactivateMethod == ReflectionHelper.SENTINEL )
        {
            deactivateMethod = getMethod( implementationObject, getComponentMetadata().getDeactivate(),
                ReflectionHelper.DEACTIVATE_ACCEPTED_PARAMETERS );
        }

        // 1. Call the deactivate method, if present
        // don't care for the result, the error (acccording to 112.5.12 If the deactivate
        // method throws an exception, SCR must log an error message containing the
        // exception with the Log Service and continue) has already been logged
        if ( deactivateMethod != null )
        {
            invokeMethod( deactivateMethod, implementationObject, componentContext, reason );
        }

        // 2. Unbind any bound services
        Iterator it = getDependencyManagers();

        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();
            dm.close();
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
     * <p>
     * This causes the component to be reactivated with the new configuration
     * unless no configuration has ever been set on this component and the
     * <code>configuration</code> parameter is <code>null</code>. In this case
     * nothing is to be done. If a configuration has previously been set and
     * now the configuration is deleted, the <code>configuration</code>
     * parameter is <code>null</code> and the component has to be reactivated
     * with the default configuration.
     *
     * @param configuration The configuration properties for the component from
     *      the Configuration Admin Service or <code>null</code> if there is
     *      no configuration or if the configuration has just been deleted.
     */
    public void reconfigure( Dictionary configuration )
    {
        // nothing to do if there is no configuration (see FELIX-714)
        if ( configuration == null && m_configurationProperties == null )
        {
            log( LogService.LOG_DEBUG, "No configuration provided (or deleted), nothing to do", getComponentMetadata(),
                null );
            return;
        }

        // store the properties
        m_configurationProperties = configuration;

        // clear the current properties to force using the configuration data
        m_properties = null;

        // reactivate the component to ensure it is provided with the
        // configuration data
        if ( ( getState() & ( STATE_ACTIVE | STATE_FACTORY | STATE_REGISTERED ) ) != 0 )
        {
            log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure from configuration",
                getComponentMetadata(), null );
            int reason = ( configuration == null ) ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
                : ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED;
            reactivate( reason );
        }
    }


    /**
     * Find the method with the given name in the class hierarchy of the
     * implementation object's class. This method looks for methods which have
     * one of the parameter lists of the {@link #ACTIVATE_PARAMETER_LIST} array.
     *
     * @param implementationObject The object whose class (and its super classes)
     *      may provide the method
     * @param methodName Name of the method to look for
     * @param methodTester The {@link ReflectionHelper.MethodTester} instance
     *      used to select the actual method.
     * @return The named method or <code>null</code> if no such method is available.
     */
    private Method getMethod( final Object implementationObject, final String methodName,
        final ReflectionHelper.MethodTester methodTester )
    {
        try
        {
            return ReflectionHelper.getMethod( implementationObject.getClass(), methodName, methodTester );
        }
        catch ( InvocationTargetException ite )
        {
            // We can safely ignore this one
            log( LogService.LOG_WARNING, methodName + " cannot be found", getComponentMetadata(), ite
                .getTargetException() );
        }
        catch ( NoSuchMethodException ex )
        {
            // We can safely ignore this one
            log( LogService.LOG_DEBUG, methodName + " method is not implemented", getComponentMetadata(), null );
        }

        return null;
    }


    /**
     * Invokes the given method on the <code>implementationObject</code> using
     * the <code>componentContext</code> as the base to create method argument
     * list.
     *
     * @param method The method to call. This method must already have been
     *      made accessible by calling
     *      <code>Method.setAccessible(boolean)</code>.
     * @param implementationObject The object on which to call the method.
     * @param componentContext The <code>ComponentContext</code> used to
     *      build the argument list
     * @param reason The deactivation reason code. This should be one of the
     *      values in the {@link ComponentConstants} interface. This parameter
     *      is only of practical use for calling deactivate methods, which may
     *      take a numeric argument indicating the deactivation reason.
     *
     * @return <code>true</code> if the method should be considered invoked
     *      successfully. <code>false</code> is returned if the method threw
     *      an exception.
     *
     * @throws NullPointerException if any of the parameters is <code>null</code>.
     */
    private boolean invokeMethod( final Method method, final Object implementationObject,
        final ComponentContext componentContext, int reason )
    {
        final String methodName = method.getName();
        try
        {
            // build argument list
            Class[] paramTypes = method.getParameterTypes();
            Object[] param = new Object[paramTypes.length];
            for ( int i = 0; i < param.length; i++ )
            {
                if ( paramTypes[i] == ReflectionHelper.COMPONENT_CONTEXT_CLASS )
                {
                    param[i] = componentContext;
                }
                else if ( paramTypes[i] == ReflectionHelper.BUNDLE_CONTEXT_CLASS )
                {
                    param[i] = componentContext.getBundleContext();
                }
                else if ( paramTypes[i] == ReflectionHelper.MAP_CLASS )
                {
                    // note: getProperties() returns a Hashtable which is a Map
                    param[i] = componentContext.getProperties();
                }
                else if ( paramTypes[i] == ReflectionHelper.INTEGER_CLASS || paramTypes[i] == Integer.TYPE)
                {
                    param[i] = new Integer(reason);
                }
            }

            method.invoke( implementationObject, param );
        }
        catch ( IllegalAccessException ex )
        {
            // Ignored, but should it be logged?
            log( LogService.LOG_DEBUG, methodName + " method cannot be called", getComponentMetadata(), null );
        }
        catch ( InvocationTargetException ex )
        {
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            log( LogService.LOG_ERROR, "The " + methodName + " method has thrown an exception", getComponentMetadata(),
                ex.getCause() );

            // method threw, so it was a failure
            return false;
        }
        catch ( Throwable t )
        {
            // anything else went wrong, log the message and fail the invocation
            log( LogService.LOG_ERROR, "The " + methodName + " method could not be called", getComponentMetadata(), t );

            // method invocation threw, so it was a failure
            return false;
        }

        // assume success (also if the method is not available or accessible)
        return true;
    }
}
