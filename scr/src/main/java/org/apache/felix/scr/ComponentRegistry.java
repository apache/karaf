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
package org.apache.felix.scr;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentException;


/**
 * The <code>ComponentRegistry</code> TODO
 *
 * @author fmeschbe
 */
public class ComponentRegistry implements ConfigurationListener
{

    // Known and registered ComponentManager instances
    private Map m_componentNames;

    // component id counter
    private long m_componentCounter;

    // the service registration of the ConfigurationListener service
    private ServiceRegistration registration;


    ComponentRegistry( BundleContext context )
    {
        m_componentNames = new HashMap();
        m_componentCounter = -1;

        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Service Component Configuration Support" );
        props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );
        registration = context.registerService( ConfigurationListener.class.getName(), this, props );
    }


    void dispose()
    {
        if ( registration != null )
        {
            registration.unregister();
            registration = null;
        }
    }


    //---------- ConfigurationListener ----------------------------------------

    public void configurationEvent( ConfigurationEvent configEvent )
    {
        String pid = configEvent.getPid();
        ComponentManager cm = this.getComponent( pid );
        if ( cm != null )
        {
            cm.reconfigure();
        }
    }


    //---------- ComponentManager registration support ------------------------

    long createComponentId()
    {
        m_componentCounter++;
        return m_componentCounter;
    }


    void checkComponentName( String name )
    {
        if ( m_componentNames.containsKey( name ) )
        {
            throw new ComponentException( "The component name '" + name + "' has already been registered." );
        }

        // reserve the name
        m_componentNames.put( name, name );
    }


    void registerComponent( String name, ComponentManager component )
    {
        // only register the component if there is a registration for it !
        if ( !name.equals( m_componentNames.get( name ) ) )
        {
            // this is not expected if all works ok
            throw new ComponentException( "The component name '" + name + "' has already been registered." );
        }

        m_componentNames.put( name, component );
    }


    ComponentManager getComponent( String name )
    {
        Object entry = m_componentNames.get( name );

        // only return the entry if non-null and not a reservation
        if ( entry instanceof ComponentManager )
        {
            return ( ComponentManager ) entry;
        }

        return null;
    }


    void unregisterComponent( String name )
    {
        m_componentNames.remove( name );
    }
}
