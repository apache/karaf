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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.dm.service.Service;
import org.osgi.framework.ServiceReference;

public class AdapterImpl extends AbstractDecorator {
	private volatile Service m_service;
    private final Class m_serviceInterface;
    private final String m_serviceFilter;
    private final Object m_adapterImplementation;
    private final Object m_adapterInterface;
    private final Dictionary m_adapterProperties;
	
    public AdapterImpl(Class serviceInterface, String serviceFilter, Object adapterImplementation, String adapterInterface, Dictionary adapterProperties) {
        m_serviceInterface = serviceInterface;
        m_serviceFilter = serviceFilter;
        m_adapterImplementation = adapterImplementation;
        m_adapterInterface = adapterInterface;
        m_adapterProperties = adapterProperties;
    }

    public AdapterImpl(Class serviceInterface, String serviceFilter, Object adapterImplementation, String[] adapterInterfaces, Dictionary adapterProperties) {
        m_serviceInterface = serviceInterface;
        m_serviceFilter = serviceFilter;
        m_adapterImplementation = adapterImplementation;
        m_adapterInterface = adapterInterfaces;
        m_adapterProperties = adapterProperties;
    }
	
    public Service createService(Object[] properties) {
        ServiceReference ref = (ServiceReference) properties[0]; 
        Object service = properties[1];
        Properties props = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            props.put(keys[i], ref.getProperty(keys[i]));
        }
        if (m_adapterProperties != null) {
            Enumeration e = m_adapterProperties.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                props.put(key, m_adapterProperties.get(key));
            }
        }
        if (m_adapterInterface instanceof String) {
            return m_manager.createService()
                .setInterface((String) m_adapterInterface, props)
                .setImplementation(m_adapterImplementation)
                .add(m_service.getDependencies())
                .add(m_manager.createServiceDependency()
                    .setService(m_serviceInterface, ref)
                    .setRequired(true)
                );
        }
        else {
            return m_manager.createService()
            .setInterface((String[]) m_adapterInterface, props)
            .setImplementation(m_adapterImplementation)
            .add(m_service.getDependencies())
            .add(m_manager.createServiceDependency()
                .setService(m_serviceInterface, ref)
                .setRequired(true)
            );
        }
	}
}
