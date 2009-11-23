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
package org.apache.felix.dependencymanager;

import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Base bundle activator class. Subclass this activator if you want to use dependency
 * management in your bundle. There are two methods you should implement:
 * <code>init()</code> and <code>destroy()</code>. Both methods take two arguments,
 * the bundle context and the dependency manager. The dependency manager can be used
 * to define all the dependencies.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class DependencyActivatorBase implements BundleActivator {
    private BundleContext m_context;
    private DependencyManager m_manager;
    private Logger m_logger;
    
    /**
     * Initialize the dependency manager. Here you can add all services and their dependencies.
     * If something goes wrong and you do not want your bundle to be started, you can throw an
     * exception. This exception will be passed on to the <code>start()</code> method of the
     * bundle activator, causing the bundle not to start.
     * 
     * @param context the bundle context
     * @param manager the dependency manager
     * @throws Exception if the initialization fails
     */
    public abstract void init(BundleContext context, DependencyManager manager) throws Exception;
    
    /**
     * Destroy the dependency manager. Here you can remove all services and their dependencies.
     * Actually, the base class will clean up your dependencies anyway, so most of the time you
     * don't need to do anything here.
     * If something goes wrong and you do not want your bundle to be stopped, you can throw an
     * exception. This exception will be passed on to the <code>stop()</code> method of the
     * bundle activator, causing the bundle not to stop.
     * 
     * @param context the bundle context
     * @param manager the dependency manager
     * @throws Exception if the destruction fails
     */
    public abstract void destroy(BundleContext context, DependencyManager manager) throws Exception;

    /**
     * Start method of the bundle activator. Initializes the dependency manager
     * and calls <code>init()</code>.
     * 
     * @param context the bundle context
     */
    public void start(BundleContext context) throws Exception {
        m_context = context;
        m_logger = new Logger(context);
        m_manager = new DependencyManager(context, m_logger);
        init(m_context, m_manager);
    }

    /**
     * Stop method of the bundle activator. Calls the <code>destroy()</code> method
     * and cleans up all left over dependencies.
     * 
     * @param context the bundle context
     */
    public void stop(BundleContext context) throws Exception {
        destroy(m_context, m_manager);
        cleanup(m_manager);
        m_manager = null;
        m_context = null;
    }
    
    /**
     * Creates a new service.
     * 
     * @return the new service
     */
    public Service createService() {
        return new ServiceImpl(m_context, m_manager, m_logger);
    }
    
    /**
     * Creates a new service dependency.
     * 
     * @return the service dependency
     */
    public ServiceDependency createServiceDependency() {
        return new ServiceDependency(m_context, m_logger);
    }
    
    /**
     * Creates a new temporal service dependency.
     * 
     * @param timeout the max number of milliseconds to wait for a service availability.
     * @return the service dependency
     */
    public TemporalServiceDependency createTemporalServiceDependency() {
        return new TemporalServiceDependency(m_context, m_logger);
    }
    
    /**
     * Creates a new configuration dependency.
     * 
     * @return the configuration dependency
     */
    public ConfigurationDependency createConfigurationDependency() {
    	return new ConfigurationDependency(m_context, m_logger);
    }

    /**
     * Cleans up all services and their dependencies.
     * 
     * @param manager the dependency manager
     */
    private void cleanup(DependencyManager manager) {
        List services = manager.getServices();
        for (int i = services.size() - 1; i >= 0; i--) {
            Service service = (Service) services.get(i);
            manager.remove(service);
            // remove any state listeners that are still registered
            if (service instanceof ServiceImpl) {
                ServiceImpl si = (ServiceImpl) service;
                si.removeStateListeners();
            }
        }
    }
}
