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


import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;


/**
 * Implementation for the ComponentContext interface
 *
 */
class ComponentContextImpl implements ComponentContext
{

    private AbstractComponentManager m_componentManager;


    ComponentContextImpl( AbstractComponentManager componentManager )
    {
        m_componentManager = componentManager;
    }


    protected AbstractComponentManager getComponentManager()
    {
        return m_componentManager;
    }

    public Dictionary getProperties()
    {
        // 112.11.3.5 The Dictionary is read-only and cannot be modified
        return new ReadOnlyDictionary( m_componentManager.getProperties() );
    }


    public Object locateService( String name )
    {
        DependencyManager dm = m_componentManager.getDependencyManager( name );
        if ( dm == null )
        {
            return null;
        }

        ServiceReference selectedRef;
        if ( dm.size() == 1 )
        {
            // short cut for single bound service
            selectedRef = dm.getServiceReference();
        }
        else
        {
            // is it correct to assume an ordered bound services set ?
            int maxRanking = Integer.MIN_VALUE;
            long minId = Long.MAX_VALUE;
            selectedRef = null;

            ServiceReference[] refs = dm.getFrameworkServiceReferences();
            for ( int i = 0; refs != null && i < refs.length; i++ )
            {
                ServiceReference ref = refs[i];
                Integer rank = ( Integer ) ref.getProperty( Constants.SERVICE_RANKING );
                int ranking = ( rank == null ) ? Integer.MIN_VALUE : rank.intValue();
                long id = ( ( Long ) ref.getProperty( Constants.SERVICE_ID ) ).longValue();
                if ( maxRanking < ranking || ( maxRanking == ranking && id < minId ) )
                {
                    maxRanking = ranking;
                    minId = id;
                    selectedRef = ref;
                }
            }
        }

        // this is not realistic, as at least one service is available
        // whose service id is smaller than Long.MAX_VALUE, still be sure
        if ( selectedRef == null )
        {
            return null;
        }

        // return the service for the selected reference
        return dm.getService( selectedRef );
    }


    public Object locateService( String name, ServiceReference ref )
    {
        DependencyManager dm = m_componentManager.getDependencyManager( name );
        return ( dm != null ) ? dm.getService( ref ) : null;
    }


    public Object[] locateServices( String name )
    {
        DependencyManager dm = m_componentManager.getDependencyManager( name );
        return ( dm != null ) ? dm.getServices() : null;
    }


    public BundleContext getBundleContext()
    {
        return m_componentManager.getActivator().getBundleContext();
    }


    public Bundle getUsingBundle()
    {
        return null;
    }


    public ComponentInstance getComponentInstance()
    {
        return m_componentManager;
    }


    public void enableComponent( String name )
    {
        m_componentManager.getActivator().enableComponent( name );
    }


    public void disableComponent( String name )
    {
        m_componentManager.getActivator().disableComponent( name );
    }


    public ServiceReference getServiceReference()
    {
        return m_componentManager.getServiceReference();
    }
}
