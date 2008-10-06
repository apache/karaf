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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Property;
import org.osgi.framework.Bundle;
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
    public static final int SINGLETON_FACTORY = 0;

    /**
     * Factory policy : SERVICE_FACTORY.
     */
    public static final int SERVICE_FACTORY = 1;
    
    /**
     * Factory policy : STATIC_FACTORY.
     */
    public static final int STATIC_FACTORY = 2;

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
    private Property[] m_properties;

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
        
        // Add instance name & factory name
        try {
            addProperty(new Property("instance.name", null, null, handler.getInstanceManager().getInstanceName(), String.class.getName(), handler.getInstanceManager(), handler));       
            addProperty(new Property("factory.name", null, null, handler.getInstanceManager().getFactory().getFactoryName(), String.class.getName(), handler.getInstanceManager(), handler));
        } catch (ConfigurationException e) {
            m_handler.error("An exception occurs when adding instance.name and factory.name property : " + e.getMessage());
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
        Object svc = null;
        switch (m_factoryPolicy) {
            case SINGLETON_FACTORY:
                svc = m_handler.getInstanceManager().getPojoObject();
                break;
            case SERVICE_FACTORY:
                svc = m_handler.getInstanceManager().createPojoObject();
                break;
            case STATIC_FACTORY:
                // In this case, we need to try to create a new pojo object, the factory method will handle the creation.
                svc = m_handler.getInstanceManager().createPojoObject();
                break;
            default:
                m_handler.error("[" + m_handler.getInstanceManager().getClassName() + "] Unknown factory policy for " + m_serviceSpecification + " : " + m_factoryPolicy);
                getInstanceManager().stop();
                break;
        }
        return svc;
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
     * service. To avoid cycle in Check Context, the registered service is set to
     * registered before the real registration.
     */
    protected synchronized void registerService() {
        if (m_serviceRegistration == null) {
            // Build the service properties list
            Properties serviceProperties = getServiceProperties();
            m_serviceRegistration = m_handler.getInstanceManager().getContext().registerService(m_serviceSpecification, this, serviceProperties);
        }
    }

    /**
     * Unregister the service.
     */
    protected synchronized void unregisterService() {
        if (m_serviceRegistration != null) {
            m_serviceRegistration.unregister();
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
            if (m_properties[i].getValue() != null) {
                serviceProperties.put(m_properties[i].getName(), m_properties[i].getValue());
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
