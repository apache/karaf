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
package org.apache.felix.dependencymanager.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dependencymanager.Dependency;
import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.Service;
import org.apache.felix.dependencymanager.ServiceStateListener;
import org.osgi.framework.ServiceRegistration;

/**
 * Service implementation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceImpl implements Service
{
    private org.apache.felix.dm.service.Service m_delegate;
    private Map m_stateListeners = new HashMap();
    private ArrayList m_dependencies = new ArrayList();

    public ServiceImpl(DependencyManager dm)
    {
        org.apache.felix.dm.DependencyManager dmDelegate = (org.apache.felix.dm.DependencyManager) dm.getDelegate();
        m_delegate = dmDelegate.createService();
    }

    public Service add(Dependency dependency)
    {
        synchronized (this)
        {
            m_dependencies.add(dependency);
        }
        if (dependency instanceof ServiceDependencyImpl)
        {
            m_delegate.add(((org.apache.felix.dependencymanager.impl.ServiceDependencyImpl) dependency).getDelegate());

        }
        else if (dependency instanceof ConfigurationDependencyImpl)
        {
            m_delegate.add(((org.apache.felix.dependencymanager.impl.ConfigurationDependencyImpl) dependency).getDelegate());
        }
        else
        {
            throw new IllegalArgumentException("dependency type not supported: " + dependency);
        }
        return this;
    }

    public Service remove(Dependency dependency)
    {
        if (dependency instanceof ServiceDependencyImpl)
        {
            m_delegate.remove(((org.apache.felix.dependencymanager.impl.ServiceDependencyImpl) dependency).getDelegate());
        }
        else if (dependency instanceof ConfigurationDependencyImpl)
        {
            m_delegate.remove(((org.apache.felix.dependencymanager.impl.ConfigurationDependencyImpl) dependency).getDelegate());
        }
        else
        {
            throw new IllegalArgumentException("dependency type not supported: " + dependency);
        }
        return this;
    }

    public void addStateListener(final ServiceStateListener listener)
    {
        org.apache.felix.dm.service.ServiceStateListener wrappedListener = new org.apache.felix.dm.service.ServiceStateListener()
        {
            public void started(org.apache.felix.dm.service.Service service)
            {
                listener.started(ServiceImpl.this);
            }

            public void starting(org.apache.felix.dm.service.Service service)
            {
                listener.starting(ServiceImpl.this);
            }

            public void stopped(org.apache.felix.dm.service.Service service)
            {
                listener.stopped(ServiceImpl.this);
            }

            public void stopping(org.apache.felix.dm.service.Service service)
            {
                listener.stopping(ServiceImpl.this);
            }
        };
        synchronized (this)
        {
            m_stateListeners.put(listener, wrappedListener);
        }
        m_delegate.addStateListener(wrappedListener);
    }

    public void removeStateListener(ServiceStateListener listener)
    {
        org.apache.felix.dm.service.ServiceStateListener wrappedListener = null;
        synchronized (m_stateListeners)
        {
            wrappedListener = (org.apache.felix.dm.service.ServiceStateListener) m_stateListeners.remove(listener);
        }
        if (wrappedListener != null)
        {
            m_delegate.removeStateListener(wrappedListener);
        }
    }

    public List getDependencies()
    {
        synchronized (m_dependencies)
        {
            return (List) m_dependencies.clone();
        }
    }

    public Object getService()
    {
        return m_delegate.getService();
    }

    public Dictionary getServiceProperties()
    {
        return m_delegate.getServiceProperties();
    }

    public ServiceRegistration getServiceRegistration()
    {
        return m_delegate.getServiceRegistration();
    }

    public Service setCallbacks(String init, String start, String stop, String destroy)
    {
        m_delegate.setCallbacks(init, start, stop, destroy);
        return this;
    }

    public Service setComposition(Object instance, String getMethod)
    {
        m_delegate.setComposition(instance, getMethod);
        return this;
    }

    public Service setComposition(String getMethod)
    {
        m_delegate.setComposition(getMethod);
        return this;
    }

    public Service setFactory(Object factory, String createMethod)
    {
        m_delegate.setFactory(factory, createMethod);
        return this;
    }

    public Service setFactory(String createMethod)
    {
        m_delegate.setFactory(createMethod);
        return this;
    }

    public Service setImplementation(Object implementation)
    {
        m_delegate.setImplementation(implementation);
        return this;
    }

    public Service setInterface(String serviceName, Dictionary properties)
    {
        m_delegate.setInterface(serviceName, properties);
        return this;
    }

    public Service setInterface(String[] serviceNames, Dictionary properties)
    {
        m_delegate.setInterface(serviceNames, properties);
        return this;
    }

    public void setServiceProperties(Dictionary serviceProperties)
    {
        m_delegate.setServiceProperties(serviceProperties);
    }

    public void start()
    {
        m_delegate.start();
    }

    public void stop()
    {
        m_delegate.stop();
    }
}
