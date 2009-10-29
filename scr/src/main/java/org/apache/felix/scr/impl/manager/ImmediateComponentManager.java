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
package org.apache.felix.scr.impl.manager;


import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.helper.ActivateMethod;
import org.apache.felix.scr.impl.helper.DeactivateMethod;
import org.apache.felix.scr.impl.helper.ModifiedMethod;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;


/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 */
public class ImmediateComponentManager extends AbstractComponentManager
{

    // The object that implements the service and that is bound to other services
    private Object m_implementationObject;

    // The context that will be passed to the implementationObject
    private ComponentContextImpl m_componentContext;

    // the component holder responsible for managing this component
    private ComponentHolder m_componentHolder;

    // the activate method
    private ActivateMethod m_activateMethod;

    // the deactivate method
    private DeactivateMethod m_deactivateMethod;

    // the modify method
    private ModifiedMethod m_modifyMethod;

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
    public ImmediateComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
        ComponentMetadata metadata )
    {
        super( activator, metadata );

        m_componentHolder = componentHolder;
    }


    ComponentHolder getComponentHolder()
    {
        return m_componentHolder;
    }


    void clear()
    {
        if ( m_componentHolder != null )
        {
            m_componentHolder.disposed( this );
        }

        super.clear();
    }


    // 1. Load the component implementation class
    // 2. Create the component instance and component context
    // 3. Bind the target services
    // 4. Call the activate method, if present
    // if this method is overwritten, the deleteComponent method should
    // also be overwritten
    protected boolean createComponent()
    {
        ComponentContextImpl tmpContext = new ComponentContextImpl( this );
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


    ComponentContext getComponentContext()
    {
        return m_componentContext;
    }


    public ComponentInstance getComponentInstance()
    {
        return m_componentContext;
    }


    //**********************************************************************************************************

    /**
    * Get the object that is implementing this descriptor
    *
    * @return the object that implements the services
    */
    Object getInstance()
    {
        return m_implementationObject;
    }


    protected Object createImplementationObject( ComponentContext componentContext )
    {
        final Class implementationObjectClass;
        final Object implementationObject;

        // 1. Load the component implementation class
        // 2. Create the component instance and component context
        // If the component is not immediate, this is not done at this moment
        try
        {
            // 112.4.4 The class is retrieved with the loadClass method of the component's bundle
            implementationObjectClass = getActivator().getBundleContext().getBundle().loadClass(
                getComponentMetadata().getImplementationClassName() );

            // 112.4.4 The class must be public and have a public constructor without arguments so component instances
            // may be created by the SCR with the newInstance method on Class
            implementationObject = implementationObjectClass.newInstance();
        }
        catch ( Exception ex )
        {
            // failed to instantiate, return null
            log( LogService.LOG_ERROR, "Error during instantiation of the implementation object", ex );
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
                log( LogService.LOG_ERROR, "Cannot create component instance due to failure to bind reference {0}",
                    new Object[]
                        { dm.getName() }, null );

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
        if ( m_activateMethod == null)
        {
            m_activateMethod = new ActivateMethod( this, getComponentMetadata().getActivate(), getComponentMetadata()
                .isActivateDeclared(), implementationObjectClass );
        }

        // 4. Call the activate method, if present
        if ( !m_activateMethod.invoke( implementationObject,
            new ActivateMethod.ActivatorParameter( componentContext, 1 ) ) )
        {
            // 112.5.8 If the activate method throws an exception, SCR must log an error message
            // containing the exception with the Log Service and activation fails
            it = getDependencyManagers();
            while ( it.hasNext() )
            {
                DependencyManager dm = ( DependencyManager ) it.next();
                dm.close();
            }

            return null;
        }

        return implementationObject;
    }


    protected void disposeImplementationObject( Object implementationObject, ComponentContext componentContext,
        int reason )
    {

        // get the method
        if ( m_deactivateMethod == null )
        {
            m_deactivateMethod = new DeactivateMethod( this, getComponentMetadata().getDeactivate(),
                getComponentMetadata().isDeactivateDeclared(), implementationObject.getClass() );
        }

        // 1. Call the deactivate method, if present
        // don't care for the result, the error (acccording to 112.5.12 If the deactivate
        // method throws an exception, SCR must log an error message containing the
        // exception with the Log Service and continue) has already been logged
        m_deactivateMethod.invoke( implementationObject, new ActivateMethod.ActivatorParameter( componentContext,
            reason ) );

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


    public boolean hasConfiguration()
    {
        return m_configurationProperties != null;
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
            log( LogService.LOG_DEBUG, "No configuration provided (or deleted), nothing to do", null );
            return;
        }

        // store the properties
        m_configurationProperties = configuration;

        // clear the current properties to force using the configuration data
        m_properties = null;

        if ( getState() == STATE_UNSATISFIED && configuration != null
            && getComponentMetadata().isConfigurationRequired() )
        {
            activateInternal();
            return;
        }

        // reactivate the component to ensure it is provided with the
        // configuration data
        if ( ( getState() & ( STATE_ACTIVE | STATE_FACTORY | STATE_REGISTERED ) ) == 0 )
        {
            // nothing to do for inactive components, leave this method
            return;
        }

        // if the configuration has been deleted but configuration is required
        // this component must be deactivated
        if ( configuration == null && getComponentMetadata().isConfigurationRequired() )
        {
            deactivateInternal( ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED );
        }
        else if ( !modify() )
        {
            log( LogService.LOG_DEBUG, "Deactivating and Activating to reconfigure from configuration", null );
            int reason = ( configuration == null ) ? ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED
                : ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_MODIFIED;
            reactivate( reason );
        }
    }

    private boolean modify() {
        // 1. no live update if there is no declared method
        if ( getComponentMetadata().getModified() == null )
        {
            return false;
        }
        // invariant: we have a modified method name

        // 2. get and check configured method
        if ( m_modifyMethod == null )
        {
            m_modifyMethod = new ModifiedMethod( this, getComponentMetadata().getModified(), getInstance().getClass() );
        }
        // invariant: modify method is configured and found

        // 3. check whether we can dynamically apply the configuration if
        // any target filters influence the bound services
        final Dictionary props = getProperties();
        Iterator it = getDependencyManagers();
        while ( it.hasNext() )
        {
            DependencyManager dm = ( DependencyManager ) it.next();
            if ( !dm.canUpdateDynamically( props ) )
            {
                log( LogService.LOG_INFO,
                    "Cannot dynamically update the configuration due to dependency changes induced on dependency {0}",
                    new Object[]
                        { dm.getName() }, null );
                return false;
            }
        }
        // invariant: modify method existing and no static bound service changes

        // 4. call method (nothing to do when failed, since it has already been logged)
        if ( !m_modifyMethod.invoke( getInstance(), new ActivateMethod.ActivatorParameter( m_componentContext, -1 ) ) )
        {
            // log an error if the declared method cannot be found
            log( LogService.LOG_ERROR, "Declared modify method '{0}' cannot be found, configuring by reactivation",
                new Object[]
                    { getComponentMetadata().getModified() }, null );
            return false;
        }

        // 5. update the target filter on the services now, this may still
        // result in unsatisfied dependencies, in which case we abort
        // this dynamic update and have the component be deactivated
        if ( !verifyDependencyManagers( props ) )
        {
            log(
                LogService.LOG_ERROR,
                "Updating the service references caused at least on reference to become unsatisfied, deactivating component",
                null );
            return false;
        }

        // 6. update service registration properties
        ServiceRegistration sr = getServiceRegistration();
        if ( sr != null )
        {
            try
            {
                final Dictionary regProps = getServiceProperties();
                sr.setProperties( regProps );
            }
            catch ( IllegalStateException ise )
            {
                // service has been unregistered asynchronously, ignore
            }
            catch ( IllegalArgumentException iae )
            {
                log( LogService.LOG_ERROR,
                    "Unexpected configuration property problem when updating service registration",
                    iae );
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Unexpected problem when updating service registration",
                    t );
            }
        }

        // 7. everything set and done, the component has been udpated
        return true;
    }
}
