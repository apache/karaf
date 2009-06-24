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
import org.apache.felix.ipojo.api.HandlerConfiguration;
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
     * List of contained instance.
     */
    private List m_contained = new ArrayList();

    /**
     * Is the factory public?
     */
    private boolean m_public = true;

    /**
     * Component type name.
     */
    private String m_name;

    /**
     * Component type version.
     */
    private String m_version;

    /**
     * List of Handler representing external.
     * handler configuration
     */
    private List m_handlers = new ArrayList();

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
            throw new IllegalStateException("The composite component type has no bundle context");
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

    /**
     * Sets the component type version.
     * @param version the factory version or "bundle" to use the
     * bundle version.
     * @return the current component type
     */
    public CompositeComponentType setComponentTypeVersion(String version) {
        ensureNotInitialized();
        m_version = version;
        return this;
    }

    /**
     * Adds a contained instance.
     * @param inst the instance to add
     * @return the current composite component type
     */
    public CompositeComponentType addInstance(Instance inst) {
        m_contained.add(inst);
        return this;
    }

    /**
     * Adds an imported (sub-)service.
     * @param is the imported service to add
     * @return the current composite component type
     */
    public CompositeComponentType addSubService(ImportedService is) {
        m_imported.add(is);
        return this;
    }

    /**
     * Adds an instantiated sub-service.
     * @param is the instantiated service to add
     * @return the current composite component type
     */
    public CompositeComponentType addSubService(InstantiatedService is) {
        m_instantiated.add(is);
        return this;
    }

    /**
     * Adds an exported service.
     * @param es the exported service to add
     * @return the current composite component type
     */
    public CompositeComponentType addService(ExportedService es) {
        m_exported.add(es);
        return this;
    }

    /**
     * Adds a provided service.
     * @param es the provided service to add
     * @return the current composite component type
     */
    public CompositeComponentType addService(ProvidedService es) {
        m_provided.add(es);
        return this;
    }

    /**
     * Adds an HandlerConfiguration to the component type. Each component type
     * implementation must uses the populated list (m_handlers) when generating
     * the component metadata.
     * @param handler the handler configuration to add
     * @return the current component type.
     */
    public CompositeComponentType addHandler(HandlerConfiguration handler) {
        m_handlers.add(handler);
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
        if (m_version != null) {
            element.addAttribute(new Attribute("version", m_version));
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
        for (int i = 0; i < m_exported.size(); i++) {
            ExportedService inst = (ExportedService) m_exported.get(i);
            element.addElement(inst.getElement());
        }
        for (int i = 0; i < m_provided.size(); i++) {
            ProvidedService inst = (ProvidedService) m_provided.get(i);
            element.addElement(inst.getElement());
        }

        // External handlers
        for (int i = 0; i < m_handlers.size(); i++) {
            HandlerConfiguration hc = (HandlerConfiguration) m_handlers.get(i);
            element.addElement(hc.getElement());
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
