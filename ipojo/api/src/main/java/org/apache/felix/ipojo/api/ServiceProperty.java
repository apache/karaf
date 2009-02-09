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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows configuring a service property.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceProperty {
    
    /**
     * The property name.
     */
    private String m_name;
    
    /**
     * The property field. 
     */
    private String m_field;
    
    /**
     * The property type. 
     */
    private String m_type;
    
    /**
     * The property value. 
     */
    private String m_value;
    
    /**
     * Is the property mandatory. 
     */
    private boolean m_mandatory;
    
    /**
     * Is the property immutable. 
     */
    private boolean m_immutable;
    
    /**
     * Sets the property name.
     * @param name the property name
     * @return the current property object
     */
    public ServiceProperty setName(String name) {
        m_name = name;
        return this;
    }
    
    /**
     * Sets the property field.
     * @param name the property field
     * @return the current property object
     */
    public ServiceProperty setField(String name) {
        m_field = name;
        return this;
    }
    
    /**
     * Sets the property type.
     * @param name the property type
     * @return the current property object
     */
    public ServiceProperty setType(String name) {
        m_type = name;
        return this;
    }
    
    /**
     * Sets the property value.
     * @param name the property value
     * @return the current property object
     */
    public ServiceProperty setValue(String name) {
        m_value = name;
        return this;
    }
    
    /**
     * Sets if the property is immutable.
     * @param immutable <code>true</code> if the dependency is immutable.
     * @return the current property object
     */
    public ServiceProperty setImmutable(boolean immutable) {
        m_immutable = immutable;
        return this;
    }
    
    /**
     * Sets if the property is mandatory.
     * @param mandatory <code>true</code> if the dependency is mandatory.
     * @return the current property object
     */
    public ServiceProperty setMandatory(boolean mandatory) {
        m_mandatory = mandatory;
        return this;
    }
    
    
    /**
     * Gets the 'property' element.
     * @return the element describing the current property.
     */
    public Element getElement() {
        ensureValidity();
        Element element = new Element("property", "");
        if (m_name != null) {
            element.addAttribute(new Attribute("name", m_name));
        }
        if (m_type != null) {
            element.addAttribute(new Attribute("type", m_type));
        }
        if (m_value != null) {
            element.addAttribute(new Attribute("value", m_value));
        }
        if (m_field != null) {
            element.addAttribute(new Attribute("field", m_field));
        }
        if (m_mandatory) {
            element.addAttribute(new Attribute("mandatory", new Boolean(m_mandatory).toString()));
        }
        if (m_immutable) {
            element.addAttribute(new Attribute("immutable", new Boolean(m_immutable).toString()));
        }
        return element;
    }

    /**
     * Checks the configuration validity.
     */
    private void ensureValidity() {
        // Two cases
        // Field or type
        if (m_field == null && m_type == null) {
            throw new IllegalStateException("A service property must have either a field or a type");
        }
        if (m_immutable && m_value == null) {
            throw new IllegalStateException("A immutable service property must have a value");
        }
    }
    
    /**
     * Gets the property value of the current property
     * on the given instance.
     * @param instance the instance on which looking for 
     * the property value
     * @return the property value or <code>null</code>
     * if not found.
     */
    public Object getPropertyValue(ComponentInstance instance) {
        PrimitiveInstanceDescription desc = (PrimitiveInstanceDescription) instance.getInstanceDescription();
        ProvidedServiceDescription[] pss = desc.getProvidedServices();
        for (int i = 0; i < pss.length; i++) {
            // Check with the name
            if (m_name != null && pss[i].getProperties().containsKey(m_name)) {
                return pss[i].getProperties().get(m_name);
            }
            // Check with the field
            if (m_field != null && pss[i].getProperties().containsKey(m_field)) {
                return pss[i].getProperties().get(m_field);
            }
        }
        // Not found.
        return null;
    }


}
