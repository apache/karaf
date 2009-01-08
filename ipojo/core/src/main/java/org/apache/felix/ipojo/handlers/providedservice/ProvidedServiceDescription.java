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
import java.util.Properties;

import org.apache.felix.ipojo.util.Property;
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
    public static final int UNREGISTERED = ProvidedService.UNREGISTERED;

    /**
     * State : the service is registered.
     */
    public static final int REGISTERED = ProvidedService.REGISTERED;
    
    /**
     * The describe provided service.
     */
    private ProvidedService m_ps;

    /**
     * Constructor.
     * @param ps the described provided service.
     */
    public ProvidedServiceDescription(ProvidedService ps) {
        m_ps = ps;
    }

    /**
     * Gets the list of provided service specifications.
     * @return the provided contract name.
     */
    public String[] getServiceSpecifications() {
        return m_ps.getServiceSpecifications();
    }

    /**
     * Gets the list of properties.
     * A copy of the actual property set is returned.
     * @return the properties.
     */
    public Properties getProperties() {
        Properties props = new Properties();
        org.apache.felix.ipojo.util.Property[] ps = m_ps.getProperties();
        for (int i = 0; i < ps.length; i++) {
            if (ps[i].getValue() != Property.NO_VALUE) {
                props.put(ps[i].getName(), ps[i].getValue());
            }
        }
        return props;
    }
    
    /**
     * Adds and Updates service properties.
     * Existing properties are updated. 
     * New ones are added.
     * @param props the new properties
     */
    public void addProperties(Dictionary props) {
        m_ps.addProperties(props);
    }
    
    /**
     * Removes service properties.
     * @param props the properties to remove
     */
    public void removeProperties(Dictionary props) {
        m_ps.deleteProperties(props);
    }

    /**
     * Gets provided service state.
     * @return the state of the provided service (UNREGISTERED | REGISTRED).
     */
    public int getState() {
        return m_ps.getState();
    }

    /**
     * Gets the service reference.
     * @return the service reference (null if the service is unregistered).
     */
    public ServiceReference getServiceReference() {
        return m_ps.getServiceReference();
    }
    
    /**
     * Gets the 'main' service object.
     * @return the 'main' service object or <code>null</code>
     * if no service object are created.
     */
    public Object getService() {
        Object[] objs = m_ps.getInstanceManager().getPojoObjects();
        if (objs == null) { 
            return null;
        } else {
            return objs[0];
        }
    }
    
    public Object[] getServices() {
        return m_ps.getInstanceManager().getPojoObjects();
    }
    

}
