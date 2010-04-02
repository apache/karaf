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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.dependencies.BundleDependency;
import org.apache.felix.dm.dependencies.ConfigurationDependency;
import org.apache.felix.dm.dependencies.PropertyMetaData;
import org.apache.felix.dm.dependencies.ResourceDependency;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.dependencies.TemporalServiceDependency;
import org.apache.felix.dm.impl.AdapterImpl;
import org.apache.felix.dm.impl.AspectImpl;
import org.apache.felix.dm.impl.BundleAdapterImpl;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.impl.ResourceAdapterImpl;
import org.apache.felix.dm.impl.ServiceImpl;
import org.apache.felix.dm.impl.dependencies.BundleDependencyImpl;
import org.apache.felix.dm.impl.dependencies.ConfigurationDependencyImpl;
import org.apache.felix.dm.impl.dependencies.ResourceDependencyImpl;
import org.apache.felix.dm.impl.dependencies.ServiceDependencyImpl;
import org.apache.felix.dm.impl.dependencies.TemporalServiceDependencyImpl;
import org.apache.felix.dm.impl.metatype.PropertyMetaDataImpl;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.BundleContext;

/**
 * The dependency manager manages all services and their dependencies. Using 
 * this API you can declare all services and their dependencies. Under normal
 * circumstances, you get passed an instance of this class through the
 * <code>DependencyActivatorBase</code> subclass you use as your
 * <code>BundleActivator</code>, but it is also possible to create your
 * own instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyManager {
    public static final String ASPECT = "org.apache.felix.dependencymanager.aspect";
    private final BundleContext m_context;
    private final Logger m_logger;
    private List m_services = Collections.synchronizedList(new ArrayList());

    /**
     * Creates a new dependency manager. You need to supply the
     * <code>BundleContext</code> to be used by the dependency
     * manager to register services and communicate with the 
     * framework.
     * 
     * @param context the bundle context
     */
    public DependencyManager(BundleContext context) {
        this(context, new Logger(context));
    }
    
    DependencyManager(BundleContext context, Logger logger) {
        m_context = context;
        m_logger = logger;
    }

    /**
     * Adds a new service to the dependency manager. After the service was added
     * it will be started immediately.
     * 
     * @param service the service to add
     */
    public void add(Service service) {
        m_services.add(service);
        service.start();
    }

    /**
     * Removes a service from the dependency manager. Before the service is removed
     * it is stopped first.
     * 
     * @param service the service to remove
     */
    public void remove(Service service) {
        service.stop();
        m_services.remove(service);
    }

    /**
     * Creates a new service.
     * 
     * @return the new service
     */
    public Service createService() {
        return new ServiceImpl(m_context, this, m_logger);
    }
    
    /**
     * Creates a new service dependency.
     * 
     * @return the service dependency
     */
    public ServiceDependency createServiceDependency() {
        return new ServiceDependencyImpl(m_context, m_logger);
    }
    
    /**
     * Creates a new temporal service dependency.
     * 
     * @param timeout the max number of milliseconds to wait for a service availability.
     * @return the service dependency
     */
    public TemporalServiceDependency createTemporalServiceDependency() {
        return new TemporalServiceDependencyImpl(m_context, m_logger);
    }

    /**
     * Creates a new configuration dependency.
     * 
     * @return the configuration dependency
     */
    public ConfigurationDependency createConfigurationDependency() {
        return new ConfigurationDependencyImpl(m_context, m_logger);
    }
    
    /**
     * Creates a new configuration property MetaData.
     * @return a new Configuration property MetaData.
     */
    public PropertyMetaData createPropertyMetaData() {
        return new PropertyMetaDataImpl();
    }

    /**
     * Creates a new bundle dependency.
     * 
     * @return
     */
    public BundleDependency createBundleDependency() {
        return new BundleDependencyImpl(m_context, m_logger);
    }
    
    /**
     * Creates a new resource dependency.
     * 
     * @return the resource dependency
     */
    public ResourceDependency createResourceDependency() {
        return new ResourceDependencyImpl(m_context, m_logger);
    }

    /**
     * Creates a new aspect. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param aspectImplementation the implementation of the aspect
     * @param aspectProperties additional properties to use with the aspect service registration
     * @return a service that acts as a factory for generating aspects
     */
    public Service createAspectService(Class serviceInterface, String serviceFilter, int ranking, Object aspectImplementation, Dictionary aspectProperties) {
        return createService()
            .setImplementation(new AspectImpl(serviceInterface, serviceFilter, ranking, aspectImplementation, aspectProperties))
            .add(createServiceDependency()
                .setService(serviceInterface, createAspectFilter(serviceFilter))
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }
    public Service createAspectService(Class serviceInterface, String serviceFilter, int ranking, Object factory, String factoryCreateMethod, Dictionary aspectProperties) {
        return createService()
            .setImplementation(new AspectImpl(serviceInterface, serviceFilter, ranking, factory, factoryCreateMethod, aspectProperties))
            .add(createServiceDependency()
                .setService(serviceInterface, createAspectFilter(serviceFilter))
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }
    private String createAspectFilter(String filter) {
        // we only want to match services which are not themselves aspects
        if (filter == null || filter.length() == 0) {
            return "(!(" + ASPECT + "=*))";
        }
        else {
            return "(&(!(" + ASPECT + "=*))" + filter + ")";
        }        
    }
    
    /**
     * Creates a new adapter. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @param adapterInterface the interface to use when registering adapters
     * @param adapterImplementation the implementation of the adapter
     * @param adapterProperties additional properties to use with the adapter service registration
     * @return a service that acts as a factory for generating adapters
     */
    public Service createAdapterService(Class serviceInterface, String serviceFilter, String adapterInterface, Object adapterImplementation, Dictionary adapterProperties) {
        return createService()
            .setImplementation(new AdapterImpl(serviceInterface, serviceFilter, adapterImplementation, adapterInterface, adapterProperties))
            .add(createServiceDependency()
                .setService(serviceInterface)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }
    
    public Service createAdapterService(Class serviceInterface, String serviceFilter, String[] adapterInterface, Object adapterImplementation, Dictionary adapterProperties) {
        return createService()
            .setImplementation(new AdapterImpl(serviceInterface, serviceFilter, adapterImplementation, adapterInterface, adapterProperties))
            .add(createServiceDependency()
                .setService(serviceInterface)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }
    
    /**
     * Creates a new resource adapter. The adapter will be applied to any resource that
     * matches the specified filter condition. For each matching resource
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original resource plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @param resourceFilter the filter condition to use with the resource
     * @param adapterInterface the interface to use when registering adapters
     * @param adapterProperties additional properties to use with the adapter service registration
     * @param adapterImplementation the implementation of the adapter
     * @param propagate <code>true</code> if properties from the resource should be propagated to the service
     * @return a service that acts as a factory for generating resource adapters
     * @see Resource
     */
    public Service createResourceAdapterService(String resourceFilter, String adapterInterface, Dictionary adapterProperties, Object adapterImplementation, boolean propagate) {
        return createService()
            .setImplementation(new ResourceAdapterImpl(resourceFilter, adapterImplementation, adapterInterface, adapterProperties, propagate))
            .add(createResourceDependency()
                .setFilter(resourceFilter)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }
    public Service createResourceAdapterService(String resourceFilter, String[] adapterInterface, Dictionary adapterProperties, Object adapterImplementation, boolean propagate) {
        return createService()
            .setImplementation(new ResourceAdapterImpl(resourceFilter, adapterImplementation, adapterInterface, adapterProperties, propagate))
            .add(createResourceDependency()
                .setFilter(resourceFilter)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }

    /**
     * Creates a new bundle adapter. The adapter will be applied to any bundle that
     * matches the specified bundle state mask and filter condition. For each matching
     * bundle an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface
     * 
     * TODO and existing properties from the original resource plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @param bundleStateMask the bundle state mask to apply
     * @param bundleFilter the filter to apply to the bundle manifest
     * @param adapterImplementation the implementation of the adapter
     * @param adapterInterface the interface to use when registering adapters
     * @param adapterProperties additional properties to use with the service registration
     * @param propagate <code>true</code> if properties from the bundle should be propagated to the service
     * @return a service that acts as a factory for generating bundle adapters
     */
    public Service createBundleAdapterService(int bundleStateMask, String bundleFilter, Object adapterImplementation, String adapterInterface, Dictionary adapterProperties, boolean propagate) {
        return createService()
            .setImplementation(new BundleAdapterImpl(bundleStateMask, bundleFilter, adapterImplementation, adapterInterface, adapterProperties, propagate))
            .add(createBundleDependency()
                .setFilter(bundleFilter)
                .setStateMask(bundleStateMask)
                .setCallbacks("added", "removed")
            );
    }
    public Service createBundleAdapterService(int bundleStateMask, String bundleFilter, Object adapterImplementation, String[] adapterInterface, Dictionary adapterProperties, boolean propagate) {
        return createService()
            .setImplementation(new BundleAdapterImpl(bundleStateMask, bundleFilter, adapterImplementation, adapterInterface, adapterProperties, propagate))
            .add(createBundleDependency()
                .setFilter(bundleFilter)
                .setStateMask(bundleStateMask)
                .setCallbacks("added", "removed")
            );
    }

    /**
     * Returns a list of services.
     * 
     * @return a list of services
     */
    public List getServices() {
        return Collections.unmodifiableList(m_services);
    }
}
