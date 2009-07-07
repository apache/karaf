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
package org.apache.felix.ipojo.composite;

import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.FactoryStateListener;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Bridge representing a Factory inside a composition.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FactoryProxy implements Factory {

    /**
     * Delegated factory.
     */
    private Factory m_delegate;

    /**
     * Destination context.
     */
    private ServiceContext m_context;

    /**
     * Constructor.
     * @param fact : the targeted factory.
     * @param svcContext : the service context to target.
     */
    public FactoryProxy(Factory fact, ServiceContext svcContext) {
        m_delegate = fact;
        m_context = svcContext;
    }

    /**
     * Create an instance manager (i.e. component type instance).
     * @param configuration : the configuration properties for this component.
     * @return the created instance manager.
     * @throws UnacceptableConfiguration : when a given configuration is not valid.
     * @throws MissingHandlerException : occurs when the creation failed due to a missing handler (the factory should be invalid)
     * @throws ConfigurationException : occurs when the creation failed due to a configuration issue
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException,
            ConfigurationException {
        return m_delegate.createComponentInstance(configuration, m_context);
    }

    /**
     * Create an instance manager (i.e. component type instance). This has these service interaction in the scope given in argument.
     * @param configuration : the configuration properties for this component.
     * @param serviceContext : the service context of the component.
     * @return the created instance manager.
     * @throws UnacceptableConfiguration : when the given configuration is not valid.
     * @throws MissingHandlerException : when at least one handler is missing.
     * @throws ConfigurationException : when an issue occurs during the oconfiguration of the instance.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary, org.apache.felix.ipojo.ServiceContext)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration,
            MissingHandlerException, ConfigurationException {
        return m_delegate.createComponentInstance(configuration, serviceContext);
    }

    /**
     * Get the component type information containing provided service, configuration properties ...
     * @return the component type information.
     * @see org.apache.felix.ipojo.Factory#getDescription()
     */
    public Element getDescription() {
        return m_delegate.getDescription();
    }

    /**
     * Return the factory name.
     * @return the name of the factory.
     * @see org.apache.felix.ipojo.Factory#getName()
     */
    public String getName() {
        return m_delegate.getName();
    }

    /**
     * Check if the given configuration is acceptable as a configuration of a component instance.
     * @param conf : the configuration to test
     * @return true if the configuration is acceptable
     * @see org.apache.felix.ipojo.Factory#isAcceptable(java.util.Dictionary)
     */
    public boolean isAcceptable(Dictionary conf) {
        return m_delegate.isAcceptable(conf);
    }

    /**
     * Reconfigure an instance already created. This configuration need to have the name property to identify the instance.
     * @param conf : the configuration to reconfigure the instance.
     * @throws UnacceptableConfiguration : if the given configuration is not consistent for the targeted instance.
     * @throws MissingHandlerException : when at least one handler is missing
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException {
        m_delegate.reconfigure(conf);
    }

    /**
     * Add a factory listener.
     * @param listener : the listener to add.
     * @see org.apache.felix.ipojo.Factory#addFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void addFactoryStateListener(FactoryStateListener listener) {
        m_delegate.addFactoryStateListener(listener);

    }

    public List getMissingHandlers() {
        return m_delegate.getMissingHandlers();
    }

    public List getRequiredHandlers() {
        return m_delegate.getRequiredHandlers();
    }

    /**
     * Remove a service listener.
     * @param listener : the listener to remove
     * @see org.apache.felix.ipojo.Factory#removeFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void removeFactoryStateListener(FactoryStateListener listener) {
        m_delegate.removeFactoryStateListener(listener);

    }

    public ComponentTypeDescription getComponentDescription() {
        return m_delegate.getComponentDescription();
    }

    public String getClassName() {
        return m_delegate.getClassName();
    }

    public int getState() {
        return m_delegate.getState();
    }

    public BundleContext getBundleContext() {
        return m_delegate.getBundleContext();
    }

    public String getVersion() {
        return m_delegate.getVersion();
    }

}
