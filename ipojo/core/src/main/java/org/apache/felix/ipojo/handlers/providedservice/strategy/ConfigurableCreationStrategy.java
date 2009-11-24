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

package org.apache.felix.ipojo.handlers.providedservice.strategy;

import java.lang.reflect.Proxy;
import java.util.Properties;

import org.apache.felix.ipojo.IPOJOServiceFactory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.handlers.providedservice.CreationStrategy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * This {@link CreationStrategy} is here to ease customization of the strategy
 * by hiding all the reflection stuff.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class ConfigurableCreationStrategy extends CreationStrategy {

    /**
     * The instance manager passed to the iPOJO ServiceFactory to manage
     * instances.
     */
    private InstanceManager m_manager;

    /**
     * The lists of interfaces provided by this service.
     */
    private String[] m_specs;

    /**
     * The iPOJO ServiceFactory.
     */
    private IPOJOServiceFactory m_factory;

    /**
     * Method called when the underlying iPOJO Service factory
     * is published.
     * @param manager the instance manager
     * @param specifications the provided specifications
     * @param props the service properties
     * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(org.apache.felix.ipojo.InstanceManager, java.lang.String[], java.util.Properties)
     */
    public void onPublication(final InstanceManager manager,
            final String[] specifications, final Properties props) {
        // Store specifications (aka interfaces)
        this.m_specs = specifications;
        this.m_manager = manager;
    }

    /**
     * Method called when the underlying iPOJO Service factory is
     * un-published.
     * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
     */
    public void onUnpublication() {
        // Try to close the factory
        if (m_factory instanceof ServiceObjectFactory) {
            ((ServiceObjectFactory) m_factory).close();
        }
    }

    /**
     * Method called when a bundle want to access a service. This method is called once per
     * asking bundle. To turn around this limitation, a proxy is registered.
     * @param bundle the asking bundle
     * @param registration the service registration
     * @return the service object
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService(final Bundle bundle,
            final ServiceRegistration registration) {
        // Init the factory if needed
        if (m_factory == null) {
            m_factory = getServiceFactory(m_manager);
        }
        // Return a proxy wrapping the real iPOJO ServiceFactory
        return Proxy.newProxyInstance(m_manager.getClazz().getClassLoader(),
                getModifiedSpecifications(m_manager.getContext()),
                new ErrorPrintingServiceFactoryProxy(m_factory));
    }

    /**
     * Method called when a bundle release a service.
     * @param bundle the bundle
     * @param registration the service registration
     * @param service the service object
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(final Bundle bundle,
            final ServiceRegistration registration, final Object service) {
        // Nothing to do
    }

    /**
     * Utility method that transform the specifications names into a Class
     * array, appending the IPOJOServiceFactory interface to it.
     * @param context used for class loading
     * @return a array of Class
     */
    private Class[] getModifiedSpecifications(final BundleContext context) {
        Class[] classes = new Class[m_specs.length + 1];
        int i = 0;
        for (i = 0; i < m_specs.length; i++) {
            try {
                classes[i] = context.getBundle().loadClass(m_specs[i]);
            } catch (ClassNotFoundException e) {
                // Should not happen.
            }
        }
        classes[i] = IPOJOServiceFactory.class;
        return classes;
    }

    /**
     * User provided {@link CreationStrategy} MUST implement this method to
     * provide the real iPOJO ServiceFactory instance.
     * @param manager {@link InstanceManager} that the factory could use
     * @return an instance of {@link IPOJOServiceFactory}
     */
    protected abstract IPOJOServiceFactory getServiceFactory(
            final InstanceManager manager);
}
