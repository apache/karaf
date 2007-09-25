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

import java.util.List;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

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
     * Component Type implementation class.
     */
    private String m_className;

    /**
     * Configuration Properties accepted by the component type.
     */
    private PropertyDescription[] m_properties = new PropertyDescription[0];

    /**
     * Get the name of this component type.
     */
    private String m_name;

    /**
     * Bundle Id of the bundle containing this type.
     */
    private long m_bundleId;

    /**
     * State of the factory.
     */
    private int m_state;

    /**
     * Required handler list.
     */
    private List m_rh;

    /**
     * Missing handler list.
     */
    private List m_mh;

    /**
     * Constructor.
     * @param name : name.
     * @param className : implementation class or "composite"
     * @param state : state of the type (valid or invalid)
     * @param rh : required handler list
     * @param mh : missing handler list
     * @param bundle : bundle containing the type
     */
    public ComponentDescription(String name, String className, int state, List rh, List mh, long bundle) {
        m_name = name;
        m_className = className;
        m_bundleId = bundle;
        m_state = state;
        m_rh = rh;
        m_mh = mh;
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
        return m_className;
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
        if ("name".equals(pd.getName())) {
            pd = new PropertyDescription(pd.getName(), pd.getType(), null); // Erase the instance name
        }

        for (int i = 0; i < m_properties.length; i++) {
            if (m_properties[i].getName().equals(pd.getName())) { return; }
        }

        PropertyDescription[] newProps = new PropertyDescription[m_properties.length + 1];
        System.arraycopy(m_properties, 0, newProps, 0, m_properties.length);
        newProps[m_properties.length] = pd;
        m_properties = newProps;
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
        return m_name;
    }

    /**
     * Get the component type description.
     * @return : the description
     */
    public Element getDescription() {
        Element desc = new Element("Factory", "");

        desc.addAttribute(new Attribute("name", m_name));
        desc.addAttribute(new Attribute("bundle", "" + m_bundleId));

        if (m_className != null) {
            desc.addAttribute(new Attribute("Implementation-Class", m_className));
        } else {
            desc.addAttribute(new Attribute("Composite", "true"));
        }

        String state = "valid";
        if (m_state == Factory.INVALID) {
            state = "invalid";
        }
        desc.addAttribute(new Attribute("state", state));

        // Display required & missing handlers
        Element rh = new Element("RequiredHandlers", "");
        rh.addAttribute(new Attribute("list", m_rh.toString()));
        Element mh = new Element("MissingHandlers", "");
        mh.addAttribute(new Attribute("list", m_mh.toString()));
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

}
