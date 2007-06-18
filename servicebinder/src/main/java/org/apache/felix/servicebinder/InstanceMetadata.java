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
package org.apache.felix.servicebinder;

import java.util.Properties;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Description of an instance entry in the descriptor file *  * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceMetadata
{
    // These properties will be filled by the parser
    private String m_implementorName;
    private Collection m_interfaces = null;
    private Properties m_properties = new Properties();
    private Collection m_dependencies = null;

    /**
     * 
     * @uml.property name="m_instantiates"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private InstanceMetadata m_instantiates = null;

    /**
     * 
     * @uml.property name="m_parent"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private InstanceMetadata m_parent = null;

    private boolean m_isFactory = false;
    private boolean m_registersService = false;
    private boolean m_isInstance = false;

    /**
     * Constructor
     *
     * @param   implementorName name of the class of the implementation object
     * @param   parent the parent to this service descriptor
     */
    InstanceMetadata(String implementorName,InstanceMetadata parent)
    {
        m_interfaces = new ArrayList();
        m_dependencies = new ArrayList();

        m_implementorName = implementorName;

        // The parent will be != null if this descriptor corresponds to an
        // instantiate entry

        if(parent != null)
        {
            m_parent = parent;
            m_parent.m_instantiates = this;
            m_parent.m_isFactory = true;
            m_isInstance = true;
        }
    }

    /**
     * Returns the name of the implementor
     *
     * @return the name of the implementor
     */
    public String getImplementorName()
    {
        return m_implementorName;
    }


    /**
     * Used to add an interface to the service descriptor
     *
     * @param   newInterface name of the interface implemented by the implementation object
     */
    void addInterface(String newInterface)
    {
        // As soon as there is one interface provided, it cannot be a bundle-to-service.
        m_registersService = true;
        m_interfaces.add(newInterface);
    }

    /**
     * Returns the implemented interfaces
     *
     * @return the implemented interfaces as a string array
     */
    public String [] getInterfaces()
    {
        String interfaces[] = new String[m_interfaces.size()];
        Iterator it = m_interfaces.iterator();
        int count = 0;
        while (it.hasNext())
        {
            interfaces[count++] = it.next().toString();
        }
        return interfaces;
    }

    /**
     * Used to add a property to the instance
     *
     * @param   newProperty a property descriptor
     */
    void addProperty(PropertyMetadata newProperty)
    {
        String key = newProperty.getName();
        Object value = newProperty.getValue();
        if(key != null && value != null)
        {
            m_properties.put(key,value);
        }
    }

    /**
     * Returns the property descriptors
     *
     * @return the property descriptors as a Collection
     */
    public Properties getProperties()
    {
        return m_properties;
    }

    /**
     * Used to add a dependency descriptor to the service descriptor
     *
     * @param newDependency a new dependency to be added
     */
    void addDependency(DependencyMetadata newDependency)
    {
        m_dependencies.add(newDependency);
    }


    /**
     * Returns the dependency descriptors
     *
     * @return a Collection of dependency descriptors
     */
    public Collection getDependencies()
    {
        return m_dependencies;
    }

    /**
     * Test to see if this service is a factory
     *
     * @return true if it is a factory, false otherwise
     */
    public boolean isFactory()
    {
        return m_isFactory;
    }

     /**
     * Get the meta data of the instances
     *
     * @return the instance metadata
     */
    public InstanceMetadata getInstantiates()
    {
        return m_instantiates;
    }

     /**
     * Test to see if this descriptor describes a bundle-to-service dependency
     * that means that the instance does not register any services.
     *
     * @return true if the dependency is bundle-to-service
     */
    public boolean instanceRegistersServices()
    {
        return m_registersService;
    }

     /**
     * Test to see if this descriptor is registered from an instance from a factory.
     *
     * @return true if this descriptor is registered from an instance from a factory.
     */
    public boolean isInstance()
    {
        return m_isInstance;
    }
}
