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

import java.util.ArrayList;
import java.util.List;
/**
 * Component Type information.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentInfo {

    /**
     * Provided service by the component type.
     */
    private ArrayList m_providedServiceSpecification = new ArrayList();

    /**
     * Component Type implementation class.
     */
    private String m_className;

    /**
     * Configuration Properties accepted by the component type.
     */
    private ArrayList m_properties = new ArrayList();

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String res = "";
        res += "Component : " + m_className + "\n";
        for (int i = 0; i < m_providedServiceSpecification.size(); i++) {
            res += "\tProvides : " + (String) m_providedServiceSpecification.get(i) + "\n";
        }
        for (int i = 0; i < m_properties.size(); i++) {
            res += "\tProperty : " + (PropertyInfo) m_properties.get(i) + "\n";
        }
        return res;
    }

    /**
     * @return the component type implementation class name.
     */
    public String getClassName() { return m_className; }

    /**
     * Set the component type implementation class name.
     * @param name : the name of the implementation class
     */
    protected void setClassName(String name) { m_className = name; }

    /**
     * @return the list of configuration properties accepted by the component type.
     */
    public List getProperties() { return m_properties; }

    /**
     * Add a configuration properties to the component type.
     * @param pi : the property to add
     */
    public void addProperty(PropertyInfo pi) { this.m_properties.add(pi); }

    /**
     * @return the list of the provided service.
     */
    public List getprovidedServiceSpecification() { return m_providedServiceSpecification; }

    /**
     * add a provided service to the component type.
     * @param serviceSpecification : the provided service to add (interface name)
     */
    public void addProvidedServiceSpecification(String serviceSpecification) { m_providedServiceSpecification.add(serviceSpecification); }




}
