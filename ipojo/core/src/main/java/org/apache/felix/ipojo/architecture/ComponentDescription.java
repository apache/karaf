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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Component Type description.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentDescription {

    /**
     * Provided service by the component type.
     */
    private String[] m_providedServiceSpecification = new String[0];

    /**
     * Configuration Properties accepted by the component type.
     */
    private PropertyDescription[] m_properties = new PropertyDescription[0];
    
    /**
     * List of required properties.
     * This list contains only property which does not have a default value.
     */
    private List m_requiredProperties = new ArrayList();


    /**
     * Represented factory.
     */
    private Factory m_factory;
    
    /**
     * Constructor.
     * @param factory : represented factory.
     */
    public ComponentDescription(Factory factory) {
        m_factory = factory;
    }
    
    public List getRequiredProperties() {
        return m_requiredProperties;
    }

    /**
     * Get a printable form of the current component type description.
     * @return printable form of the component type description
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getDescription().toString();
    }

    /**
     * Get the implementation class of this component type.
     * @return the component type implementation class name.
     */
    public String getClassName() {
        return m_factory.getClassName();
    }

    /**
     * Get component-type properties.
     * @return the list of configuration properties accepted by the component type type.
     */
    public PropertyDescription[] getProperties() {
        return m_properties;
    }

    /**
     * Add a String property in the component type.
     * @param name : property name.
     * @param value : property value.
     */
    public void addProperty(String name, String value) {
        PropertyDescription pd = new PropertyDescription(name, String.class.getName(), value);
        addProperty(pd);
    }

    /**
     * Add a configuration properties to the component type.
     * @param pd : the property to add
     */
    public void addProperty(PropertyDescription pd) {
        String n = pd.getName();
        if ("name".equals(n)) {
            pd = new PropertyDescription(n, pd.getType(), null); // Instance name case.
        }
        
        // Check if the property is not already in the array
        for (int i = 0; i < m_properties.length; i++) {
            PropertyDescription desc = m_properties[i];
            if (desc.getName().equals(n)) {
                return;
            }
        }

        PropertyDescription[] newProps = new PropertyDescription[m_properties.length + 1];
        System.arraycopy(m_properties, 0, newProps, 0, m_properties.length);
        newProps[m_properties.length] = pd;
        m_properties = newProps;
        
        if (pd.getValue() == null) {
            m_requiredProperties.add(n);
        }
    }

    /**
     * Get the list of provided service offered by instances of this type.
     * @return the list of the provided service.
     */
    public String[] getprovidedServiceSpecification() {
        return m_providedServiceSpecification;
    }

    /**
     * Add a provided service to the component type.
     * @param serviceSpecification : the provided service to add (interface name)
     */
    public void addProvidedServiceSpecification(String serviceSpecification) {
        String[] newSs = new String[m_providedServiceSpecification.length + 1];
        System.arraycopy(m_providedServiceSpecification, 0, newSs, 0, m_providedServiceSpecification.length);
        newSs[m_providedServiceSpecification.length] = serviceSpecification;
        m_providedServiceSpecification = newSs;
    }

    /**
     * Return the component-type name.
     * @return the name of this component type
     */
    public String getName() {
        return m_factory.getName();
    }

    /**
     * Get the component type description.
     * @return : the description
     */
    public Element getDescription() {
        Element desc = new Element("Factory", "");

        desc.addAttribute(new Attribute("name", m_factory.getName()));
        desc.addAttribute(new Attribute("bundle", "" + ((ComponentFactory) m_factory).getBundleContext().getBundle().getBundleId()));

        String cn = getClassName();
        if (cn == null) {
            desc.addAttribute(new Attribute("Composite", "true"));
        } else {
            desc.addAttribute(new Attribute("Implementation-Class", getClassName()));
        }

        String state = "valid";
        if (m_factory.getState() == Factory.INVALID) {
            state = "invalid";
        }
        desc.addAttribute(new Attribute("state", state));

        // Display required & missing handlers
        Element rh = new Element("RequiredHandlers", "");
        rh.addAttribute(new Attribute("list", m_factory.getRequiredHandlers().toString()));
        Element mh = new Element("MissingHandlers", "");
        mh.addAttribute(new Attribute("list", m_factory.getMissingHandlers().toString()));
        desc.addElement(rh);
        desc.addElement(mh);

        for (int i = 0; i < m_providedServiceSpecification.length; i++) {
            Element prov = new Element("provides", "");
            prov.addAttribute(new Attribute("specification", m_providedServiceSpecification[i]));
            desc.addElement(prov);
        }

        for (int i = 0; i < m_properties.length; i++) {
            Element prop = new Element("property", "");
            prop.addAttribute(new Attribute("name", m_properties[i].getName()));
            prop.addAttribute(new Attribute("type", m_properties[i].getType()));
            if (m_properties[i].getValue() != null) {
                prop.addAttribute(new Attribute("value", m_properties[i].getValue()));
            } else {
                prop.addAttribute(new Attribute("value", "REQUIRED"));
            }
            desc.addElement(prop);
        }

        return desc;
    }

    public BundleContext getBundleContext() {
        return m_factory.getBundleContext();
    }

}
