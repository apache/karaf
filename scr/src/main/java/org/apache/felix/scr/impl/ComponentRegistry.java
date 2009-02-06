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


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentException;


/**
 * The <code>ComponentRegistry</code> TODO
 *
 * @author fmeschbe
 */
public class ComponentRegistry implements ScrService
{

    // Known and registered ComponentManager instances
    private Map m_componentsByName;

    // components registered by their component ID
    private Map m_componentsById;

    // component id counter
    private long m_componentCounter;

    // the service m_registration of the ConfigurationListener service
    private ServiceRegistration m_registration;


    ComponentRegistry( BundleContext context )
    {
        m_componentsByName = new HashMap();
        m_componentsById = new HashMap();
        m_componentCounter = -1;

        // register as ScrService
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Declarative Services Management Agent" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        m_registration = context.registerService( ScrService.class.getName(), this, props );
    }


    void dispose()
    {
        if ( m_registration != null )
        {
            m_registration.unregister();
            m_registration = null;
        }
    }


    //---------- ScrService interface -----------------------------------------


    public Component[] getComponents()
    {
        if (m_componentsById.isEmpty()) {
            return null;
        }

        return (org.apache.felix.scr.Component[] ) m_componentsById.values().toArray( new Component[m_componentsById.size()] );
    }

    public Component[] getComponents( Bundle bundle )
    {
        Component[] all = getComponents();
        if (all == null || all.length == 0) {
            return null;
        }

        // compare the bundle by its id
        long bundleId = bundle.getBundleId();

        // scan the components for the the desired components
        List perBundle = new ArrayList();
        for (int i=0; i < all.length; i++) {
            if (all[i].getBundle().getBundleId() == bundleId) {
                perBundle.add( all[i] );
            }
        }

        // nothing to return
        if (perBundle.isEmpty()) {
            return null;
        }

        return (org.apache.felix.scr.Component[] ) perBundle.toArray( new Component[perBundle.size()] );
    }


    public Component getComponent( long componentId )
    {
        return (Component) m_componentsById.get(new Long(componentId));
    }


    //---------- ComponentManager m_registration support ------------------------

    long createComponentId()
    {
        m_componentCounter++;
        return m_componentCounter;
    }


    void checkComponentName( String name )
    {
        if ( m_componentsByName.containsKey( name ) )
        {
            String message = "The component name '" + name + "' has already been registered";

            Object co = m_componentsByName.get( name );
            if ( co instanceof ComponentManager )
            {
                ComponentManager c = ( ComponentManager ) co;
                StringBuffer buf = new StringBuffer( message );
                buf.append( " by Bundle " ).append( c.getBundle().getBundleId() );
                if ( c.getBundle().getSymbolicName() != null )
                {
                    buf.append( " (" ).append( c.getBundle().getSymbolicName() ).append( ")" );
                }
                buf.append( " as Component " ).append( c.getId() );
                buf.append( " of Class " ).append( c.getClassName() );
                message = buf.toString();
            }

            throw new ComponentException( message );
        }

        // reserve the name
        m_componentsByName.put( name, name );
    }


    void registerComponent( String name, ComponentManager component )
    {
        // only register the component if there is a m_registration for it !
        if ( !name.equals( m_componentsByName.get( name ) ) )
        {
            // this is not expected if all works ok
            throw new ComponentException( "The component name '" + name + "' has already been registered." );
        }

        m_componentsByName.put( name, component );
        m_componentsById.put( new Long(component.getId()), component );
    }


    ComponentManager getComponent( String name )
    {
        Object entry = m_componentsByName.get( name );

        // only return the entry if non-null and not a reservation
        if ( entry instanceof ComponentManager )
        {
            return ( ComponentManager ) entry;
        }

        return null;
    }


    void unregisterComponent( String name )
    {
        Object entry = m_componentsByName.remove( name );
        if ( entry instanceof ComponentManager )
        {
            Long id = new Long( ( ( ComponentManager ) entry ).getId() );
            m_componentsById.remove( id );
        }
    }
}
