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
package org.apache.felix.ipojo.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows configuring a provided service.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Service implements HandlerConfiguration {
    
    /**
     * Creation strategy : singleton (default).
     */
    public static final int SINGLETON_STRATEGY = ProvidedService.SINGLETON_STRATEGY;
    
    /**
     * Creation strategy : delegate on the static factory method.
     */
    public static final int STATIC_STRATEGY = ProvidedService.STATIC_STRATEGY;
    
    /**
     * Creation strategy : one service object per instance.
     */
    public static final int INSTANCE_STRATEGY = ProvidedService.INSTANCE_STRATEGY;
    
    /**
     * Creation strategy : one service object per bundle (OSGi service factory).
     */
    public static final int  SERVICE_STRATEGY = ProvidedService.SERVICE_STRATEGY;
    
    /**
     * The provided service specification. 
     */
    private List m_specifications; // null be default computed. 
    
    /**
     * The provided service strategy. 
     */
    private int m_strategy = ProvidedService.SINGLETON_STRATEGY;
    
    /**
     * The provided service custom strategy. 
     */
    private String m_customStrategy;
    
    /**
     * The service properties. 
     */
    private List m_properties = new ArrayList();
    
    /**
     * Service controller.
     */
    private String m_controller;
    
    /**
     * Service Controller value.
     */
    private boolean m_controllerValue;
    
    /**
     * Gets the provided service element.
     * @return the 'provides' element.
     */
    public Element getElement() {
        ensureValidity();
        Element element = new Element("provides", "");
        if (m_specifications != null) {
            element.addAttribute(new Attribute("specifications", getSpecificationsArray()));
        }
        element.addAttribute(new Attribute("strategy", getStringStrategy()));
        for (int i = 0; i < m_properties.size(); i++) {
            element.addElement(((ServiceProperty) m_properties.get(i)).getElement());
        }
        
        if (m_controller != null) {
            Element ctrl = new Element("controller", "");
            ctrl.addAttribute(new Attribute("field", m_controller));
            ctrl.addAttribute(new Attribute("value", String.valueOf(m_controllerValue)));
            element.addElement(ctrl);
        }
        
        return element;   
    }
    
    /**
     * Gets the provided service description associated with the current service.
     * @param instance the instance on which looking for the provided service description
     * @return the provided service description or <code>null</code> if not found. 
     */
    public ProvidedServiceDescription getProvidedServiceDescription(ComponentInstance instance) {
        PrimitiveInstanceDescription desc = (PrimitiveInstanceDescription) instance.getInstanceDescription();
        ProvidedServiceDescription[] pss = desc.getProvidedServices();
        if (pss.length == 0) {
            return null;
        }
        
        if (pss.length == 1) {
            return pss[0];
        }
        
        if (m_specifications == null) {
            return null;
        } else {
            for (int j = 0; j < pss.length; j++) {
                ProvidedServiceDescription psd = pss[j];
                List specs = Arrays.asList(psd.getServiceSpecifications());
                if (specs.containsAll(m_specifications)) {
                    return psd;
                }
            }
        }
        
        return null;
    }


    
    /**
     * Checks the validity of the configuration.
     */
    private void ensureValidity() {
        // No check required.
    }


    /**
     * The the service specification array as a String.
     * @return the string-from of the service specifications.
     */
    private String getSpecificationsArray() {
        if (m_specifications.size() == 1) {
            return (String) m_specifications.get(0);
        } else {
            StringBuffer buffer = new StringBuffer("{");
            for (int i = 0; i < m_specifications.size(); i++) {
                if (i != 0) {
                    buffer.append(',');
                }
                buffer.append(m_specifications.get(i));
            }
            buffer.append('}');
            return buffer.toString();
        }
    }
    
    /**
     * Adds a service property.
     * @param ps the service property to add
     * @return the current service object.
     */
    public Service addProperty(ServiceProperty ps) {
        m_properties.add(ps);
        return this;
    }
    
    /**
     * Adds a service property.
     * @param key the property key
     * @param obj the initial value (can be <code>null</code>)
     * @return the current service object.
     */
    public Service addProperty(String key, Object obj) {
        Class clazz = String.class;
        String value = null;
        if (obj != null) {
            clazz = obj.getClass();
            value = obj.toString();
        }

        addProperty(new ServiceProperty().setName(key).setType(clazz.getName())
                .setValue(value));

        return this;
    }

    /**
     * Sets the provided service specification.
     * @param spec the service specification
     * @return the current service object.
     */
    public Service setSpecification(String spec) {
        m_specifications = new ArrayList(1);
        m_specifications.add(spec);
        return this;
    }
    
    /**
     * Sets the provided service specifications.
     * @param specs the service specifications
     * @return the current service object.
     */
    public Service setSpecifications(List specs) {
        m_specifications  = specs;
        return this;
    }
    
    /**
     * Sets the creation strategy.
     * @param strategy the service strategy.
     * @return the current service object
     */
    public Service setCreationStrategy(int strategy) {
        m_strategy = strategy;
        return this;
    }
    
    /**
     * Sets the creation strategy.
     * This method allows using a customized
     * service strategy.
     * @param strategy the service strategy
     * @return the current service object
     */
    public Service setCreationStrategy(String strategy) {
        m_strategy = -1; // Custom
        m_customStrategy = strategy;
        return this;
    }
    
    /**
     * Sets the service controller.
     * @param field the controller field
     * @param initialValue the initial value
     * @return the current servic object
     */
    public Service setServiceController(String field, 
            boolean initialValue) {
        m_controller = field;
        m_controllerValue = initialValue;
        return this;
    }
    
    /**
     * Gets the string-form of the creation strategy.
     * @return the creation strategy string form
     */
    private String getStringStrategy() {
        switch (m_strategy) {
            case -1: // Custom policies
                return m_customStrategy;
            case ProvidedService.SINGLETON_STRATEGY:
                return "singleton";
            case ProvidedService.STATIC_STRATEGY:
                return "method";
            case ProvidedService.SERVICE_STRATEGY:
                return "service";
            case ProvidedService.INSTANCE_STRATEGY:
                return "instance";
            default:
                throw new IllegalStateException("Unknown creation strategy :  "
                        + m_strategy);
        }
    }

}
