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
package org.apache.felix.ipojo;

/**
 * Property Information.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class PropertyInfo {

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
     * Constructor.
     * @param name : name of the property
     * @param type : type of the property
     * @param value : default value of the property
     */
    public PropertyInfo(String name, String type, String value) {
        m_name = name;
        m_type = type;
        m_value = value;
    }

    /**
     * @return the property name.
     */
    public String getName() { return m_name; }

    /**
     * @return the property type.
     */
    public String getType() { return m_type; }

    /**
     * @return the default value for the property.
     */
    public String getValue() { return m_value; }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        if (m_value != null) { return getName() + " - " + getType() + " - " + getValue(); }
        else { return getName() + " - " + getType() + " - CONFIGURABLE"; }
    }

}
