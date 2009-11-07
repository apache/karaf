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
package org.apache.felix.ipojo.handlers.configuration;

import java.util.List;

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Property;

/**
 * Configuration handler description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationHandlerDescription extends HandlerDescription {
    
    /**
     * The property descriptions.
     */
    private PropertyDescription[] m_properties;
    
    /**
     * The Managed Service PID.
     * <code>null</code> if not set.
     */
    private String m_pid;

    /**
     * Creates the description object for the configuration handler description.
     * @param handler the configuration handler.
     * @param props the list of properties.
     * @param pid the managed service pid or <code>null</code> if not set.
     */
    public ConfigurationHandlerDescription(Handler handler, List/*<Property>*/ props, String pid) {
        super(handler);
        m_properties = new PropertyDescription[props.size()];
        for (int i = 0; i < props.size(); i++) {
            m_properties[i] = new PropertyDescription((Property) props.get(i));
        }        
        m_pid = pid;
    }
    
    /**
     * The handler information.
     * @return the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element elem = super.getHandlerInfo();
        
        if (m_pid != null) {
            elem.addAttribute(new Attribute("managed.service.pid", m_pid));
        }
        
        for (int i = 0; i < m_properties.length; i++) {
            String name = m_properties[i].getName();
            Object value = m_properties[i].getValue();
            Element property = new Element("property", "");
            elem.addElement(property);
            if (name != null) {
                property.addAttribute(new Attribute("name", name));
            }
            if (value != null) {
                if (value == Property.NO_VALUE) {
                    property.addAttribute(new Attribute("value", "NO_VALUE"));
                } else {
                    property.addAttribute(new Attribute("value", value.toString()));
                }
            }
        }
        return elem;
    }
    
    /**
     * Gets the properties.
     * @return the property set.
     */
    public PropertyDescription[] getProperties() {
        return m_properties;
    }
    
    /**
     * Gets the managed service pid.
     * @return the managed service pid of <code>null</code>
     * if not set.
     */
    public String getManagedServicePid() {
        return m_pid;
    }

}
