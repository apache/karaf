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
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;

/**
 * The <code>ComponentFactoryImpl</code> TODO
 */
public class ComponentFactoryImpl extends AbstractComponentManager implements ComponentFactory, ComponentHolder
{

    // The map of components created from Configuration objects
    // maps PID to ImmediateComponentManager for configuration updating
    // this map is lazily created
    private Map m_configuredServices;

    // Actually we only use the identity key stuff, but there is
    // no IdentityHashSet and HashSet internally uses a HashMap anyway
    private final Map m_createdComponents;

    // configuration of the component factory
    private Dictionary m_configuration;

    // whether this instance supports creating component instances for factory
    // configuration instances. This is backwards compatibility behaviour and
    // contradicts the specification (Section 112.7)
    private final boolean m_isConfigurationFactory;


    public ComponentFactoryImpl( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        super( activator, metadata );
        m_createdComponents = new IdentityHashMap();
        m_isConfigurationFactory = "true".equals( activator.getBundleContext().getProperty( "ds.factory.enabled" ) );
    }


    /* (non-Javadoc)
     * @see org.osgi.service.component.ComponentFactory#newInstance(java.util.Dictionary)
     */
    public ComponentInstance newInstance( Dictionary dictionary )
    {
        return createComponentManager( dictionary, true );
    }


    protected boolean createComponent()
    {
        // not component to create, newInstance must be used instead
        return true;
    }


    protected void deleteComponent( int reason )
    {
        // though we have nothing to delete really, we have to remove all
        // references to the components created for configuration
        m_createdComponents.clear();
        if (m_configuredServices != null) {
            m_configuredServices = null;
        }
    }


    protected ServiceRegistration registerService()
    {
        log( LogService.LOG_DEBUG, "registering component factory", getComponentMetadata(), null );

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


    public String getName()
    {
        return "Component Factory " + getComponentMetadata().getName();
    }


    //---------- ComponentHolder interface

    public void configurationDeleted( String pid )
    {
        if ( pid.equals( getComponentMetadata().getName() ) )
        {
            m_configuration = null;
            reconfigureComponents( null );
        }
        else if ( m_isConfigurationFactory && getState() == STATE_FACTORY && m_configuredServices != null )
        {
            ImmediateComponentManager cm = ( ImmediateComponentManager ) m_configuredServices.remove( pid );
            if ( cm != null )
            {
                log( LogService.LOG_DEBUG, "Disposing component after configuration deletion", getComponentMetadata(),
                    null );

                disposeComponentManager( cm, ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED );
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
            if ( getState() == STATE_FACTORY )
            {
                // configuration for factory configuration instances

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
                    cm = createComponentManager( configuration, false );

                    // keep a reference for future updates
                    m_configuredServices.put( pid, cm );
                }
                else
                {
                    // update the configuration as if called as ManagedService
                    cm.reconfigure( configuration );
                }
            }
        }
        else
        {
            // 112.7 Factory Configuration not allowed for factory component
            getActivator().log( LogService.LOG_ERROR,
                "Component Factory cannot be configured by factory configuration", getComponentMetadata(), null );
        }
    }


    // TODO: correct ???
    public void enableComponents()
    {
        ImmediateComponentManager[] cms = getComponentManagers( false );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].enable();
        }
    }


    // update components with this configuration
    private void reconfigureComponents( Dictionary configuration )
    {
        ImmediateComponentManager[] cms = getComponentManagers( false );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].reconfigure( configuration );
        }
    }


    // TODO: correct ???
    public void disableComponents()
    {
        ImmediateComponentManager[] cms = getComponentManagers( false );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].disable();
        }
    }


    // TODO: correct ???
    public void disposeComponents( int reason )
    {
        ImmediateComponentManager[] cms = getComponentManagers( true );
        for ( int i = 0; i < cms.length; i++ )
        {
            cms[i].dispose( reason );
        }
    }


    //---------- internal

    /**
     * ComponentManager instances created by this method are not registered
     * with the ComponentRegistry. Therefore, any configuration update to these
     * components must be effected by this class !
     *
     * @param configuration The (initial) configuration for the new
     *      component manager
     * @param isNewInstance <code>true</code> if this component manager is
     *      created as per {@link #newInstance(Dictionary)}. In this case the
     *      given configuration is used as the factory configuration and the
     *      component is immediately enabled (synchronously). Otherwise the
     *      component is created because a new configuration instance of
     *      this factory has been created. In this case the configuration is
     *      used as the normal configuration from configuration admin (not the
     *      factory configuration) and the component is enabled asynchronously.
     */
    private ImmediateComponentManager createComponentManager( Dictionary configuration, boolean isNewInstance )
    {
        ImmediateComponentManager cm = new ImmediateComponentManager( getActivator(), getComponentMetadata() );

        // register with the internal set of created components
        m_createdComponents.put( cm, cm );

        // inject configuration
        if ( isNewInstance )
        {
            cm.setFactoryProperties( configuration );
            cm.reconfigure( m_configuration );
            // enable synchronously
            cm.enableInternal();
            cm.activateInternal();
        }
        else
        {
            // this should not call component reactivation because it is
            // not active yet
            cm.reconfigure( configuration );
            // enable asynchronously
            cm.enable();
        }

        return cm;
    }

    private void disposeComponentManager( ImmediateComponentManager cm, int reason )
    {
        // remove from created components
        m_createdComponents.remove( cm );

        // finally dispose it
        cm.dispose( reason );
    }


    private ImmediateComponentManager[] getComponentManagers( boolean clear )
    {
        synchronized ( m_createdComponents )
        {
            ImmediateComponentManager[] cm = new ImmediateComponentManager[m_createdComponents.size()];
            m_createdComponents.keySet().toArray( cm );

            if ( clear )
            {
                m_createdComponents.clear();
            }

            return cm;
        }
    }
}
