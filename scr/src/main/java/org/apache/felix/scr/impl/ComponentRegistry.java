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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.ComponentFactoryImpl;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentException;


/**
 * The <code>ComponentRegistry</code> TODO
 *
 * @author fmeschbe
 */
public class ComponentRegistry implements ScrService, ConfigurationListener
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
        m_registration = context.registerService( new String[]
            { ScrService.class.getName(), ConfigurationListener.class.getName() }, this, props );
    }


    void dispose()
    {
        if ( m_registration != null )
        {
            m_registration.unregister();
            m_registration = null;
        }
    }


    //---------- ScrService interface

    public Component[] getComponents()
    {
        if ( m_componentsById.isEmpty() )
        {
            return null;
        }

        return ( org.apache.felix.scr.Component[] ) m_componentsById.values().toArray(
            new Component[m_componentsById.size()] );
    }


    public Component[] getComponents( Bundle bundle )
    {
        Component[] all = getComponents();
        if ( all == null || all.length == 0 )
        {
            return null;
        }

        // compare the bundle by its id
        long bundleId = bundle.getBundleId();

        // scan the components for the the desired components
        List perBundle = new ArrayList();
        for ( int i = 0; i < all.length; i++ )
        {
            if ( all[i].getBundle().getBundleId() == bundleId )
            {
                perBundle.add( all[i] );
            }
        }

        // nothing to return
        if ( perBundle.isEmpty() )
        {
            return null;
        }

        return ( org.apache.felix.scr.Component[] ) perBundle.toArray( new Component[perBundle.size()] );
    }


    public Component getComponent( long componentId )
    {
        return ( Component ) m_componentsById.get( new Long( componentId ) );
    }


    //---------- ConfigurationListener

    public void configurationEvent( ConfigurationEvent event )
    {
        final String pid = event.getPid();
        final String factoryPid = event.getFactoryPid();

        final AbstractComponentManager cm;
        if ( factoryPid == null )
        {
            cm = getComponent( pid );
        }
        else
        {
            cm = getComponent( factoryPid );
        }

        if (cm == null) {
            // this configuration is not for a SCR component
            return;
        }

        switch ( event.getType() )
        {
            case ConfigurationEvent.CM_DELETED:
                if ( cm instanceof ImmediateComponentManager )
                {
                    ( ( ImmediateComponentManager ) cm ).reconfigure( null );
                }
                else if ( cm instanceof ComponentFactoryImpl )
                {
                    ( ( ComponentFactoryImpl ) cm ).deleted( pid );
                }
                break;
            case ConfigurationEvent.CM_UPDATED:
                BundleContext ctx = cm.getActivator().getBundleContext();
                Dictionary dict = getConfiguration( event.getReference(), ctx, pid );
                if ( dict != null )
                {
                    if ( cm instanceof ImmediateComponentManager )
                    {
                        ( ( ImmediateComponentManager ) cm ).reconfigure( dict );
                    }
                    else if ( cm instanceof ComponentFactoryImpl )
                    {
                        ( ( ComponentFactoryImpl ) cm ).updated( pid, dict );
                    }
                }
                break;
        }

    }


    private Dictionary getConfiguration( final ServiceReference cfgAdmin, final BundleContext ctx, final String pid )
    {
        final ConfigurationAdmin ca = ( ConfigurationAdmin ) ctx.getService( cfgAdmin );
        if ( ca != null )
        {
            try
            {
                final Configuration cfg = ca.getConfiguration( pid );
                if ( ctx.getBundle().getLocation().equals( cfg.getBundleLocation() ) )
                {
                    return cfg.getProperties();
                }
            }
            catch ( IOException ioe )
            {
                // TODO: log
            }
            finally
            {
                ctx.ungetService( cfgAdmin );
            }
        }

        return null;
    }


    public Configuration getConfiguration( final BundleContext ctx, final String pid )
    {
        final String filter = "(service.pid=" + pid + ")";
        Configuration[] cfg = getConfigurationInternal( ctx, filter );
        return ( cfg == null || cfg.length == 0 ) ? null : cfg[0];
    }


    public Configuration[] getConfigurations( final BundleContext ctx, final String factoryPid )
    {
        final String filter = "(service.factoryPid=" + factoryPid + ")";
        return getConfigurationInternal( ctx, filter );
    }


    private Configuration[] getConfigurationInternal( final BundleContext ctx, final String filter )
    {
        final ServiceReference cfgAdmin = ctx.getServiceReference( ConfigurationAdmin.class.getName() );
        final ConfigurationAdmin ca = ( ConfigurationAdmin ) ctx.getService( cfgAdmin );
        if ( ca != null )
        {
            try
            {
                return ca.listConfigurations( filter );
            }
            catch ( IOException ioe )
            {
                // TODO: log
            }
            catch ( InvalidSyntaxException ise )
            {
                // TODO: log
            }
            finally
            {
                ctx.ungetService( cfgAdmin );
            }
        }

        return null;
    }


    //---------- ComponentManager registration support

    public long createComponentId()
    {
        m_componentCounter++;
        return m_componentCounter;
    }


    public void checkComponentName( String name )
    {
        if ( m_componentsByName.containsKey( name ) )
        {
            String message = "The component name '" + name + "' has already been registered";

            Object co = m_componentsByName.get( name );
            if ( co instanceof AbstractComponentManager )
            {
                AbstractComponentManager c = ( AbstractComponentManager ) co;
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


    void registerComponent( String name, AbstractComponentManager component )
    {
        // only register the component if there is a m_registration for it !
        if ( !name.equals( m_componentsByName.get( name ) ) )
        {
            // this is not expected if all works ok
            throw new ComponentException( "The component name '" + name + "' has already been registered." );
        }

        m_componentsByName.put( name, component );
        m_componentsById.put( new Long( component.getId() ), component );
    }


    AbstractComponentManager getComponent( String name )
    {
        Object entry = m_componentsByName.get( name );

        // only return the entry if non-null and not a reservation
        if ( entry instanceof AbstractComponentManager )
        {
            return ( AbstractComponentManager ) entry;
        }

        return null;
    }


    void unregisterComponent( String name )
    {
        Object entry = m_componentsByName.remove( name );
        if ( entry instanceof AbstractComponentManager )
        {
            Long id = new Long( ( ( AbstractComponentManager ) entry ).getId() );
            m_componentsById.remove( id );
        }
    }
}
