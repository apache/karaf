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
package org.apache.felix.dm;

import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.dependencies.BundleDependency;
import org.apache.felix.dm.dependencies.ConfigurationDependency;
import org.apache.felix.dm.dependencies.PropertyMetaData;
import org.apache.felix.dm.dependencies.ResourceDependency;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.dependencies.TemporalServiceDependency;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.impl.ServiceImpl;
import org.apache.felix.dm.service.Service;
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
        return m_manager.createService();
    }
    
    /**
     * Creates a new service dependency.
     * 
     * @return the service dependency
     */
    public ServiceDependency createServiceDependency() {
        return m_manager.createServiceDependency();
    }
    
    /**
     * Creates a new temporal service dependency.
     * 
     * @param timeout the max number of milliseconds to wait for a service availability.
     * @return the service dependency
     */
    public TemporalServiceDependency createTemporalServiceDependency() {
        return m_manager.createTemporalServiceDependency();
    }
    
    /**
     * Creates a new configuration dependency.
     * 
     * @return the configuration dependency
     */
    public ConfigurationDependency createConfigurationDependency() {
    	return m_manager.createConfigurationDependency();
    }
    
    /**
     * Creates a new configuration property MetaData.
     * @return a new configuration property MetaData
     */
    public PropertyMetaData createPropertyMetaData() {
        return m_manager.createPropertyMetaData();
    }

    /**
     * Creates a new bundle dependency.
     * 
     * @return the bundle dependency
     */
    public BundleDependency createBundleDependency() {
        return m_manager.createBundleDependency();
    }
    
    public ResourceDependency createResourceDependency() {
        return m_manager.createResourceDependency();
    }

    public Service createAspectService(Class serviceInterface, String serviceFilter, int ranking, String attributeName) {
        return m_manager.createAspectService(serviceInterface, serviceFilter, ranking, attributeName);
    }
    
    public Service createAdapterService(Class serviceInterface, String serviceFilter) {
        return m_manager.createAdapterService(serviceInterface, serviceFilter);
    }
    
    public Service createResourceAdapter(String resourceFilter, boolean propagate) {
        return m_manager.createResourceAdapterService(resourceFilter, propagate);
    }
    
    public Service createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate) {
        return m_manager.createBundleAdapterService(bundleStateMask, bundleFilter, propagate);
    }

    /**
     * Creates a new Managed Service Factory Configuration Adapter. For each new Config Admin factory configuration matching
     * the factoryPid, an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface, and with the specified adapter service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param update the adapter method name that will be notified when the factory configuration is created/updated.
     * @param adapterInterface the interface to use when registering adapters (can be either a String, String array) 
     * @param adapterImplementation the implementation of the adapter (can be a Class or an Object instance)
     * @param adapterProperties additional properties to use with the service registration
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     */
    public Service createFactoryConfigurationAdapterService(String factoryPid, String update, Object adapterImplementation, String adapterInterface, Dictionary adapterProperties, boolean propagate) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, adapterImplementation, adapterInterface, adapterProperties, propagate);
    }
    
    /**
     * Creates a new Managed Service Factory Configuration Adapter. For each new Config Admin factory configuration matching
     * the factoryPid, an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface, and with the specified adapter service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param update the adapter method name that will be notified when the factory configuration is created/updated.
     * @param adapterInterfaces the interfaces to use when registering adapters (can be either a String, String array) 
     * @param adapterImplementation the implementation of the adapter (can be a Class or an Object instance)
     * @param adapterProperties additional properties to use with the service registration
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     */
   public Service createFactoryConfigurationAdapterService(String factoryPid, String update, Object adapterImplementation, String[] adapterInterfaces, Dictionary adapterProperties, boolean propagate) {
        return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, adapterImplementation, adapterInterfaces, adapterProperties, propagate);
    }

   /**
    * Creates a new Managed Service Factory Configuration Adapter with meta type support. For each new Config Admin factory configuration matching
    * the factoryPid, an adapter will be created based on the adapter implementation class.
    * The adapter will be registered with the specified interface, and with the specified adapter service properties.
    * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
    * (which don't start with ".") will be propagated along with the adapter service properties. 
    * It will also inherit all dependencies.
    * 
    * @param factoryPid the pid matching the factory configuration
    * @param update the adapter method name that will be notified when the factory configuration is created/updated.
    * @param adapterInterfaces the interfaces to use when registering adapters (can be either a String, String array) 
    * @param adapterImplementation the implementation of the adapter (can be a Class or an Object instance)
    * @param adapterProperties additional properties to use with the service registration
    * @param propagate true if public factory configuration should be propagated to the adapter service properties
    * @param heading The label used to display the tab name (or section) where the properties are displayed. Example: "Printer Service"
    * @param desc A human readable description of the factory PID this configuration is associated with. Example: "Configuration for the PrinterService bundle"
    * @param localization Points to the basename of the Properties file that can localize the Meta Type informations.
    *        The default localization base name for the properties is OSGI-INF/l10n/bundle, but can
    *        be overridden by the manifest Bundle-Localization header (see core specification, in section Localization on page 68).
    *        You can specify a specific localization basename file using this parameter (e.g. <code>"person"</code> 
    *        will match person_du_NL.properties in the root bundle directory).
    * @param propertiesMetaData Array of MetaData regarding configuration properties
    * @return a service that acts as a factory for generating the managed service factory configuration adapter
    */
   public Service createFactoryConfigurationAdapterService(String factoryPid, String update, Object adapterImplementation, String[] adapterInterfaces, Dictionary adapterProperties, boolean propagate, 
       String heading, String desc, String localization, PropertyMetaData[] propertiesMetaData) 
   {
       return m_manager.createFactoryConfigurationAdapterService(factoryPid, update, adapterImplementation, adapterInterfaces, adapterProperties, propagate,
           heading, desc, localization, propertiesMetaData);
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
