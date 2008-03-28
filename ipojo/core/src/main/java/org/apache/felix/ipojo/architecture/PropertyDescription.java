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
     * Default value of the property.
     */
    private String m_value = null;
    
    
    /**
     * Immutable property flag
     * IF true, the property cannot be override by the instance configuration.
     * Moreover, immutable properties are exposed on the factory service too.
     */
    private boolean m_immutable = false;

    /**
     * Constructor.
     * 
     * @param name : name of the property
     * @param type : type of the property
     * @param value : default value of the property
     */
    public PropertyDescription(String name, String type, String value) {
        m_name = name;
        m_type = type;
        m_value = value;
    }
    
    /**
     * Constructor.
     * 
     * @param name : name of the property
     * @param type : type of the property
     * @param value : default value of the property
     * @param immutable : the property is immutable.
     */
    public PropertyDescription(String name, String type, String value, boolean immutable) {
        m_name = name;
        m_type = type;
        m_value = value;
        m_immutable = immutable;
    }

    /**
     * Get the current property name.
     * @return the property name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the current property type.
     * @return the property type.
     */
    public String getType() {
        return m_type;
    }

    /**
     * Get the current property value.
     * @return the default value for the property.
     */
    public String getValue() {
        return m_value;
    }
    
    /**
     * Is the property immutable.
     * @return true if the property is immutable.
     */
    public boolean isImmutable() {
        return m_immutable;
    }
    /**
     * Get the object value of the current immutable property.
     * @param context : bundle context to use to load classes.
     * @return the object value of the current property.
     */
    public Object getObjectValue(BundleContext context) {
        Class type = null;
        try {
            type = Property.computeType(m_type, context);
            return Property.create(type, m_value);
        } catch (ConfigurationException e) {
            return m_value; // Cannot create the object.
        }
    }

}
