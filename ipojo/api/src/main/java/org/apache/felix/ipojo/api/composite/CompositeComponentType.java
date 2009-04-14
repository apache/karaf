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
package org.apache.felix.ipojo.api.composite;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.api.ComponentType;
import org.apache.felix.ipojo.composite.CompositeFactory;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Allows defining composite types.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeComponentType extends ComponentType {
    
    /**
     * The bundle context.
     */
    private BundleContext m_context;
    
    /**
     * Component factory attached to the component
     * type. 
     */
    private ComponentFactory m_factory;
    
    /**
     * Component type metadata. 
     */
    private Element m_metadata;
    
    /**
     * List of provided services. 
     */
    private List m_provided = new ArrayList(1);
    
    /**
     * List of exported services. 
     */
    private List m_exported = new ArrayList(1);
    
    /**
     * List of imported services. 
     */
    private List m_imported = new ArrayList(1);
    
    /**
     * List of instantiated services. 
     */
    private List m_instantiated = new ArrayList();
    
    /**
     * List of contained instance:
     */
    private List m_contained = new ArrayList(); 
    
    /**
     * Is the factory public? 
     */
    private boolean m_public = true;

    private String m_name;

    /**
     * Checks that the component type is not already
     * started.
     */
    private void ensureNotInitialized() {
        if (m_factory != null) {
            throw new IllegalStateException("The component type was already initialized, cannot modify metadata");
        }
    }
    
    /**
     * Checks that the component type description is valid.
     */
    private void ensureValidity() {
        if (m_context == null) {
            throw new IllegalStateException("The primitive component type has no bundle context");
        }
    }

    /**
     * Gets the component factory.
     * @return the factory attached to this component type.
     * @see org.apache.felix.ipojo.api.ComponentType#getFactory()
     */
    public Factory getFactory() {
        initializeFactory();
        return m_factory;
    }

    /**
     * Starts the component type.
     * @see org.apache.felix.ipojo.api.ComponentType#start()
     */
    public void start() {
        initializeFactory();
        m_factory.start();
    }

    /**
     * Stops the component type.
     * @see org.apache.felix.ipojo.api.ComponentType#stop()
     */
    public void stop() {
        initializeFactory();
        m_factory.stop();
    }
    
    /**
     * Initializes the factory.
     */
    private void initializeFactory() {
        if (m_factory == null) {
            createFactory();
        }
    }
    
    /**
     * Sets the bundle context.
     * @param bc the bundle context
     * @return the current component type
     */
    public CompositeComponentType setBundleContext(BundleContext bc) {
        ensureNotInitialized();
        m_context = bc;
        return this;
    }
    
    /**
     * Sets the factory public aspect.
     * @param visible <code>false</code> to create a private factory. 
     * @return the current component type
     */
    public CompositeComponentType setPublic(boolean visible) {
        ensureNotInitialized();
        m_public = visible;
        return this;
    }
    
    /**
     * Sets the component type name.
     * @param name the factory name
     * @return the current component type
     */
    public CompositeComponentType setComponentTypeName(String name) {
        ensureNotInitialized();
        m_name = name;
        return this;
    }
    
    public CompositeComponentType addInstance(Instance inst) {
        m_contained.add(inst);
        return this;
    }
    
    public CompositeComponentType addSubService(ImportedService is) {
        m_imported.add(is);
        return this;
    }
    
    public CompositeComponentType addSubService(InstantiatedService is) {
        m_instantiated.add(is);
        return this;
    }
    
    public CompositeComponentType addService(ExportedService es) {
        m_exported.add(es);
        return this;
    }
    
    public CompositeComponentType addService(ProvidedService es) {
        m_provided.add(es);
        return this;
    }
    
    /**
     * Generates the component description.
     * @return the component type description of 
     * the current component type
     */
    private Element generateComponentMetadata() {
        Element element = new Element("composite", "");
        if (m_name != null) {
            element.addAttribute(new Attribute("name", m_name));
        }
        if (! m_public) {
            element.addAttribute(new Attribute("public", "false"));
        }
        for (int i = 0; i < m_contained.size(); i++) {
            Instance inst = (Instance) m_contained.get(i);
            element.addElement(inst.getElement());
        }
        for (int i = 0; i < m_imported.size(); i++) {
            ImportedService inst = (ImportedService) m_imported.get(i);
            element.addElement(inst.getElement());
        }
        for (int i = 0; i < m_instantiated.size(); i++) {
            InstantiatedService inst = (InstantiatedService) m_instantiated.get(i);
            element.addElement(inst.getElement());
        }
        return element;
    }
    
    /**
     * Creates the component factory.
     */
    private void createFactory() {
        ensureValidity();
        m_metadata = generateComponentMetadata();
        try {
           m_factory = new CompositeFactory(m_context, m_metadata);
            m_factory.start();
        } catch (ConfigurationException e) {
            throw new IllegalStateException("An exception occurs during factory initialization : " + e.getMessage());
        }
       
    }
    

    

}
