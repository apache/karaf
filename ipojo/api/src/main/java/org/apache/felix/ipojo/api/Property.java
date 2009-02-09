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
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows configuring a configuration property.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Property {
    
    /**
     * The property name.
     */
    private String m_name;
    
    /**
     * The property field. 
     */
    private String m_field;
    
    /**
     * The property value. 
     */
    private String m_value;
    
    /**
     * The property method. 
     */
    private String m_method;
    
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
    public Property setName(String name) {
        m_name = name;
        return this;
    }
    
    /**
     * Sets the property field.
     * @param name the property field
     * @return the current property object
     */
    public Property setField(String name) {
        m_field = name;
        return this;
    }
    
    /**
     * Sets the property method.
     * @param name the property method
     * @return the current property object
     */
    public Property setMethod(String name) {
        m_method = name;
        return this;
    }
    
    /**
     * Sets the property value.
     * @param name the property value
     * @return the current property object
     */
    public Property setValue(String name) {
        m_value = name;
        return this;
    }
    
    /**
     * Sets if the property is mandatory.
     * @param mandatory <code>true</code> if the dependency is mandatory.
     * @return the current property object
     */
    public Property setMandatory(boolean mandatory) {
        m_mandatory = mandatory;
        return this;
    }
    
    /**
     * Sets if the property is immutable.
     * @param immutable <code>true</code> if the dependency is immutable.
     * @return the current property object
     */
    public Property setImmutable(boolean immutable) {
        m_immutable = immutable;
        return this;
    }
    
    /**
     * Gets the property element.
     * @return the property element.
     */
    public Element getElement() {
        ensureValidity();
        Element element = new Element("property", "");
        if (m_name != null) {
            element.addAttribute(new Attribute("name", m_name));
        }
        if (m_method != null) {
            element.addAttribute(new Attribute("method", m_method));
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
        // Field or Method
        if (m_field == null && m_method == null) {
            throw new IllegalStateException("A property must have either a field or a method");
        }
        if (m_immutable && m_value == null) {
            throw new IllegalStateException("A immutable service property must have a value");
        }
    }
    
    /**
     * Gets the property description for the current property.
     * @param instance the component instance on which looking for the property.
     * @return the property description associated with the current property 
     * or <code>null</code> if not found.
     */
    public PropertyDescription getPropertyDescription(ComponentInstance instance) {
        PrimitiveInstanceDescription desc = (PrimitiveInstanceDescription) instance.getInstanceDescription();
        PropertyDescription[] props = desc.getProperties();
        
        for (int i = 0; i < props.length; i++) {
            if ((m_name != null && m_name.equals(props[i].getName())) 
                    || (m_field != null && m_field.equals(props[i].getName()))) {
                return props[i];
            }
        }
           
        return null;
    }
    
}
