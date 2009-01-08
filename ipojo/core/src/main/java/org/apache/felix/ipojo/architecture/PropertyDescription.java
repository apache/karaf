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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandler;
import org.apache.felix.ipojo.util.Property;
import org.osgi.framework.BundleContext;

/**
 * Property Information.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PropertyDescription {

    /**
     * Name of the property.
     */
    private String m_name;

    /**
     * Type of the property.
     */
    private String m_type;

    /**
     * Value of the property.
     */
    private String m_value = null;
    
    /**
     * Attached property object.
     */
    private Property m_property;
    
    
    /**
     * Immutable property flag
     * If set to <code>true</code>, the property cannot be override by the instance configuration.
     * Moreover, immutable properties are exposed on the factory service too.
     */
    private boolean m_immutable = false;
    
    /**
     * A property is mandatory. So, either the component type description provides a value or
     * the instance configuration must provide a value. Immutable properties are mandatories. 
     */
    private boolean m_isMandatory = false;

    /**
     * Constructor.
     * 
     * @param name the name of the property
     * @param type the type of the property
     * @param value the default value of the property, can be <code>null</code>
     */
    public PropertyDescription(String name, String type, String value) {
        m_name = name;
        m_type = type;
        m_value = value;
    }
    
    /**
     * Constructor.
     * 
     * @param prop the attache Property object.
     */
    public PropertyDescription(Property prop) {
        m_property = prop;
        m_name = prop.getName();
        m_type = prop.getType();
        m_value = null; // Living property, value will be asked at runtime.
    }
    
    /**
     * Constructor.
     * 
     * @param name the name of the property
     * @param type the type of the property
     * @param value the default value (String form) of the property, can be <code>null</code>
     * @param immutable the property is immutable.
     */
    public PropertyDescription(String name, String type, String value, boolean immutable) {
        m_name = name;
        m_type = type;
        m_value = value;
        m_immutable = immutable;
    }

    /**
     * Gets the current property name.
     * @return the property name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets the current property type.
     * @return the property type.
     */
    public String getType() {
        return m_type;
    }

    /**
     * Gets the current property value.
     * @return the default value for the property,
     * <code>null</code> if the property hasn't a value..
     */
    public String getValue() {
        if (m_property == null) {
            return m_value;
        } else {
            Object value =  m_property.getValue();
            if (value == null) {
                return "null";
            } else {
                return value.toString();
            }
        }
    }
    
    /**
     * Sets the property value.
     * This method can only be called on 'living' property
     * (properties with a {@link Property} object).
     * @param value the new value.
     */
    public void setValue(Object value) {
        if (m_property == null) {
            throw new UnsupportedOperationException("Cannot set the value of a non 'living' property");
        } else {
            ConfigurationHandler handler = (ConfigurationHandler) m_property.getHandler();
            handler.reconfigureProperty(m_property, value);
        }
    }
    
    /**
     * Is the property immutable.
     * @return <code>true</code> if the property is immutable.
     */
    public boolean isImmutable() {
        return m_immutable;
    }
    
    /**
     * Sets the property as mandatory.
     */
    public void setMandatory() {
        m_isMandatory = true;
    }
    
    /**
     * Is the property mandatory.
     * @return <code>true</code> if the property is mandatory,
     * <code>false</code> otherwise.
     */
    public boolean isMandatory() {
        return m_isMandatory;
    }
    
    /**
     * Gets the object value of the current immutable property.
     * @param context  the bundle context to use to load classes.
     * @return the object value of the current property or <code>
     * null</code> if the current value is <code>null</code>.
     */
    public Object getObjectValue(BundleContext context) {
        if (m_value == null) {
            return null;
        }
        
        Class type = null;
        try {
            type = Property.computeType(m_type, context);
            return Property.create(type, m_value);
        } catch (ConfigurationException e) {
            return m_value; // Cannot create the object.
        }
    }

}
