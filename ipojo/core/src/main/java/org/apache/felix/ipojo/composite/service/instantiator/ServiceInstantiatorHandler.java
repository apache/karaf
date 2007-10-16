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
package org.apache.felix.ipojo.composite.service.instantiator;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Service Instantiator Class. This handler allows to instantiate service
 * instance inside the composition.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceInstantiatorHandler extends CompositeHandler {

    /**
     * Is the handler valid ?
     * (Lifecycle controller)
     */
    private boolean m_isValid;

    /**
     * List of instances to manage.
     */
    private List/* <SvcInstance> */m_instances = new ArrayList();

    /**
     * Configure the handler.
     * 
     * @param metadata : the metadata of the component
     * @param conf : the instance configuration
     * @throws ConfigurationException : the specification attribute is missing
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf) throws ConfigurationException {
        Element[] services = metadata.getElements("service");
        for (int i = 0; i < services.length; i++) {
            String spec = services[i].getAttribute("specification");
            if (spec == null) {
                throw new ConfigurationException("Malformed service : the specification attribute is mandatory");
            }
            String filter = "(&(!(factory.name=" + getCompositeManager().getFactory().getComponentDescription().getName() + "))(factory.state=1))"; // Cannot reinstantiate yourself
            String f = services[i].getAttribute("filter");
            if (f != null) {
                filter = "(&" + filter + f + ")";
            }
            Properties prop = new Properties();
            for (int k = 0; k < services[i].getElements("property").length; k++) {
                String key = services[i].getElements("property")[k].getAttribute("name");
                String value = services[i].getElements("property")[k].getAttribute("value");
                prop.put(key, value);
            }
            String ag = services[i].getAttribute("aggregate");
            boolean agg = ag != null && ag.equalsIgnoreCase("true");
            
            String op = services[i].getAttribute("optional");
            boolean opt = op != null && op.equalsIgnoreCase("true");
            
            SvcInstance inst = new SvcInstance(this, spec, prop, agg, opt, filter);
            m_instances.add(inst);
        }
    }

    /**
     * Start the service instantiator handler.
     * Start all created service instance.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        // Init
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            inst.start();
        }

        m_isValid = isHandlerValid();
    }

    /**
     * Check the handler validity.
     * @return true if all created service instances are valid
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    private boolean isHandlerValid() {
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            if (!inst.isSatisfied()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler stop method.
     * Stop all created service instance.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            inst.stop();
        }
        m_instances.clear();
    }

    /**
     * An service instance becomes valid.
     */
    public void validate() {
        if (!m_isValid) {
            if (isHandlerValid()) {
                m_isValid = true;
            }
        }
    }

    /**
     * A service instance becomes invalid.
     */
    public void invalidate() {
        if (m_isValid) {
            if (!isHandlerValid()) {
                m_isValid = false;
            }
        }
    }

    /**
     * Get the service instantiator handler description.
     * @return the description
     * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
     */
    public HandlerDescription getDescription() {
        return new ServiceInstantiatorDescription(this, m_instances);
    }
    
    public List getInstances() {
        return m_instances;
    }
}
