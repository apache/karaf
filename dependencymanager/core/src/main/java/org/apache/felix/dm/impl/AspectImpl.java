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

public class AspectImpl extends AbstractDecorator {
	private volatile Service m_service;
	private final Class m_serviceInterface;
	private final String m_serviceFilter;
	private final Object m_aspectImplementation;
    private final Dictionary m_aspectProperties;
	
	public AspectImpl(Class serviceInterface, String serviceFilter, Object aspectImplementation, Dictionary properties) {
		m_serviceInterface = serviceInterface;
		m_serviceFilter = serviceFilter;
		m_aspectImplementation = aspectImplementation;
		m_aspectProperties = properties;
	}

    public Service createService(Object[] properties) {
        ServiceReference ref = (ServiceReference) properties[0]; 
        Object service = properties[1];
        Properties props = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            props.put(keys[i], ref.getProperty(keys[i]));
        }
        if (m_aspectProperties != null) {
            Enumeration e = m_aspectProperties.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                props.put(key, m_aspectProperties.get(key));
            }
        }
        return m_manager.createService()
            .setInterface(m_serviceInterface.getName(), props)
            .setImplementation(m_aspectImplementation)
            .add(m_service.getDependencies())
            .add(m_manager.createServiceDependency()
                .setService(m_serviceInterface, ref)
                .setRequired(true));
    }
}
