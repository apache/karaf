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
package org.apache.felix.ipojo.architecture;

import java.util.Properties;

import org.osgi.framework.ServiceReference;

/**
 * Provided Service Description.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceDescription {

    /**
     * Provided Service Specification.
     */
    private String[] m_serviceSpecification;

    /**
     * Dependency of the service.
     */
    private DependencyDescription[] m_dependencies = new DependencyDescription[0];

    /**
     * State.
     */
    private int m_state;

    /**
     * The service reference.
     */
    private ServiceReference m_serviceReference;

    /**
     * Handler on the component description who contains this description.
     */
    private InstanceDescription m_parent;

    /**
     * Properties of the provided service.
     */
    private Properties m_properties = new Properties();


    /**
     * Constructor.
     * @param serviceSpecification : the provided contract
     * @param state : state (UNREGITRED | REGISTRED)
     * @param sr : Service Registration (to obtain the reference), or null if state is UNREGISTRED
     * @param parent : the component description declaring this proided service
     */
    public ProvidedServiceDescription(String[] serviceSpecification, int state, ServiceReference sr, InstanceDescription parent) {
        m_serviceSpecification = serviceSpecification;
        m_state = state;
        m_serviceReference = sr;
        m_parent = parent;
    }

    /**
     * @return the provided contract name.
     */
    public String[] getServiceSpecification() {
        return m_serviceSpecification;
    }

    /**
     * Add a property to the current provided service description.
     * @param key : the key of the property
     * @param value : the value of the property
     */
    public void addProperty(String key, String value) {
        m_properties.put(key, value);
    }

    /**
     * Set the set of properties. This function create a clone of the argument.
     * @param props : the properties
     */
    public void setProperty(Properties props) {
        m_properties = (Properties) props.clone();
    }

    /**
     * @return the dependeny description list.
     */
    public DependencyDescription[] getDependencies() {
        return m_dependencies;
    }

    /**
     * @return the properties.
     */
    public Properties getProperties() {
        return m_properties;
    }

    /**
     * @return the state of the provided service (UNREGISTRED | REGISTRED).
     */
    public int getState() {
        return m_state;
    }

    /**
     * @return the service reference (null if the service is unregistred).
     */
    public ServiceReference getServiceReference() {
        return m_serviceReference;
    }

    /**
     * @return the parent description.
     */
    public InstanceDescription getInstanceDescription() {
        return m_parent;
    }

}
