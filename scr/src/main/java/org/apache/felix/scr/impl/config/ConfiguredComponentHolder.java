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
package org.apache.felix.scr.impl.config;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.service.component.ComponentConstants;


/**
 * The <code>ConfiguredComponentHolder</code> class is a
 * {@link ComponentHolder} for one or more components instances configured by
 * singleton or factory configuration objects received from the Configuration
 * Admin service.
 * <p>
 * This holder is used only for components configured (optionally or required)
 * by the Configuration Admin service. It is not used for components declared
 * as ignoring configuration or if no Configuration Admin service is available.
 * <p>
 * The holder copes with three situations:
 * <ul>
 * <li>No configuration is available for the held component. That is there is
 * no configuration whose <code>service.pid</code> or
 * <code>service.factoryPid</code> equals the component name.</li>
 * <li>A singleton configuration is available whose <code>service.pid</code>
 * equals the component name.</li>
 * <li>One or more factory configurations exist whose
 * <code>service.factoryPid</code> equals the component name.</li>
 * </ul>
 */
public class ConfiguredComponentHolder extends AbstractComponentHolder
{

    /**
     * A map of components configured with factory configuration. The indices
     * are the PIDs (<code>service.pid</code>) of the configuration objects.
     * The values are the {@link ImmediateComponentManager component instances}
     * created on behalf of the configurations.
     */
    private final Map m_components;

    /**
     * The special component used if there is no configuration or a singleton
     * configuration. This field is always non-<code>null</code> and is first
     * created in the constructor. As factory configurations are provided this
     * instance may be configured or "deconfigured".
     * <p>
     * Expected invariants:
     * <ul>
     * <li>This field is never <code>null</code></li>
     * <li>The {@link #m_components} map is empty or the component pointed to
     * by this field is also contained in the map</li>
     * <ul>
     */
    private ImmediateComponentManager m_singleComponent;

    /**
     * Whether components have already been enabled by calling the
     * {@link #enableComponents()} method. If this field is <code>true</code>
     * component instances created per configuration by the
     * {@link #configurationUpdated(String, Dictionary)} method are also
     * enabled. Otherwise they are not enabled immediately.
     */
    private boolean m_enabled;


    ConfiguredComponentHolder( final BundleComponentActivator activator, final ComponentMetadata metadata )
    {
        super( activator, metadata );

        this.m_components = new HashMap();
        this.m_singleComponent = createComponentManager();
        this.m_enabled = false;
    }


    /**
     * The configuration with the given <code>pid</code>
     * (<code>service.pid</code> of the configuration object) is deleted.
     * <p>
     * The following situations are supported:
     * <ul>
     * <li>The configuration was a singleton configuration (pid equals the
     * component name). In this case the internal component map is empty and
     * the single component has been configured by the singleton configuration
     * and is no "deconfigured".</li>
     * <li>A factory configuration object has been deleted and the configured
     * object is set as the single component. If the single component held the
     * last factory configuration object, it is deconfigured. Otherwise the
     * single component is disposed off and replaced by another component in
     * the map of existing components.</li>
     * <li>A factory configuration object has been deleted and the configured
     * object is not set as the single component. In this case the component is
     * simply disposed off and removed from the internal map.</li>
     * </ul>
     */
    public void configurationDeleted( final String pid )
    {
        if ( pid.equals( getComponentName() ) )
        {
            // singleton configuration deleted
            m_singleComponent.reconfigure( null );
        }
        else
        {
            // remove the component configured with the deleted configuration
            ImmediateComponentManager icm = removeComponentManager( pid );
            if ( icm != null )
            {
                // special casing if the single component is deconfigured
                if ( m_singleComponent == icm )
                {

                    // if the single component is the last remaining, deconfi
                    if ( m_components.isEmpty() )
                    {

                        // if the single component is the last remaining
                        // deconfigure it
                        icm.reconfigure( null );
                        icm = null;

                    }
                    else
                    {

                        // replace the single component field with another
                        // entry from the map
                        m_singleComponent = ( ImmediateComponentManager ) m_components.values().iterator().next();

                    }
                }

                // icm may be null if the last configuration deleted was the
                // single component's configuration. Otherwise the component
                // is not the "last" and has to be disposed off
                if ( icm != null )
                {
                    icm.dispose( ComponentConstants.DEACTIVATION_REASON_CONFIGURATION_DELETED );
                }
            }
        }
    }


    /**
     * Configures a component with the given configuration. This configuration
     * update may happen in various situations:
     * <ul>
     * <li>The <code>pid</code> equals the component name. Hence we have a
     * singleton configuration for the single component held by this holder</li>
     * <li>The configuration is a factory configuration and is the first
     * configuration provided. In this case the single component is provided
     * with the configuration and also stored in the map.</li>
     * <li>The configuration is a factory configuration but not the first. In
     * this case a new component is created, configured and stored in the map</li>
     * </ul>
     */
    public void configurationUpdated( final String pid, final Dictionary props )
    {
        if ( pid.equals( getComponentName() ) )
        {
            // singleton configuration has pid equal to component name
            m_singleComponent.reconfigure( props );
        }
        else
        {
            // factory configuration update or created
            final ImmediateComponentManager icm = getComponentManager( pid );
            if ( icm != null )
            {
                // factory configuration updated for existing component instance
                icm.reconfigure( props );
            }
            else
            {
                // factory configuration created
                final ImmediateComponentManager newIcm;
                if ( !m_singleComponent.hasConfiguration() )
                {
                    // configure the single instance if this is not configured
                    newIcm = m_singleComponent;
                }
                else
                {
                    // otherwise create a new instance to provide the config to
                    newIcm = createComponentManager();
                }

                // configure the component
                newIcm.reconfigure( props );

                // enable the component if it is initially enabled
                if ( m_enabled && getComponentMetadata().isEnabled() )
                {
                    newIcm.enable();
                }

                // store the component in the map
                putComponentManager( pid, newIcm );
            }
        }
    }


    public void enableComponents()
    {
        final ImmediateComponentManager[] cms = getComponentManagers( false );
        if ( cms == null )
        {
            m_singleComponent.enable();
        }
        else
        {
            for ( int i = 0; i < cms.length; i++ )
            {
                cms[i].enable();
            }
        }

        m_enabled = true;
    }


    public void disableComponents()
    {
        final ImmediateComponentManager[] cms = getComponentManagers( false );
        if ( cms == null )
        {
            m_singleComponent.disable();
        }
        else
        {
            for ( int i = 0; i < cms.length; i++ )
            {
                cms[i].disable();
            }
        }
    }


    public void disposeComponents( final int reason )
    {
        // FELIX-1733: get a copy of the single component and clear
        // the field to prevent recreation in disposed(ICM)
        final ImmediateComponentManager singleComponent = m_singleComponent;
        m_singleComponent = null;

        final ImmediateComponentManager[] cms = getComponentManagers( true );
        if ( cms == null )
        {
            singleComponent.dispose( reason );
        }
        else
        {
            for ( int i = 0; i < cms.length; i++ )
            {
                cms[i].dispose( reason );
            }
        }
    }


    public void disposed( ImmediateComponentManager component )
    {
        // ensure the component is removed from the components map
        synchronized ( m_components )
        {
            if ( !m_components.isEmpty() )
            {
                for ( Iterator vi = m_components.values().iterator(); vi.hasNext(); )
                {
                    if ( component == vi.next() )
                    {
                        vi.remove();
                        break;
                    }
                }
            }
        }

        // if the component is the single component, we have to replace it
        // by another entry in the map
        if ( component == m_singleComponent )
        {
            synchronized ( m_components )
            {
                if ( m_components.isEmpty() )
                {
                    // now what ??
                    // is it correct to create a new manager ???
                    m_singleComponent = createComponentManager();
                }
                else
                {
                    m_singleComponent = ( ImmediateComponentManager ) m_components.values().iterator().next();
                }
            }
        }
    }


    //---------- internal

    private ImmediateComponentManager getComponentManager( String pid )
    {
        synchronized ( m_components )
        {
            return ( ImmediateComponentManager ) m_components.get( pid );
        }
    }


    private ImmediateComponentManager removeComponentManager( String pid )
    {
        synchronized ( m_components )
        {
            return ( ImmediateComponentManager ) m_components.remove( pid );
        }
    }


    private void putComponentManager( String pid, ImmediateComponentManager componentManager )
    {
        synchronized ( m_components )
        {
            m_components.put( pid, componentManager );
        }
    }


    /**
     * Returns all components from the map, optionally also removing them
     * from the map. If there are no components in the map, <code>null</code>
     * is returned.
     */
    private ImmediateComponentManager[] getComponentManagers( final boolean clear )
    {
        synchronized ( m_components )
        {
            // fast exit if there is no component in the map
            if ( m_components.isEmpty() )
            {
                return null;
            }

            final ImmediateComponentManager[] cm = new ImmediateComponentManager[m_components.size()];
            m_components.values().toArray( cm );

            if ( clear )
            {
                m_components.clear();
            }

            return cm;
        }
    }
}
