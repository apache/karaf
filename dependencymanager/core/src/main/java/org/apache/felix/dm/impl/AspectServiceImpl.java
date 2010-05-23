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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.service.Service;
import org.apache.felix.dm.service.ServiceStateListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Aspect Service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual aspect service implementation.
 */
public class AspectServiceImpl extends FilterService
{
    public AspectServiceImpl(DependencyManager dm, Class aspectInterface, String aspectFilter, int ranking, String autoConfig)
    { 
        super(dm.createService()); // This service will be filtered by our super class, allowing us to take control.
        m_service.setImplementation(new AspectImpl(aspectInterface, aspectFilter, ranking, autoConfig))
                 .add(dm.createServiceDependency()
                      .setService(aspectInterface, createAspectFilter(aspectFilter))
                      .setAutoConfig(false)
                      .setCallbacks("added", "removed"));
    }

    private String createAspectFilter(String filter) {
        // we only want to match services which are not themselves aspects
        if (filter == null || filter.length() == 0) {
            return "(!(" + DependencyManager.ASPECT + "=*))";
        }
        else {
            return "(&(!(" + DependencyManager.ASPECT + "=*))" + filter + ")";
        }        
    }
    
    /**
     * This class is the Aspect Implementation. It will create the actual Aspect Service, and
     * will use the Aspect Service parameters provided by our enclosing class.
     */
    class AspectImpl extends AbstractDecorator {
        private final Class m_aspectInterface; // the service decorated by this aspect
        private final String m_aspectFilter; // the service filter decorated by this aspect
        private final int m_ranking; // the aspect ranking
        private final String m_field; // the aspect impl field name where to inject decorated service
      
        public AspectImpl(Class aspectInterface, String aspectFilter, int ranking, String field) {
            m_aspectInterface = aspectInterface;
            m_aspectFilter = aspectFilter;
            m_ranking = ranking;
            m_field = field;
        }
        
        public Service createService(Object[] params) {
            List dependencies = m_service.getDependencies();
            // remove our internal dependency
            dependencies.remove(0);
            Properties serviceProperties = getServiceProperties(params);
            String[] serviceInterfaces = getServiceInterfaces();
            Service service = m_manager.createService()
                .setInterface(serviceInterfaces, serviceProperties)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(dependencies)
                .add(getAspectDependency());
            for (int i = 0; i < m_stateListeners.size(); i ++) {
                service.addStateListener((ServiceStateListener) m_stateListeners.get(i));
            }
            return service;                
        }
        
        private Properties getServiceProperties(Object[] params) {
            ServiceReference ref = (ServiceReference) params[0]; 
            Object service = params[1];
            Properties props = new Properties();
            // first add our aspect property
            props.put(DependencyManager.ASPECT, ref.getProperty(Constants.SERVICE_ID));
            // and the ranking
            props.put(Constants.SERVICE_RANKING, Integer.valueOf(m_ranking));
            String[] keys = ref.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                props.put(keys[i], ref.getProperty(keys[i]));
            }
            if (m_serviceProperties != null) {
                Enumeration e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    Object key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            return props;
        }
        
        private String[] getServiceInterfaces()
        {
            List serviceNames = new ArrayList();
            // Of course, we provide the aspect interface.
            serviceNames.add(m_aspectInterface.getName());
            // But also append additional aspect implementation interfaces.
            if (m_serviceInterfaces != null) {
                for (int i = 0; i < m_serviceInterfaces.length; i ++) {
                    if (! m_serviceInterfaces[i].equals(m_aspectInterface.getName())) {
                        serviceNames.add(m_serviceInterfaces[i]);
                    }
                }
            }
            return (String[]) serviceNames.toArray(new String[serviceNames.size()]);
        }

       private Dependency getAspectDependency() {
           ServiceDependency sd = 
               m_manager.createServiceDependency()
                        .setService(m_aspectInterface, createAspectFilter(m_aspectFilter))
                        .setRequired(true);
        
           if (m_field != null) {
               sd.setAutoConfig(m_field);
           }
           return sd;
        }

       private String createAspectFilter(String filter) {
           if (filter == null || filter.length() == 0) {
               return "(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=" + (m_ranking - 1) + "))";
           } else {
               return "(&(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=" + (m_ranking - 1) + "))" + filter + ")";
           }
       }
    }
}
