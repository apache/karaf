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
import org.apache.felix.dm.service.Service;
import org.osgi.framework.BundleContext;

/**
 * The dependency manager. Manages all services and their dependencies.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyManager {
    private final BundleContext m_context;
    private final Logger m_logger;
    private List m_services = Collections.synchronizedList(new ArrayList());

    /**
     * Creates a new dependency manager.
     * 
     * @param context the bundle context
     * @param logger 
     */
    public DependencyManager(BundleContext context) {
        this(context, new Logger(context));
    }
    
    /**
     * Creates a new dependency manager.
     * 
     * @param context the bundle context
     * @param logger 
     */
    public DependencyManager(BundleContext context, Logger logger) {
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
     * @return
     */
    public ConfigurationDependency createConfigurationDependency() {
        return new ConfigurationDependencyImpl(m_context, m_logger);
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
     * @return
     */
    public ResourceDependency createResourceDependency() {
        return new ResourceDependencyImpl(m_context, m_logger);
    }

    /**
     * Creates a new aspect.
     * 
     * @param serviceInterface
     * @param serviceFilter
     * @param aspectImplementation
     * @param properties
     * @return
     */
    public Service createAspectService(Class serviceInterface, String serviceFilter, Object aspectImplementation, Dictionary properties) {
        return createService()
            .setImplementation(new AspectImpl(serviceInterface, serviceFilter, aspectImplementation, properties))
            .add(createServiceDependency()
                .setService(serviceInterface, serviceFilter)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
            );
    }
    
    //TODO rename iface en iface2 to adaptor en adaptee o.i.d.
    public Service createAdapterService(Class serviceInterface, String serviceFilter, Class adapterInterface, Object impl, Dictionary adapterProperties) {
        return createService()
            .setImplementation(new AdapterImpl(serviceInterface, serviceFilter, impl, adapterInterface.getName(), adapterProperties))
            .add(createServiceDependency()
                .setService(serviceInterface)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
                );
    }
    
    // TODO note to self, there are Dependency's and DependencyCollections 
    // (being a dependency on more than one, fi ServiceDendency, ResourceDependency
    public Service createResourceAdapterService(String resourceFilter, Class iface2, Object impl, boolean propagate) {
        return createService()
            .setImplementation(new ResourceAdapterImpl(resourceFilter, impl, iface2.getName(), null, propagate))
            .add(createResourceDependency()
                .setFilter(resourceFilter)
                .setAutoConfig(false)
                .setCallbacks("added", "removed")
                )
                ;
    }
    
    public Service createBundleAdapterService(int stateMask, String filter, Object impl, Class iface) {
        return createService()
            .setImplementation(new BundleAdapterImpl(stateMask, filter, impl, iface))
            .add(createBundleDependency()
                .setFilter(filter)
                .setStateMask(stateMask)
                .setCallbacks("added", "removed")
                )
            ;
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
