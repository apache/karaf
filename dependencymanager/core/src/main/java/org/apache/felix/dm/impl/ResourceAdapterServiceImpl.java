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
package org.apache.felix.dm.impl;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.service.Service;
import org.apache.felix.dm.service.ServiceStateListener;

/**
 * Resource adapter service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual resource adapter service implementation.
 */
public class ResourceAdapterServiceImpl extends FilterService
{
    /**
     * Creates a new Resource Adapter Service implementation.
     * @param dm the dependency manager used to create our internal adapter service
     */
    public ResourceAdapterServiceImpl(DependencyManager dm, String resourceFilter, boolean propagate)    {
        super(dm.createService()); // This service will be filtered by our super class, allowing us to take control.
        m_service.setImplementation(new ResourceAdapterImpl(resourceFilter, propagate))
                 .add(dm.createResourceDependency()
                      .setFilter(resourceFilter)
                      .setAutoConfig(false)
                      .setCallbacks("added", "removed"));
    }
    
    public class ResourceAdapterImpl extends AbstractDecorator {
        private final String m_resourceFilter;
        private final boolean m_propagate;

        public ResourceAdapterImpl(String resourceFilter, boolean propagate) {
            m_resourceFilter = resourceFilter;
            m_propagate = propagate;
        }

        public Service createService(Object[] properties) {
            Resource resource = (Resource) properties[0]; 
            Properties props = new Properties();
            if (m_serviceProperties != null) {
                Enumeration e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    Object key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            List dependencies = m_service.getDependencies();
            // the first dependency is always the dependency on the resource, which
            // will be replaced with a more specific dependency below
            dependencies.remove(0);
            Service service = m_manager.createService()
                .setInterface(m_serviceInterfaces, props)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(dependencies)
                .add(m_manager.createResourceDependency()
                     .setResource(resource)
                     .setPropagate(m_propagate)
                     .setCallbacks(null, "changed", null)
                     .setAutoConfig(true)
                     .setRequired(true));         
            for (int i = 0; i < m_stateListeners.size(); i ++) {
                service.addStateListener((ServiceStateListener) m_stateListeners.get(i));
            }
            return service;
        }
    }
}
