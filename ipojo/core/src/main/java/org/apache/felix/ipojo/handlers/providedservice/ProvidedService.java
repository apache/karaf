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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Provided Service represent a provided service by the component.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
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
    public static final int SINGLETON_FACTORY = 0;

    /**
     * Factory policy : SERVICE_FACTORY.
     */
    public static final int SERVICE_FACTORY = 1;

    /**
     * At this time, it is only the java interface full name.
     */
    private String[] m_serviceSpecification = new String[0];

    /**
     * Factory policy.
     */
    private int m_factoryPolicy = SINGLETON_FACTORY;

    /**
     * The service registration. is null when the service is not registred.
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
    private Property[] m_properties = new Property[0];

    /**
     * Construct a provided service object.
     * 
     * @param handler : the provided service handler.
     * @param specification : specifications provided by this provided service
     * @param factoryPolicy : service providing policy
     */
    public ProvidedService(ProvidedServiceHandler handler, String[] specification, int factoryPolicy) {
        m_handler = handler;

        m_serviceSpecification = specification;
        m_factoryPolicy = factoryPolicy;

        // Add service pid and factory pid
        addProperty(new Property(this, org.osgi.framework.Constants.SERVICE_PID, handler.getInstanceManager().getInstanceName()));
        addProperty(new Property(this, "factory.pid", handler.getInstanceManager().getFactory().getName()));
    }

    // TODO check if we need to erase previous props or add to the previous
    // props.
    /**
     * Add properties to the provided service.
     * 
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
     * @param p : the element to add
     */
    private synchronized void addProperty(Property p) {
        for (int i = 0; (m_properties != null) && (i < m_properties.length); i++) {
            if (m_properties[i] == p) {
                return;
            }
        }

        if (m_properties.length > 0) {
            Property[] newProp = new Property[m_properties.length + 1];
            System.arraycopy(m_properties, 0, newProp, 0, m_properties.length);
            newProp[m_properties.length] = p;
            m_properties = newProp;
        } else {
            m_properties = new Property[] { p };
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
            if (m_properties[i].getName() == name) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_properties.length - 1) == 0) {
                m_properties = new Property[0];
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
        if (m_serviceRegistration != null) {
            return m_serviceRegistration.getReference();
        } else {
            return null;
        }
    }

    /**
     * Return a service object for the dependency.
     * 
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle,
     * org.osgi.framework.ServiceRegistration)
     * @param bundle : the bundle
     * @param registration : the service registration of the registred service
     * @return : a new service object or a already created service object (in
     * the case of singleton)
     */
    public Object getService(Bundle bundle, ServiceRegistration registration) {

        switch (m_factoryPolicy) {

            case SINGLETON_FACTORY:
                return m_handler.getInstanceManager().getPojoObject();

            case SERVICE_FACTORY:
                return m_handler.getInstanceManager().createPojoObject();

            default:
                m_handler.getInstanceManager().getFactory().getLogger().log(
                        Logger.ERROR,
                        "[" + m_handler.getInstanceManager().getClassName() + "] Unknown factory policy for " + m_serviceSpecification + " : "
                                + m_factoryPolicy);
                return null;
        }

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
        // Nothing to do
    }

    /**
     * Register the service. The service object must be able to serve this
     * service. To avoid cycle in Check Context, the registred service is set to
     * registred before the real registration.
     */
    protected synchronized void registerService() {
        if (m_serviceRegistration == null) {
            String spec = "";
            for (int i = 0; i < m_serviceSpecification.length; i++) {
                spec = spec + m_serviceSpecification[i] + ", ";
            }
            // Contruct the service properties list
            Properties serviceProperties = getServiceProperties();

            m_serviceRegistration = m_handler.getInstanceManager().getContext().registerService(m_serviceSpecification, this, serviceProperties);
        }
    }

    /**
     * Unregister the service.
     */
    protected synchronized void unregisterService() {
        if (m_serviceRegistration != null) {
            try {
                m_serviceRegistration.unregister();
            } catch (Exception e) {
                return;
            }
            m_serviceRegistration = null;
        }
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
        // Contruct the service properties list
        Properties serviceProperties = new Properties();
        for (int i = 0; i < m_properties.length; i++) {
            if (m_properties[i].get() != null) {
                serviceProperties.put(m_properties[i].getName(), m_properties[i].get());
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
     * Update the service properties. The new list of properties is sended to
     * the service registry.
     */
    public synchronized void update() {
        // Contruct the service properties list
        Properties serviceProperties = getServiceProperties();

        if (serviceProperties == null) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "Cannot get the properties of the provided service");
        }

        // Update the service registration
        if (m_serviceRegistration != null) {
            m_serviceRegistration.setProperties(serviceProperties);
        }
    }

    /**
     * Add properties to the list.
     * 
     * @param props : properties to add
     */
    protected void addProperties(Dictionary props) {
        Enumeration keys = props.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = props.get(key);
            Property prop = new Property(this, key, value);
            addProperty(prop);
        }
    }

    /**
     * Remove properties from the list.
     * 
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
    public String[] getServiceSpecification() {
        return m_serviceSpecification;
    }

    /**
     * Get the service registration.
     * @return the service registration of this service.
     */
    public ServiceRegistration getServiceRegistration() {
        return m_serviceRegistration;
    }

}
