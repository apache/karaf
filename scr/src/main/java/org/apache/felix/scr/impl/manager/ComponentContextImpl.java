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

import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;


/**
 * Implementation for the ComponentContext interface
 *
 */
public class ComponentContextImpl implements ComponentContext, ComponentInstance
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

    public final Dictionary getProperties()
    {
        // 112.11.3.5 The Dictionary is read-only and cannot be modified
        return new ReadOnlyDictionary( m_componentManager.getProperties() );
    }


    public Object locateService( String name )
    {
        DependencyManager dm = m_componentManager.getDependencyManager( name );
        return ( dm != null ) ? dm.getService() : null;
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
        return this;
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


    //---------- ComponentInstance interface ------------------------------

    public Object getInstance()
    {
        return getComponentManager().getInstance();
    }


    public void dispose()
    {
        getComponentManager().dispose();
    }
}
