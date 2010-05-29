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


import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;


/**
 * The <code>DelayedComponentManager</code> TODO
 */
public class DelayedComponentManager extends ImmediateComponentManager implements ServiceFactory
{

    // keep the using bundles as reference "counters" for instance deactivation
    private final Object m_useCountLock;
    private int m_useCount;


    /**
     * @param activator
     * @param metadata
     */
    public DelayedComponentManager( BundleComponentActivator activator, ComponentHolder componentHolder,
        ComponentMetadata metadata )
    {
        super( activator, componentHolder, metadata );
        this.m_useCountLock = new Object();
        this.m_useCount = 0;
    }


    protected boolean createComponent()
    {
        // nothing to do here for a delayed component, will be done in the
        // getService method for the first bundle acquiring the component
        return true;
    }


    protected void deleteComponent( int reason )
    {
        // only have to delete, if there is actually an instance
        if ( getInstance() != null )
        {
            super.deleteComponent( reason );
        }

        // ensure the refence set is also clear
        m_useCount = 0;
    }


    protected Object getService()
    {
        return this;
    }


    //---------- ServiceFactory interface -------------------------------------

    public synchronized Object getService( Bundle bundle, ServiceRegistration sr )
    {
        synchronized ( m_useCountLock )
        {
            m_useCount++;
            return state().getService( this );
        }
    }


    protected boolean createRealComponent()
    {
        return super.createComponent();
    }


    public void ungetService( Bundle bundle, ServiceRegistration sr, Object service )
    {
        synchronized ( m_useCountLock )
        {
            // the framework should not call ungetService more than it calls
            // calls getService. Still, we want to be sure to not go below zero
            if ( m_useCount > 0 )
            {
                m_useCount--;

                // unget the service instance if no bundle is using it
                // any longer
                if ( m_useCount == 0 )
                {
                    state().ungetService( this );
                }
            }
        }
    }
}
