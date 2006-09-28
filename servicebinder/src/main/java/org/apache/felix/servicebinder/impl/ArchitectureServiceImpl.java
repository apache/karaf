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
package org.apache.felix.servicebinder.impl;

import org.apache.felix.servicebinder.InstanceReference;
import org.apache.felix.servicebinder.Lifecycle;
import org.apache.felix.servicebinder.architecture.ArchitectureService;
import org.apache.felix.servicebinder.architecture.DependencyChangeEvent;
import org.apache.felix.servicebinder.architecture.Instance;
import org.apache.felix.servicebinder.architecture.InstanceChangeEvent;
import org.apache.felix.servicebinder.architecture.ServiceBinderListener;

import java.util.List;
import java.util.ArrayList;

/**
 * Class that implements the architecture service
 * an object of this class is created when the
 * service binder bundle is activated
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ArchitectureServiceImpl implements ArchitectureService, Lifecycle
{
    /**
     * 
     * @uml.property name="m_listeners"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private ServiceBinderListener m_listeners = null;

    /**
     * 
     * @uml.property name="m_ref"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private static ArchitectureServiceImpl m_ref = null;

    private static List m_instanceReferences = new ArrayList();

    public ArchitectureServiceImpl() throws Exception
    {
        if(m_ref == null)
        {
            m_ref = this;
        }
    }

    public static InstanceReference findInstanceReference(Object obj)
    {
        Object[] refs=m_instanceReferences.toArray();

        for(int i=0; i<refs.length ;i++)
        {
            InstanceReference current = (InstanceReference) refs [i];
            if(current.getObject( )== obj)
            {
                return current;
            }
        }

        return null;
    }

    public static void addInstanceReference(InstanceReference ref)
    {
        m_instanceReferences.add(ref);
    }

    public static void removeInstanceReference(InstanceReference ref)
    {
        m_instanceReferences.remove(ref);
    }

    public Instance[] getInstances()
    {
        Instance instances[]=new Instance[m_instanceReferences.size()];
        instances = (Instance [])m_instanceReferences.toArray(instances);

        return instances;
    }

    public static ArchitectureServiceImpl getReference()
    {
        return m_ref;
    }

    public void activate()
    {
    }

    synchronized public void deactivate()
    {
        m_ref = null;
        m_listeners = null; //new EventListenerList();
    }

    /**
     * Add a service binder listener
    **/
    public void addServiceBinderListener(ServiceBinderListener listener)
    {
        //m_listeners.add(ServiceBinderListener.class, listener);
        m_listeners = ArchitectureEventMulticaster.add(m_listeners, listener);
    }

    /**
     * Remove a service binder listener
    **/
    public void removeServiceBinderListener(ServiceBinderListener listener)
    {
        //m_listeners.remove(ServiceBinderListener.class, listener);
        m_listeners = ArchitectureEventMulticaster.remove(m_listeners, listener);
    }

    /**
     * Fires an event when an instance has changed
    **/
    public void fireInstanceChangeEvent(InstanceChangeEvent evt)
    {
        try
        {
            if (m_listeners != null)
            {
                m_listeners.instanceReferenceChanged(evt);
            }
        }
        catch(Exception ex)
        {
            // Ignore any exception
        }
    }

    /**
     * Fires an event when a dependency has changed
    **/
    public void fireDependencyChangeEvent(DependencyChangeEvent evt)
    {
        try
        {
            if (m_listeners != null)
            {
                m_listeners.dependencyChanged(evt);
            }
        }
        catch(Exception ex)
        {
            // Ignore any exception
        }
    }
    


}



