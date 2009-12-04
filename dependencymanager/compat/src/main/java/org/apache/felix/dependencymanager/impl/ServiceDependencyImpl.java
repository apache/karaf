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

import org.apache.felix.dependencymanager.DependencyManager;
import org.apache.felix.dependencymanager.ServiceDependency;
import org.osgi.framework.ServiceReference;

public class ServiceDependencyImpl extends ServiceDependency
{
    org.apache.felix.dm.dependencies.ServiceDependency m_delegate;

    public ServiceDependencyImpl(DependencyManager dm)
    {
        org.apache.felix.dm.DependencyManager dmDelegate = (org.apache.felix.dm.DependencyManager) dm.getDelegate();
        m_delegate = dmDelegate.createServiceDependency();
    }

    public org.apache.felix.dm.dependencies.ServiceDependency getDelegate()
    {
        return m_delegate;
    }

    public ServiceDependency setService(Class serviceName)
    {
        m_delegate.setService(serviceName);
        return this;
    }

    public ServiceDependency setService(Class serviceName, String serviceFilter)
    {
        m_delegate.setService(serviceName, serviceFilter);
        return this;
    }

    public ServiceDependency setService(Class serviceName, ServiceReference serviceReference)
    {
        m_delegate.setService(serviceName, serviceReference);
        return this;
    }

    public ServiceDependency setDefaultImplementation(Object implementation)
    {
        m_delegate.setDefaultImplementation(implementation);
        return this;
    }

    public ServiceDependency setRequired(boolean required)
    {
        m_delegate.setRequired(required);
        return this;
    }

    public ServiceDependency setAutoConfig(boolean autoConfig)
    {
        m_delegate.setAutoConfig(autoConfig);
        return this;
    }

    public ServiceDependency setAutoConfig(String instanceName)
    {
        m_delegate.setAutoConfig(instanceName);
        return this;
    }

    public ServiceDependency setCallbacks(Object instance, String added, String changed,
        String removed)
    {
        m_delegate.setCallbacks(instance, added, changed, removed);
        return this;
    }

    public boolean isAvailable()
    {
        return m_delegate.isAvailable();
    }

    public boolean isRequired()
    {
        return m_delegate.isRequired();
    }
}
