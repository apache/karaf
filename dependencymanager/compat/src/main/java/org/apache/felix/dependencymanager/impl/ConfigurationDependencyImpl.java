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

import java.util.Dictionary;

import org.apache.felix.dependencymanager.ConfigurationDependency;
import org.apache.felix.dependencymanager.DependencyManager;

/**
 * Configuration dependency that can track the availability of a (valid) configuration.
 * To use it, specify a PID for the configuration. The dependency is always required,
 * because if it is not, it does not make sense to use the dependency manager. In that
 * scenario, simply register your service as a <code>ManagedService(Factory)</code> and
 * handle everything yourself. Also, only managed services are supported, not factories.
 * There are a couple of things you need to be aware of when implementing the
 * <code>updated(Dictionary)</code> method:
 * <ul>
 * <li>Make sure it throws a <code>ConfigurationException</code> when you get a
 * configuration that is invalid. In this case, the dependency will not change:
 * if it was not available, it will still not be. If it was available, it will
 * remain available and implicitly assume you keep working with your old
 * configuration.</li>
 * <li>This method will be called before all required dependencies are available.
 * Make sure you do not depend on these to parse your settings.</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependencyImpl extends ConfigurationDependency
{
    org.apache.felix.dm.dependencies.ConfigurationDependency m_delegate;

    public ConfigurationDependencyImpl(DependencyManager dm)
    {
        org.apache.felix.dm.DependencyManager dmDelegate = (org.apache.felix.dm.DependencyManager) dm.getDelegate();
        m_delegate = dmDelegate.createConfigurationDependency();
    }

    public org.apache.felix.dm.dependencies.ConfigurationDependency getDelegate()
    {
        return m_delegate;
    }

    public ConfigurationDependency setCallback(String callback)
    {
        m_delegate.setCallback(callback);
        return this;
    }

    /**
     * Sets the <code>service.pid</code> of the configuration you
     * are depending on.
     */
    public ConfigurationDependency setPid(String pid)
    {
        m_delegate.setPid(pid);
        return this;
    }

    /**
     * Sets propagation of the configuration properties to the service
     * properties. Any additional service properties specified directly
     * are merged with these.
     */
    public ConfigurationDependency setPropagate(boolean propagate)
    {
        m_delegate.setPropagate(propagate);
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
