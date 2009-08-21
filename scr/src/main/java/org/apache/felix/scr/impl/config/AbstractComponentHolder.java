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


import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.manager.DelayedComponentManager;
import org.apache.felix.scr.impl.manager.ImmediateComponentManager;
import org.apache.felix.scr.impl.manager.ServiceFactoryComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;


abstract class AbstractComponentHolder implements ComponentHolder
{

    private final BundleComponentActivator m_activator;

    private final ComponentMetadata m_componentMetadata;


    public AbstractComponentHolder( final BundleComponentActivator activator, final ComponentMetadata metadata )
    {
        this.m_activator = activator;
        this.m_componentMetadata = metadata;
    }


    protected ImmediateComponentManager createComponentManager()
    {

        ImmediateComponentManager manager;
        if ( m_componentMetadata.isFactory() )
        {
            throw new IllegalArgumentException( "Cannot create component factory for " + m_componentMetadata.getName() );
        }
        else if ( m_componentMetadata.isImmediate() )
        {
            manager = new ImmediateComponentManager( m_activator, this, m_componentMetadata );
        }
        else if ( m_componentMetadata.getServiceMetadata() != null )
        {
            if ( m_componentMetadata.getServiceMetadata().isServiceFactory() )
            {
                manager = new ServiceFactoryComponentManager( m_activator, this, m_componentMetadata );
            }
            else
            {
                manager = new DelayedComponentManager( m_activator, this, m_componentMetadata );
            }
        }
        else
        {
            // if we get here, which is not expected after all, we fail
            throw new IllegalArgumentException( "Cannot create a component manager for "
                + m_componentMetadata.getName() );
        }

        return manager;
    }


    public final BundleComponentActivator getActivator()
    {
        return m_activator;
    }


    public final ComponentMetadata getComponentMetadata()
    {
        return m_componentMetadata;
    }


    protected final String getComponentName()
    {
        return getComponentMetadata().getName();
    }
}
