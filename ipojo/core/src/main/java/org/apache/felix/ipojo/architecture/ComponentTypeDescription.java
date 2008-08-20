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

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * Component Type description.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentTypeDescription {

    /**
     * Provided service by the component type.
     */
    private String[] m_providedServiceSpecification = new String[0];

    /**
     * Configuration Properties accepted by the component type.
     */
    private PropertyDescription[] m_properties = new PropertyDescription[0];

    /**
     * Represented factory.
     */
    private final Factory m_factory;
    
    /**
     * Constructor.
     * @param factory : represented factory.
     */
    public ComponentTypeDescription(Factory factory) {
        m_factory = factory;
    }

    /**
     * Gets a printable form of the current component type description.
     * @return printable form of the component type description
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getDescription().toString();
    }

    /**
     * Gets the implementation class of this component type.
     * @return the component type implementation class name.
     */
    public String getClassName() {
        return m_factory.getClassName();
    }

    /**
     * Gets component-type properties.
     * @return the list of configuration properties accepted by the component type type.
     */
    public PropertyDescription[] getProperties() {
        return m_properties;
    }

    /**
     * Adds a String property in the component type.
     * @param name : property name.
     * @param value : property value.
     */
    public void addProperty(String name, String value) {
        addProperty(name, value, false);
    }
    
    /**
     * Adds a String property in the component type.
     * @param name : property name.
     * @param value : property value.
     * @param immutable : the property is immutable.
     */
    public void addProperty(String name, String value, boolean immutable) {
        PropertyDescription prop = new PropertyDescription(name, String.class.getName(), value);
        addProperty(prop);
    }

    /**
     * Adds a configuration properties to the component type.
     * @param pd : the property to add
     */
    public void addProperty(PropertyDescription pd) { //NOPMD remove the instance name of the 'name' property.
        String name = pd.getName();
//        if ("name".equals(name)) {
//            pd = new PropertyDescription(name, pd.getType(), null); //NOPMD Instance name case.
//        } 
        
        // Check if the property is not already in the array
        for (int i = 0; i < m_properties.length; i++) {
            PropertyDescription desc = m_properties[i];
            if (desc.getName().equals(name)) { return; }
        }

        PropertyDescription[] newProps = new PropertyDescription[m_properties.length + 1];
        System.arraycopy(m_properties, 0, newProps, 0, m_properties.length);
        newProps[m_properties.length] = pd;
        m_properties = newProps;
    }

    /**
     * Gets the list of provided service offered by instances of this type.
     * @return the list of the provided service.
     */
    public String[] getprovidedServiceSpecification() {
        return m_providedServiceSpecification;
    }

    /**
     * Adds a provided service to the component type.
     * @param serviceSpecification : the provided service to add (interface name)
     */
    public void addProvidedServiceSpecification(String serviceSpecification) {
        String[] newSs = new String[m_providedServiceSpecification.length + 1];
        System.arraycopy(m_providedServiceSpecification, 0, newSs, 0, m_providedServiceSpecification.length);
        newSs[m_providedServiceSpecification.length] = serviceSpecification;
        m_providedServiceSpecification = newSs;
    }

    /**
     * Returns the component-type name.
     * @return the name of this component type
     */
    public String getName() {
        return m_factory.getName();
    }
    
    /**
     * Computes the default service properties to publish : 
     * factory.name, service.pid, component.providedServiceSpecification, component.properties, component.description, factory.State.
     * @return : the dictionary of properties to publish.
     */
    public Dictionary getPropertiesToPublish() {
        Properties props = new Properties();

        props.put("factory.name", m_factory.getName());
        props.put(Constants.SERVICE_PID, m_factory.getName()); // Service PID is required for the integration in the configuration admin.

        props.put("component.providedServiceSpecifications", m_providedServiceSpecification);
        props.put("component.properties", m_properties);
        props.put("component.description", this);
        
        // add every immutable property
        for (int i = 0; i < m_properties.length; i++) {
            if (m_properties[i].isImmutable() && m_properties[i].getValue() != null) {
                props.put(m_properties[i].getName(), m_properties[i].getObjectValue(m_factory.getBundleContext()));
            }
        }

        // Add factory state
        props.put("factory.state", new Integer(m_factory.getState()));

        return props;

    }
    
    /**
     * Gets the interfaces published by the factory.
     * By default publish both {@link Factory} and {@link ManagedServiceFactory}.
     * @return : the list of interface published by the factory.
     */
    public String[] getFactoryInterfacesToPublish() {
        return new String[] {Factory.class.getName(), ManagedServiceFactory.class.getName()};
    }
    
    /**
     * Gets the component type description.
     * @return : the description
     */
    public Element getDescription() {
        Element desc = new Element("Factory", "");

        desc.addAttribute(new Attribute("name", m_factory.getName()));
        desc.addAttribute(
                          new Attribute("bundle", 
                                          Long.toString(((ComponentFactory) m_factory).getBundleContext().getBundle().getBundleId())));

        String state = "valid";
        if (m_factory.getState() == Factory.INVALID) {
            state = "invalid";
        }
        desc.addAttribute(new Attribute("state", state));

        // Display required & missing handlers
        Element req = new Element("RequiredHandlers", "");
        req.addAttribute(new Attribute("list", m_factory.getRequiredHandlers().toString()));
        Element missing = new Element("MissingHandlers", "");
        missing.addAttribute(new Attribute("list", m_factory.getMissingHandlers().toString()));
        desc.addElement(req);
        desc.addElement(missing);

        for (int i = 0; i < m_providedServiceSpecification.length; i++) {
            Element prov = new Element("provides", "");
            prov.addAttribute(new Attribute("specification", m_providedServiceSpecification[i]));
            desc.addElement(prov);
        }

        for (int i = 0; i < m_properties.length; i++) {
            Element prop = new Element("property", "");
            prop.addAttribute(new Attribute("name", m_properties[i].getName()));
            prop.addAttribute(new Attribute("type", m_properties[i].getType()));
            if (m_properties[i].getValue() == null) {
                prop.addAttribute(new Attribute("value", "REQUIRED"));
            } else {
                prop.addAttribute(new Attribute("value", m_properties[i].getValue()));
            }
            desc.addElement(prop);
        }

        return desc;
    }

    public BundleContext getBundleContext() {
        return m_factory.getBundleContext();
    }

}
