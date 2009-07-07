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
package org.apache.felix.ipojo.handlers.providedservice;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.IPOJOServiceFactory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Property;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Provided Service represent a provided service by the component.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedService implements ServiceFactory {

    /**
     * Service State : REGISTRED.
     */
    public static final int REGISTERED = 1;

    /**
     * Service State : UNREGISTRED.
     */
    public static final int UNREGISTERED = 0;

    /**
     * Factory Policy : SINGLETON_FACTORY.
     */
    public static final int SINGLETON_STRATEGY = 0;

    /**
     * Factory policy : SERVICE_FACTORY.
     */
    public static final int SERVICE_STRATEGY = 1;

    /**
     * Factory policy : STATIC_FACTORY.
     */
    public static final int STATIC_STRATEGY = 2;

    /**
     * Factory policy : INSTANCE.
     * Creates one service object per instance consuming the service.
     */
    public static final int INSTANCE_STRATEGY = 3;

    /**
     * At this time, it is only the java interface full name.
     */
    private String[] m_serviceSpecification = new String[0];

    /**
     * The service registration. is null when the service is not registered.
     * m_serviceRegistration : ServiceRegistration
     */
    private ServiceRegistration m_serviceRegistration;

    /**
     * Link to the owner handler. m_handler : Provided Service Handler
     */
    private ProvidedServiceHandler m_handler;

    /**
     * Properties Array.
     */
    private Property[] m_properties;

    /**
     * Service Object creation policy.
     */
    private CreationStrategy m_strategy;

    /**
     * Creates a provided service object.
     *
     * @param handler the the provided service handler.
     * @param specification the specifications provided by this provided service
     * @param factoryPolicy the service providing policy
     * @param creationStrategyClass the customized service object creation strategy.
     */
    public ProvidedService(ProvidedServiceHandler handler, String[] specification, int factoryPolicy, Class creationStrategyClass) {
        m_handler = handler;

        m_serviceSpecification = specification;

        // Add instance name, factory name and factory version is set.
        try {
            addProperty(new Property("instance.name", null, null, handler.getInstanceManager().getInstanceName(), String.class.getName(), handler.getInstanceManager(), handler));
            addProperty(new Property("factory.name", null, null, handler.getInstanceManager().getFactory().getFactoryName(), String.class.getName(), handler.getInstanceManager(), handler));
            if (handler.getInstanceManager().getFactory().getVersion() != null) {
                addProperty(new Property("factory.version", null, null, handler.getInstanceManager().getFactory().getVersion(), String.class.getName(), handler.getInstanceManager(), handler));
            }
        } catch (ConfigurationException e) {
            m_handler.error("An exception occurs when adding instance.name and factory.name property : " + e.getMessage());
        }

        if (creationStrategyClass != null) {
            try {
                m_strategy = (CreationStrategy) creationStrategyClass.newInstance();
            } catch (IllegalAccessException e) {
                m_handler.error("["
                        + m_handler.getInstanceManager().getInstanceName()
                        + "] The customized service object creation policy "
                        + "(" + creationStrategyClass.getName() + ") is not accessible: "
                        + e.getMessage());
                getInstanceManager().stop();
                return;
            } catch (InstantiationException e) {
                m_handler.error("["
                        + m_handler.getInstanceManager().getInstanceName()
                        + "] The customized service object creation policy "
                        + "(" + creationStrategyClass.getName() + ") cannot be instantiated: "
                        + e.getMessage());
                getInstanceManager().stop();
                return;
            }
        } else {
            switch (factoryPolicy) {
                case SINGLETON_STRATEGY:
                    m_strategy = new SingletonStrategy();
                    break;
                case SERVICE_STRATEGY:
                case STATIC_STRATEGY:
                    // In this case, we need to try to create a new pojo object,
                    // the factory method will handle the creation.
                    m_strategy = new FactoryStrategy();
                    break;
                case INSTANCE_STRATEGY:
                    m_strategy = new PerInstanceStrategy();
                    break;
                // Other policies:
                // Thread : one service object per asking thread
                // Consumer : one service object per consumer
                default:
                    List specs = Arrays.asList(m_serviceSpecification);
                    m_handler.error("["
                            + m_handler.getInstanceManager().getInstanceName()
                            + "] Unknown creation policy for " + specs + " : "
                            + factoryPolicy);
                    getInstanceManager().stop();
                    break;
            }
        }
    }

    /**
     * Add properties to the provided service.
     * @param props : the properties to attached to the service registration
     */
    protected void setProperties(Property[] props) {
        for (int i = 0; i < props.length; i++) {
            addProperty(props[i]);
        }
    }

    /**
     * Add the given property to the property list.
     *
     * @param prop : the element to add
     */
    private synchronized void addProperty(Property prop) {
        for (int i = 0; (m_properties != null) && (i < m_properties.length); i++) {
            if (m_properties[i] == prop) {
                return;
            }
        }

        if (m_properties == null) {
            m_properties = new Property[] { prop };
        } else {
            Property[] newProp = new Property[m_properties.length + 1];
            System.arraycopy(m_properties, 0, newProp, 0, m_properties.length);
            newProp[m_properties.length] = prop;
            m_properties = newProp;
        }
    }

    /**
     * Remove a property.
     *
     * @param name : the property to remove
     */
    private synchronized void removeProperty(String name) {
        int idx = -1;
        for (int i = 0; i < m_properties.length; i++) {
            if (m_properties[i].getName().equals(name)) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_properties.length - 1) == 0) {
                m_properties = null;
            } else {
                Property[] newPropertiesList = new Property[m_properties.length - 1];
                System.arraycopy(m_properties, 0, newPropertiesList, 0, idx);
                if (idx < newPropertiesList.length) {
                    System.arraycopy(m_properties, idx + 1, newPropertiesList, idx, newPropertiesList.length - idx);
                }
                m_properties = newPropertiesList;
            }
        }
    }

    /**
     * Get the service reference of the service registration.
     * @return the service reference of the provided service (null if the
     * service is not published).
     */
    public ServiceReference getServiceReference() {
        if (m_serviceRegistration == null) {
            return null;
        } else {
            return m_serviceRegistration.getReference();
        }
    }

    /**
     * Return a service object for the dependency.
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     * @param bundle : the bundle
     * @param registration : the service registration of the registred service
     * @return : a new service object or a already created service object (in the case of singleton)
     */
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        return m_strategy.getService(bundle, registration);
    }

    /**
     * The unget method.
     *
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle,
     * org.osgi.framework.ServiceRegistration, java.lang.Object)
     * @param bundle : bundle
     * @param registration : service registration
     * @param service : service object
     */
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        m_strategy.ungetService(bundle, registration, service);
    }

    /**
     * Registers the service. The service object must be able to serve this
     * service.
     * This method also notifies the creation strategy of the publication.
     */
    protected synchronized void registerService() {
        if (m_serviceRegistration == null) {
            // Build the service properties list
            Properties serviceProperties = getServiceProperties();
            m_strategy.onPublication(getInstanceManager(), m_serviceSpecification, serviceProperties);
            m_serviceRegistration = m_handler.getInstanceManager().getContext().registerService(m_serviceSpecification, this, serviceProperties);
        }
    }

    /**
     * Unregisters the service.
     */
    protected synchronized void unregisterService() {
        if (m_serviceRegistration != null) {
            m_serviceRegistration.unregister();
            m_serviceRegistration = null;
        }

        m_strategy.onUnpublication();

    }

    /**
     * Get the current provided service state.
     * @return The state of the provided service.
     */
    public int getState() {
        if (m_serviceRegistration == null) {
            return UNREGISTERED;
        } else {
            return REGISTERED;
        }
    }

    protected InstanceManager getInstanceManager() {
        return m_handler.getInstanceManager();
    }

    /**
     * Return the list of properties attached to this service. This list
     * contains only property where a value are assigned.
     *
     * @return the properties attached to the provided service.
     */
    private Properties getServiceProperties() {
        // Build the service properties list
        Properties serviceProperties = new Properties();
        for (int i = 0; i < m_properties.length; i++) {
            Object value = m_properties[i].getValue();
            if (value != null && value != Property.NO_VALUE) {
                serviceProperties.put(m_properties[i].getName(), value);
            }
        }
        return serviceProperties;
    }

    /**
     * Get the list of properties attached to the service registration.
     * @return the properties attached to the provided service.
     */
    public Property[] getProperties() {
        return m_properties;
    }

    /**
     * Update the service properties. The new list of properties is sent to
     * the service registry.
     */
    public synchronized void update() {
        // Update the service registration
        if (m_serviceRegistration != null) {
            m_serviceRegistration.setProperties(getServiceProperties());
        }
    }

    /**
     * Add properties to the list.
     * @param props : properties to add
     */
    protected void addProperties(Dictionary props) {
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = props.get(key);
            Property prop;
            try {
                prop = new Property(key, null, null, value.toString(), value.getClass().getName(), getInstanceManager(), m_handler);
                addProperty(prop);
            } catch (ConfigurationException e) {
                m_handler.error("The propagated property " + key + " cannot be created correctly : " + e.getMessage());
            }
        }
    }

    /**
     * Remove properties from the list.
     * @param props : properties to remove
     */
    protected void deleteProperties(Dictionary props) {
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            removeProperty(key);
        }
    }

    /**
     * Get the published service specifications.
     * @return the list of provided service specifications (i.e. java
     * interface).
     */
    public String[] getServiceSpecifications() {
        return m_serviceSpecification;
    }

    /**
     * Get the service registration.
     * @return the service registration of this service.
     */
    public ServiceRegistration getServiceRegistration() {
        return m_serviceRegistration;
    }

    /**
     * Singleton creation strategy.
     * This strategy just creates one service object and
     * returns always the same.
     */
    private class SingletonStrategy extends CreationStrategy {

        /**
         * The service is going to be registered.
         * @param instance the instance manager
         * @param interfaces the published interfaces
         * @param props the properties
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(InstanceManager, java.lang.String[], java.util.Properties)
         */
        public void onPublication(InstanceManager instance, String[] interfaces,
                Properties props) { }

        /**
         * The service was unpublished.
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
         */
        public void onUnpublication() { }

        /**
         * A service object is required.
         * @param arg0 the bundle requiring the service object.
         * @param arg1 the service registration.
         * @return the first pojo object.
         * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
         */
        public Object getService(Bundle arg0, ServiceRegistration arg1) {
            return m_handler.getInstanceManager().getPojoObject();
        }

        /**
         * A service object is released.
         * @param arg0  the bundle
         * @param arg1 the service registration
         * @param arg2 the get service object.
         * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
         */
        public void ungetService(Bundle arg0, ServiceRegistration arg1,
                Object arg2) {
        }

    }

    /**
     * Service object creation policy following the OSGi Service Factory
     * policy {@link ServiceFactory}.
     */
    private class FactoryStrategy extends CreationStrategy {

        /**
         * The service is going to be registered.
         * @param instance the instance manager
         * @param interfaces the published interfaces
         * @param props the properties
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(InstanceManager, java.lang.String[], java.util.Properties)
         */
        public void onPublication(InstanceManager instance, String[] interfaces,
                Properties props) { }

        /**
         * The service is unpublished.
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
         */
        public void onUnpublication() { }

        /**
         * OSGi Service Factory getService method.
         * Returns a new service object per asking bundle.
         * This object is then cached by the framework.
         * @param arg0 the bundle requiring the service
         * @param arg1 the service registration
         * @return the service object for the asking bundle
         * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
         */
        public Object getService(Bundle arg0, ServiceRegistration arg1) {
            return m_handler.getInstanceManager().createPojoObject();
        }

        /**
         * OSGi Service Factory unget method.
         * Deletes the created object for the asking bundle.
         * @param arg0 the asking bundle
         * @param arg1 the service registration
         * @param arg2 the created service object returned for this bundle
         * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
         */
        public void ungetService(Bundle arg0, ServiceRegistration arg1,
                Object arg2) {
            m_handler.getInstanceManager().deletePojoObject(arg2);
        }
    }


    /**
     * Service object creation policy creating a service object per asking iPOJO component
     * instance. This creation policy follows the iPOJO Service Factory interaction pattern
     * and does no support 'direct' invocation.
     */
    private class PerInstanceStrategy extends CreationStrategy implements IPOJOServiceFactory, InvocationHandler {
        /**
         * Map [ComponentInstance->ServiceObject] storing created service objects.
         */
        private Map/*<ComponentInstance, ServiceObject>*/ m_instances = new HashMap();

        /**
         * A method is invoked on the proxy object.
         * If the method is the {@link IPOJOServiceFactory#getService(ComponentInstance)}
         * method, this method creates a service object if no already created for the asking
         * component instance.
         * If the method is {@link IPOJOServiceFactory#ungetService(ComponentInstance, Object)}
         * the service object is unget (i.e. removed from the map and deleted).
         * In all other cases, a {@link UnsupportedOperationException} is thrown as this policy
         * requires to use  the {@link IPOJOServiceFactory} interaction pattern.
         * @param arg0 the proxy object
         * @param arg1 the called method
         * @param arg2 the arguments
         * @return the service object attached to the asking instance for 'get',
         * <code>null</code> for 'unget',
         * a {@link UnsupportedOperationException} for all other methods.
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(Object arg0, Method arg1, Object[] arg2) {
            if (isGetServiceMethod(arg1)) {
                return getService((ComponentInstance) arg2[0]);
            }

            if (isUngetServiceMethod(arg1)) {
                ungetService((ComponentInstance) arg2[0], arg2[1]);
                return null;
            }

            throw new UnsupportedOperationException("This service requires an advanced creation policy. "
                    + "Before calling the service, call the getService(ComponentInstance) method to get "
                    + "the service object. ");
        }

        /**
         * A service object is required.
         * This policy returns a service object per asking instance.
         * @param instance the instance requiring the service object
         * @return the service object for this instance
         * @see org.apache.felix.ipojo.IPOJOServiceFactory#getService(org.apache.felix.ipojo.ComponentInstance)
         */
        public Object getService(ComponentInstance instance) {
            Object obj = m_instances.get(instance);
            if (obj == null) {
                obj = m_handler.getInstanceManager().createPojoObject();
                m_instances.put(instance, obj);
            }
            return obj;
        }

        /**
         * A service object is unget.
         * The service object is removed from the map and deleted.
         * @param instance the instance releasing the service
         * @param svcObject the service object
         * @see org.apache.felix.ipojo.IPOJOServiceFactory#ungetService(org.apache.felix.ipojo.ComponentInstance, java.lang.Object)
         */
        public void ungetService(ComponentInstance instance, Object svcObject) {
            Object pojo = m_instances.remove(instance);
            m_handler.getInstanceManager().deletePojoObject(pojo);
        }

        /**
         * The service is going to be registered.
         * @param instance the instance manager
         * @param interfaces the published interfaces
         * @param props the properties
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(InstanceManager, java.lang.String[], java.util.Properties)
         */
        public void onPublication(InstanceManager instance, String[] interfaces,
                Properties props) { }

        /**
         * The service is going to be unregistered.
         * The instance map is cleared. Created object are disposed.
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
         */
        public void onUnpublication() {
            Collection col = m_instances.values();
            Iterator it = col.iterator();
            while (it.hasNext()) {
                m_handler.getInstanceManager().deletePojoObject(it.next());
            }
            m_instances.clear();
        }

        /**
         * OSGi Service Factory getService method.
         * @param arg0 the asking bundle
         * @param arg1 the service registration
         * @return a proxy implementing the {@link IPOJOServiceFactory}
         * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
         */
        public Object getService(Bundle arg0, ServiceRegistration arg1) {
            Object proxy = Proxy.newProxyInstance(getInstanceManager().getClazz().getClassLoader(),
                    getSpecificationsWithIPOJOServiceFactory(m_serviceSpecification, m_handler.getInstanceManager().getContext()), this);
            return proxy;
        }

        /**
         * OSGi Service factory unget method.
         * Does nothing.
         * @param arg0 the asking bundle
         * @param arg1 the service registration
         * @param arg2 the service object created for this bundle.
         * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
         */
        public void ungetService(Bundle arg0, ServiceRegistration arg1,
                Object arg2) { }

        /**
         * Utility method returning the class array of provided service
         * specification and the {@link IPOJOServiceFactory} interface.
         * @param specs the published service interface
         * @param bc the bundle context, used to load classes
         * @return the class array containing provided service specification and
         * the {@link IPOJOServiceFactory} class.
         */
        private Class[] getSpecificationsWithIPOJOServiceFactory(String[] specs, BundleContext bc) {
            Class[] classes = new Class[specs.length + 1];
            int i = 0;
            for (i = 0; i < specs.length; i++) {
                try {
                    classes[i] = bc.getBundle().loadClass(specs[i]);
                } catch (ClassNotFoundException e) {
                    // Should not happen.
                }
            }
            classes[i] = IPOJOServiceFactory.class;
            return classes;
        }


    }

}
