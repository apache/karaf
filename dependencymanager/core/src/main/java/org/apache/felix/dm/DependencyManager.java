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
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.dependencies.BundleDependency;
import org.apache.felix.dm.dependencies.ConfigurationDependency;
import org.apache.felix.dm.dependencies.PropertyMetaData;
import org.apache.felix.dm.dependencies.ResourceDependency;
import org.apache.felix.dm.dependencies.ServiceDependency;
import org.apache.felix.dm.dependencies.TemporalServiceDependency;
import org.apache.felix.dm.impl.AdapterServiceImpl;
import org.apache.felix.dm.impl.AspectServiceImpl;
import org.apache.felix.dm.impl.BundleAdapterServiceImpl;
import org.apache.felix.dm.impl.FactoryConfigurationAdapterImpl;
import org.apache.felix.dm.impl.FactoryConfigurationAdapterMetaTypeImpl;
import org.apache.felix.dm.impl.Logger;
import org.apache.felix.dm.impl.ResourceAdapterServiceImpl;
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
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;

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
     * <h3>Usage Example</h3>
     * 
     * <blockquote>
     *  manager.createAspectService(ExistingService.class, "(foo=bar)", 10, "m_aspect")
     *         .setImplementation(ExistingServiceAspect.class)
     *         .setServiceProperties(new Hashtable() {{ put("additional", "properties"); }})
     *         .setComposition("getComposition")
     *         .setCallbacks(new Handler(), null, "mystart", "mystop", null);
     * <pre>
     * </pre>
     * </blockquote>
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param ranking the level used to organize the aspect chain ordering
     * @param attributeName, the aspect implementation field name where to inject original service. 
     *                  If null, any field matching the original service will be injected.
     * @return a service that acts as a factory for generating aspects
     */
    public Service createAspectService(Class serviceInterface, String serviceFilter, int ranking, String attributeName) {
        return new AspectServiceImpl(this, serviceInterface, serviceFilter, ranking, attributeName);
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
     * <h3>Usage Example</h3>
     * 
     * <blockquote>
     *  manager.createAdapterService(AdapteeService.class, "(foo=bar)")
     *         // The interface to use when registering adapter
     *         .setInterface(AdapterService.class, new Hashtable() {{ put("additional", "properties"); }})
     *         // the implementation of the adapter
     *         .setImplementation(AdapterImpl.class);
     * <pre>
     * </pre>
     * </blockquote>
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @return a service that acts as a factory for generating adapters
     */
    public Service createAdapterService(Class serviceInterface, String serviceFilter) {
        return new AdapterServiceImpl(this, serviceInterface, serviceFilter);
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
     * <h3>Usage Example</h3>
     * 
     * <blockquote>
     *  manager.createResourceAdapterService("(&(path=/test)(repository=TestRepository))", true)
     *         // The interface to use when registering adapter
     *         .setInterface(AdapterService.class, new Hashtable() {{ put("foo", "bar"); }})
     *         // the implementation of the adapter
     *         .setImplementation(AdapterServiceImpl.class)
     *         // The callback invoked on adapter lifecycle events
     *         .setCallbacks(new Handler(), "init", "start", "stop", "destroy");
     * <pre>
     * </pre>
     * </blockquote>
     *
     * @param resourceFilter the filter condition to use with the resource
     * @param propagate <code>true</code> if properties from the resource should be propagated to the service
     * @return a service that acts as a factory for generating resource adapters
     * @see Resource
     */
    public Service createResourceAdapterService(String resourceFilter, boolean propagate) {
        return new ResourceAdapterServiceImpl(this, resourceFilter, propagate);
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
     * <h3>Usage Example</h3>
     * 
     * <blockquote>
     *  manager.createBundleAdapterService(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE, 
     *                                     "(Bundle-SymbolicName=org.apache.felix.dependencymanager)",
     *                                     true)
     *         // The interface to use when registering adapter
     *         .setInterface(AdapterService.class, new Hashtable() {{ put("foo", "bar"); }})
     *         // the implementation of the adapter
     *         .setImplementation(AdapterServiceImpl.class)
     *         // The callback invoked on adapter lifecycle events
     *         .setCallbacks(new Handler(), "init", "start", "stop", "destroy");
     * <pre>
     * </pre>
     * </blockquote>
     * 
     * @param bundleStateMask the bundle state mask to apply
     * @param bundleFilter the filter to apply to the bundle manifest
     * @param propagate <code>true</code> if properties from the bundle should be propagated to the service
     * @return a service that acts as a factory for generating bundle adapters
     */
    public Service createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate) {
        return new BundleAdapterServiceImpl(this, bundleStateMask, bundleFilter, propagate);
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
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, factoryPid);
        return createService()
            .setInterface(ManagedServiceFactory.class.getName(), props)
            .setImplementation(new FactoryConfigurationAdapterImpl(factoryPid, update, adapterImplementation, adapterInterface, adapterProperties, propagate));
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
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, factoryPid);
        return createService()
            .setInterface(ManagedServiceFactory.class.getName(), props)
            .setImplementation(new FactoryConfigurationAdapterImpl(factoryPid, update, adapterImplementation, adapterInterfaces, adapterProperties, propagate));
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
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, factoryPid);
        FactoryConfigurationAdapterMetaTypeImpl impl = 
            new FactoryConfigurationAdapterMetaTypeImpl(factoryPid, update, adapterImplementation, adapterInterfaces, adapterProperties, propagate,
                                                        m_context, m_logger, heading, desc, localization, propertiesMetaData);
        return createService()
            .setInterface(ManagedServiceFactory.class.getName(), props)
            .setImplementation(impl);
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
