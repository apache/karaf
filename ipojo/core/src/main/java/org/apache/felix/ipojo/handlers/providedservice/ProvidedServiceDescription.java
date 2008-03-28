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

import java.util.Properties;

import org.osgi.framework.ServiceReference;

/**
 * Provided Service Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceDescription {

    /**
     * State : the service is unregistered.
     */
    public static final int UNREGISTERED = 0;

    /**
     * State : the service is registered.
     */
    public static final int REGISTERED = 1;

    /**
     * Provided Service Specification.
     */
    private String[] m_serviceSpecification;

    /**
     * State.
     */
    private int m_state;

    /**
     * The service reference.
     */
    private ServiceReference m_serviceReference;

    // /**
    // * Handler on the component description who contains this description.
    // */
    // private InstanceDescription m_parent;

    /**
     * Properties of the provided service.
     */
    private Properties m_properties = new Properties();

    /**
     * Constructor.
     * 
     * @param serviceSpecification : the provided contract
     * @param state : state (UNREGITRED | REGISTRED)
     * @param ref : Service Registration (to obtain the reference), or null if
     * state is UNREGISTRED
     */
    public ProvidedServiceDescription(String[] serviceSpecification, int state, ServiceReference ref) {
        m_serviceSpecification = serviceSpecification;
        m_state = state;
        m_serviceReference = ref;
        // m_parent = parent;
    }

    /**
     * Get the list of provided service specifications.
     * @return the provided contract name.
     */
    public String[] getServiceSpecification() {
        return m_serviceSpecification;
    }

    /**
     * Add a property to the current provided service description.
     * 
     * @param key : the key of the property
     * @param value : the value of the property
     */
    public void addProperty(String key, String value) {
        m_properties.put(key, value);
    }

    /**
     * Set the set of properties. This function create a clone of the argument.
     * 
     * @param props : the properties
     */
    public void setProperty(Properties props) {
        m_properties = props;
    }

    /**
     * Get the list of properties.
     * @return the properties.
     */
    public Properties getProperties() {
        return m_properties;
    }

    /**
     * Get provided service state.
     * @return the state of the provided service (UNREGISTERED | REGISTRED).
     */
    public int getState() {
        return m_state;
    }

    /**
     * Get the service reference.
     * @return the service reference (null if the service is unregistred).
     */
    public ServiceReference getServiceReference() {
        return m_serviceReference;
    }

}
