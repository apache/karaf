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
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class AspectImpl extends AbstractDecorator {
	private volatile Service m_service;
	private final Class m_serviceInterface;
	private final String m_serviceFilter;
	private final Object m_aspectImplementation;
    private final Dictionary m_aspectProperties;
    private final Object m_factory;
    private final String m_factoryCreateMethod;
    private final int m_ranking;
	
	public AspectImpl(Class serviceInterface, String serviceFilter, int ranking, Object aspectImplementation, Dictionary properties) {
		m_serviceInterface = serviceInterface;
		m_serviceFilter = serviceFilter;
		m_aspectImplementation = aspectImplementation;
		m_aspectProperties = properties;
		m_factory = null;
		m_factoryCreateMethod = null;
		m_ranking = ranking;
	}
	
    public AspectImpl(Class serviceInterface, String serviceFilter, int ranking, Object factory, String factoryCreateMethod, Dictionary properties) {
        m_serviceInterface = serviceInterface;
        m_serviceFilter = serviceFilter;
        m_factory = factory;
        m_factoryCreateMethod = factoryCreateMethod;
        m_aspectProperties = properties;
        m_aspectImplementation = null;
        m_ranking = ranking;
    }

    public Service createService(Object[] properties) {
        ServiceReference ref = (ServiceReference) properties[0]; 
        Object service = properties[1];
        Properties props = new Properties();
        // first add our aspect property
        props.put(DependencyManager.ASPECT, "true");
        // and the ranking
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(m_ranking));
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
        List dependencies = m_service.getDependencies();
        dependencies.remove(0);
        if (m_aspectImplementation == null) {
            return m_manager.createService()
            .setInterface(m_serviceInterface.getName(), props)
            .setFactory(m_factory, m_factoryCreateMethod)
            .add(dependencies)
            .add(m_manager.createServiceDependency()
                .setService(m_serviceInterface, createAspectFilter(m_serviceFilter))
                .setRequired(true));
        }
        else {
            return m_manager.createService()
                .setInterface(m_serviceInterface.getName(), props)
                .setImplementation(m_aspectImplementation)
                .add(dependencies)
                .add(m_manager.createServiceDependency()
                    .setService(m_serviceInterface, createAspectFilter(m_serviceFilter))
                    .setRequired(true));
        }
    }
    private String createAspectFilter(String filter) {
        if (filter == null || filter.length() == 0) {
            return "(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=" + (m_ranking - 1) + "))";
        }
        else {
            return "(&(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=" + (m_ranking - 1) + "))" + filter + ")";
        }
    }
}
