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
import org.apache.felix.ipojo.CompositeManager;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Service Instantiator Class. This handler allows to instantiate service
 * instance inside the composition.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ServiceInstantiatorHandler extends CompositeHandler {

    /**
     * Composite Manager.
     */
    private CompositeManager m_manager;

    /**
     * Is the handler valid ?
     */
    private boolean m_isValid = false;

    /**
     * List of instances to manage.
     */
    private List/* <SvcInstance> */m_instances = new ArrayList();

    /**
     * Configure the handler.
     * 
     * @param im : the instance manager
     * @param metadata : the metadata of the component
     * @param conf : the instance configuration
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(CompositeManager im, Element metadata, Dictionary conf) {
        m_manager = im;
        Element[] services = metadata.getElements("service");
        for (int i = 0; i < services.length; i++) {
            String spec = services[i].getAttribute("specification");
            String filter = "(&(objectClass=" + Factory.class.getName() + ")(!(service.pid=" + m_manager.getComponentDescription().getName() + ")))"; // Cannot reinstantiate yourself
            if (services[i].containsAttribute("filter")) {
                String classnamefilter = "(&(objectClass=" + Factory.class.getName() + ")(!(service.pid=" + m_manager.getComponentDescription().getName() + ")))"; // Cannot reinstantiate yourself
                filter = "";
                if (!services[i].getAttribute("filter").equals("")) {
                    filter = "(&" + classnamefilter + services[i].getAttribute("filter") + ")";
                } else {
                    filter = classnamefilter;
                }
            }
            Properties prop = new Properties();
            for (int k = 0; k < services[i].getElements("property").length; k++) {
                String key = services[i].getElements("property")[k].getAttribute("name");
                String value = services[i].getElements("property")[k].getAttribute("value");
                prop.put(key, value);
            }
            boolean agg = false;
            if (services[i].containsAttribute("aggregate") && services[i].getAttribute("aggregate").equalsIgnoreCase("true")) {
                agg = true;
            }
            boolean opt = false;
            if (services[i].containsAttribute("optional") && services[i].getAttribute("optional").equalsIgnoreCase("true")) {
                opt = true;
            }
            SvcInstance inst = new SvcInstance(this, spec, prop, agg, opt, filter);
            m_instances.add(inst);
        }
        if (m_instances.size() > 0) {
            m_manager.register(this);
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

        m_isValid = isValid();
    }

    /**
     * Check the handler validity.
     * @return true if oall created service isntance are valid
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    public boolean isValid() {
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
            if (isValid()) {
                m_manager.checkInstanceState();
            }
            m_isValid = true;
        }
    }

    /**
     * A service instance becomes invalid.
     */
    public void invalidate() {
        if (m_isValid) {
            if (!isValid()) {
                m_manager.checkInstanceState();
            }
            m_isValid = false;
        }
    }

    /**
     * Get the service instantiator handler description.
     * @return the description
     * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
     */
    public HandlerDescription getDescription() {
        return new ServiceInstantiatorDescription(this.getClass().getName(), isValid(), m_instances);
    }

    /**
     * Get the composite manager.
     * @return the composite manager.
     */
    protected CompositeManager getManager() {
        return m_manager;
    }
}
