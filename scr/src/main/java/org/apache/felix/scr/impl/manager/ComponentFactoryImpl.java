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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;

/**
 * The <code>ComponentFactoryImpl</code> extends the {@link AbstractComponentManager}
 * class to implement the component factory functionality. As such the
 * OSGi Declarative Services <code>ComponentFactory</code> interface is
 * implemented.
 * <p>
 * In addition the {@link ComponentHolder} interface is implemented to use this
 * class directly as the holder for component instances created by the
 * {@link #newInstance(Dictionary)} method.
 * <p>
 * Finally, if the <code>ds.factory.enabled</code> bundle context property is
 * set to <code>true</code>, component instances can be created by factory
 * configurations. This functionality is present for backwards compatibility
 * with earlier releases of the Apache Felix Declarative Services implementation.
 * But keep in mind, that this is non-standard behaviour.
 */
public class ComponentFactoryImpl extends AbstractComponentManager implements ComponentFactory, ComponentHolder
{

    /**
     * Contains the component instances created by calling the
     * {@link #newInstance(Dictionary)} method. These component instances are
     * provided with updated configuration (or deleted configuration) if
     * such modifications for the component factory takes place.
     * <p>
     * The map is keyed by the component manager instances. The value of each
     * entry is the same as the entry's key.
     */
    private final Map m_componentInstances;

    /**
     * The configuration for the component factory. This configuration is
     * supplied as the base configuration for each component instance created
     * by the {@link #newInstance(Dictionary)} method.
     */
    private Dictionary m_configuration;

    /**
     * The map of components created from Configuration objects maps PID to
     * {@link ImmediateComponentManager} for configuration updating this map is
     * lazily created. This map is only used if the {@link #m_isConfigurationFactory}
     * field is <code>true</code>.
     */
    private Map m_configuredServices;

    /**
     * Whether this instance supports creating component instances for factory
     * configuration instances. This is backwards compatibility behaviour and
     * contradicts the specification (Section 112.7)
     */
    private final boolean m_isConfigurationFactory;


    public ComponentFactoryImpl( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        super( activator, metadata );
        m_componentInstances = new IdentityHashMap();
        m_isConfigurationFactory = "true".equals( activator.getBundleContext().getProperty( "ds.factory.enabled" ) );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.component.ComponentFactory#newInstance(java.util.Dictionary)
     */
    public ComponentInstance newInstance( Dictionary dictionary )
    {
        final ImmediateComponentManager cm = createComponentManager();

        cm.setFactoryProperties( dictionary );
        cm.reconfigure( m_configuration );

        // enable and activate immediately
        cm.enableInternal();
        cm.activateInternal();

        final ComponentInstance instance = cm.getComponentInstance();
        if ( instance == null )
        {
            // activation failed, clean up component manager
            cm.dispose();
            throw new ComponentException( "Failed activating component" );
        }

        m_componentInstances.put( cm, cm );
        return instance;
    }


    /**
     * The component factory does not have a component to create.
     * <p>
     * But in the backwards compatible case any instances created for factory
     * configuration instances are to enabled as a consequence of activating
     * the component factory.
     */
    protected boolean createComponent()
    {
        ImmediateComponentManager[] cms = getComponentManagers( m_configuredServices );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].enable();
        }

        return true;
    }


    /**
     * The component factory does not have a component to delete.
     * <p>
     * But in the backwards compatible case any instances created for factory
     * configuration instances are to disabled as a consequence of deactivating
     * the component factory.
     */
    protected void deleteComponent( int reason )
    {
        ImmediateComponentManager[] cms = getComponentManagers( m_configuredServices );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].disable();
        }
    }


    protected ServiceRegistration registerService()
    {
        log( LogService.LOG_DEBUG, "registering component factory", null );

        Dictionary serviceProperties = getProperties();
        return getActivator().getBundleContext().registerService( new String[]
            { ComponentFactory.class.getName() }, getService(), serviceProperties );
    }


    public Object getInstance()
    {
        // this does not return the component instance actually
        return null;
    }


    public boolean hasConfiguration()
    {
        return true;
    }


    public Dictionary getProperties()
    {
        Dictionary props = new Hashtable();

        // 112.5.5 The Component Factory service must register with the following properties
        props.put( ComponentConstants.COMPONENT_NAME, getComponentMetadata().getName() );
        props.put( ComponentConstants.COMPONENT_FACTORY, getComponentMetadata().getFactoryIdentifier() );

        // also register with the factory PID
        props.put( Constants.SERVICE_PID, getComponentMetadata().getName() );

        // descriptive service properties
        props.put( Constants.SERVICE_DESCRIPTION, "ManagedServiceFactory for Factory Component"
            + getComponentMetadata().getName() );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );

        return props;
    }


    protected Object getService()
    {
        return this;
    }

    //---------- Component interface


    public ComponentInstance getComponentInstance()
    {
        // a ComponentFactory is not a real component and as such does
        // not have a ComponentInstance
        return null;
    }


    //---------- ComponentHolder interface

    public void configurationDeleted( String pid )
    {
        if ( pid.equals( getComponentMetadata().getName() ) )
        {
            m_configuration = null;
            reconfigureComponents( null );
        }
        else if ( m_isConfigurationFactory && m_configuredServices != null )
        {
            ImmediateComponentManager cm = ( ImmediateComponentManager ) m_configuredServices.remove( pid );
            if ( cm != null )
            {
                log( LogService.LOG_DEBUG, "Disposing component after configuration deletion", null );

                cm.dispose();
            }
        }
    }


    public void configurationUpdated( String pid, Dictionary configuration )
    {
        if ( pid.equals( getComponentMetadata().getName() ) )
        {
            m_configuration = configuration;
            reconfigureComponents( configuration );
        }
        else if ( m_isConfigurationFactory )
        {
            ImmediateComponentManager cm;
            if ( m_configuredServices != null )
            {
                cm = ( ImmediateComponentManager ) m_configuredServices.get( pid );
            }
            else
            {
                m_configuredServices = new HashMap();
                cm = null;
            }

            if ( cm == null )
            {
                // create a new instance with the current configuration
                cm = createComponentManager();

                // this should not call component reactivation because it is
                // not active yet
                cm.reconfigure( configuration );

                // enable asynchronously if components are already enabled
                if ( getState() == STATE_FACTORY )
                {
                    cm.enable();
                }

                // keep a reference for future updates
                m_configuredServices.put( pid, cm );

            }
            else
            {
                // update the configuration as if called as ManagedService
                cm.reconfigure( configuration );
            }
        }
        else
        {
            // 112.7 Factory Configuration not allowed for factory component
            log( LogService.LOG_ERROR, "Component Factory cannot be configured by factory configuration", null );
        }
    }


    /**
     * A component factory component holder enables the held components by
     * enabling itself.
     */
    public void enableComponents()
    {
        enable();
    }


    /**
     * Reconfigure all components created calling the
     * {@link #newInstance(Dictionary)} method to update them with the new
     * configuration from the configuration admin.
     * <p>
     * This method is not used to reconfigure components created as part of
     * backwards compatible support for configuration factories since they
     * are reconfigured directly by {@link #configurationUpdated(String, Dictionary)}
     * and {@link #configurationDeleted(String)}
     *
     * @param configuration the new configuration
     */
    private void reconfigureComponents( Dictionary configuration )
    {
        ImmediateComponentManager[] cms = getComponentManagers( m_componentInstances );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].reconfigure( configuration );
        }
    }


    /**
     * A component factory component holder disables the held components by
     * disabling itself.
     */
    public void disableComponents()
    {
        disable();
    }


    /**
     * Disposes off all components ever created by this component holder. This
     * method is called if either the Declarative Services runtime is stopping
     * or if the owning bundle is stopped. In both cases all components created
     * by this holder must be disposed off.
     */
    public void disposeComponents( int reason )
    {
        ImmediateComponentManager[] cms = getComponentManagers( m_componentInstances );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].dispose( reason );
        }

        m_componentInstances.clear();

        cms = getComponentManagers( m_configuredServices );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].dispose( reason );
        }

        m_configuredServices = null;

        // finally dispose the component factory itself
        dispose( reason );
    }


    public void disposed( ImmediateComponentManager component )
    {
        synchronized ( m_componentInstances )
        {
            m_componentInstances.remove( component );
        }
    }


    //---------- internal

    /**
     * Creates an {@link ImmediateComponentManager} instance with the
     * {@link BundleComponentActivator} and {@link ComponentMetadata} of this
     * instance. The component manager is kept in the internal set of created
     * components. The component is neither configured nor enabled.
     */
    private ImmediateComponentManager createComponentManager()
    {
        return new ImmediateComponentManager( getActivator(), this, getComponentMetadata() );
    }


    private ImmediateComponentManager[] getComponentManagers( Map componentMap )
    {
        if ( componentMap != null )
        {
            synchronized ( componentMap )
            {
                ImmediateComponentManager[] cm = new ImmediateComponentManager[componentMap.size()];
                componentMap.values().toArray( cm );
                return cm;
            }
        }

        return new ImmediateComponentManager[0];
    }
}
