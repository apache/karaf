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

/**
 * Component Type information.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentDescription {

    /**
     * Provided service by the component type.
     */
    private String[] m_providedServiceSpecification = new String[0];

    /**
     * Component Type implementation class.
     */
    private String m_className;

    /**
     * Configuration Properties accepted by the component type.
     */
    private PropertyDescription[] m_properties = new PropertyDescription[0];

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String res = "";
        res += "Component : " + m_className + "\n";
        for (int i = 0; i < m_providedServiceSpecification.length; i++) {
            res += "\tProvides : " + m_providedServiceSpecification[i] + "\n";
        }
        for (int i = 0; i < m_properties.length; i++) {
            res += "\tProperty : " + m_properties[i] + "\n";
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
    public void setClassName(String name) { m_className = name; }

    /**
     * @return the list of configuration properties accepted by the component type.
     */
    public PropertyDescription[] getProperties() { return m_properties; }

    /**
     * Add a configuration properties to the component type.
     * @param pd : the property to add
     */
    public void addProperty(PropertyDescription pd) { 
    	for(int i = 0; i < m_properties.length; i++) {
    		if(m_properties[i].getName().equals(pd.getName())) { return; }
    	}
    	
    	PropertyDescription[] newProps = new PropertyDescription[m_properties.length + 1];
        System.arraycopy(m_properties, 0, newProps, 0, m_properties.length);
        newProps[m_properties.length] = pd;
        m_properties = newProps;
    }

    /**
     * @return the list of the provided service.
     */
    public String[] getprovidedServiceSpecification() { return m_providedServiceSpecification; }

    /**
     * add a provided service to the component type.
     * @param serviceSpecification : the provided service to add (interface name)
     */
    public void addProvidedServiceSpecification(String serviceSpecification) { 
            String[] newSs = new String[m_providedServiceSpecification.length + 1];
            System.arraycopy(m_providedServiceSpecification, 0, newSs, 0, m_providedServiceSpecification.length);
            newSs[m_providedServiceSpecification.length] = serviceSpecification;
            m_providedServiceSpecification = newSs;
    }
    
    




}
