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
package org.apache.felix.ipojo.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.manipulation.Manipulator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Allows defining primitive component types.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PrimitiveComponentType extends ComponentType {

    /**
     * The bundle context.
     */
    private BundleContext m_context;

    /**
     * The implementation class name.
     */
    private String m_classname;

    /**
     * The component type name.
     */
    private String m_name;

    /**
     * The component type version.
     */
    private String m_version;

    /**
     * Is the component type immediate.
     */
    private boolean m_immediate;

    /**
     * Manipulation metadata of the component type.
     */
    private Element m_manipulation;

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
    private List m_services = new ArrayList(1);

    /**
     * List of service dependencies.
     */
    private List m_dependencies = new ArrayList();

    /**
     * List of configuration properties.
     */
    private List m_properties = new ArrayList();

    /**
     * The validate callback.
     */
    private String m_validate;

    /**
     * The invalidate callback.
     */
    private String m_invalidate;

    /**
     * The udpated callback.
     */
    private String m_updated;

    /**
     * Are the properties propagated to provided services?
     */
    private boolean m_propagation;

    /**
     * The factory method.
     */
    private String m_factoryMethod;

    /**
     * Is the factory public?
     */
    private boolean m_public = true;

    /**
     * The Managed Service PID.
     */
    private String m_msPID;

    /**
     * The temporal dependencies.
     */
    private ArrayList m_temporals = new ArrayList();

    /**
     * List of Handler representing external
     * handler configuration.
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
        if (m_classname == null) {
            throw new IllegalStateException("The primitive component type has no implementation class");
        }
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
    public PrimitiveComponentType setBundleContext(BundleContext bc) {
        ensureNotInitialized();
        m_context = bc;
        return this;
    }

    /**
     * Sets the implementation class.
     * @param classname the class name
     * @return the current component type
     */
    public PrimitiveComponentType setClassName(String classname) {
        ensureNotInitialized();
        m_classname = classname;
        return this;
    }

    /**
     * Sets the component type name.
     * @param name the factory name
     * @return the current component type
     */
    public PrimitiveComponentType setComponentTypeName(String name) {
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
    public PrimitiveComponentType setComponentTypeVersion(String version) {
        ensureNotInitialized();
        m_version = version;
        return this;
    }

    /**
     * Sets if the component type is immediate or not.
     * @param immediate <code>true</code> to set the component
     * type to immediate
     * @return the current component type
     */
    public PrimitiveComponentType setImmediate(boolean immediate) {
        ensureNotInitialized();
        m_immediate = immediate;
        return this;
    }

    /**
     * Sets the dependency factory method.
     * @param method the method used to create pojo object.
     * @return the current component type
     */
    public PrimitiveComponentType setFactoryMethod(String method) {
        ensureNotInitialized();
        m_factoryMethod = method;
        return this;
    }

    /**
     * Sets if the component type propagates properties to service properties.
     * @param propagation <code>true</code> to enable propagation
     * @return the current component type
     */
    public PrimitiveComponentType setPropagation(boolean propagation) {
        ensureNotInitialized();
        m_propagation = propagation;
        return this;
    }

    /**
     * Sets the factory public aspect.
     * @param visible <code>false</code> to create a private factory.
     * @return the current component type
     */
    public PrimitiveComponentType setPublic(boolean visible) {
        ensureNotInitialized();
        m_public = visible;
        return this;
    }

    /**
     * Sets the managed service pid.
     * @param pid the managed service pid
     * @return the current component type
     */
    public PrimitiveComponentType setManagedServicePID(String pid) {
        ensureNotInitialized();
        m_msPID = pid;
        return this;
    }

    /**
     * Sets the validate method.
     * @param method the validate method
     * @return the current component type
     */
    public PrimitiveComponentType setValidateMethod(String method) {
        ensureNotInitialized();
        m_validate = method;
        return this;
    }

    /**
     * Sets the invalidate method.
     * @param method the invalidate method
     * @return the current component type
     */
    public PrimitiveComponentType setInvalidateMethod(String method) {
        ensureNotInitialized();
        m_invalidate = method;
        return this;
    }

    /**
     * Sets the updated method.
     * @param method the updated method
     * @return the current component type
     */
    public PrimitiveComponentType setUpdatedMethod(String method) {
        ensureNotInitialized();
        m_updated = method;
        return this;
    }

    /**
     * Generates the component description.
     * @return the component type description of
     * the current component type
     */
    private Element generateComponentMetadata() {
        Element element = new Element("component", "");
        element.addAttribute(new Attribute("classname", m_classname));
        if (m_name != null) {
            element.addAttribute(new Attribute("name", m_name));
        }
        if (m_version != null) {
            element.addAttribute(new Attribute("version", m_version));
        }
        if (m_factoryMethod != null) {
            element.addAttribute(new Attribute("factory-method", m_factoryMethod));
        }
        if (! m_public) {
            element.addAttribute(new Attribute("public", "false"));
        }
        if (m_immediate) {
            element.addAttribute(new Attribute("immediate", "true"));
        }
        for (int i = 0; i < m_services.size(); i++) {
            Service svc = (Service) m_services.get(i);
            element.addElement(svc.getElement());
        }
        for (int i = 0; i < m_dependencies.size(); i++) {
            Dependency dep = (Dependency) m_dependencies.get(i);
            element.addElement(dep.getElement());
        }
        for (int i = 0; i < m_temporals.size(); i++) {
            TemporalDependency dep = (TemporalDependency) m_temporals.get(i);
            element.addElement(dep.getElement());
        }
        if (m_validate != null) {
            Element callback = new Element("callback", "");
            callback.addAttribute(new Attribute("transition", "validate"));
            callback.addAttribute(new Attribute("method", m_validate));
            element.addElement(callback);
        }
        if (m_invalidate != null) {
            Element callback = new Element("callback", "");
            callback.addAttribute(new Attribute("transition", "invalidate"));
            callback.addAttribute(new Attribute("method", m_invalidate));
            element.addElement(callback);
        }

        // Properties
        // First determine if we need the properties element
        if (m_propagation || m_msPID != null || ! m_properties.isEmpty()) {
            Element properties = new Element("properties", "");
            if (m_propagation) {
                properties.addAttribute(new Attribute("propagation", "true"));
            }
            if (m_msPID != null) {
                properties.addAttribute(new Attribute("pid", m_msPID));
            }
            if (m_updated != null) {
                properties.addAttribute(new Attribute("updated", m_updated));
            }
            for (int i = 0; i < m_properties.size(); i++) {
                Property prop = (Property) m_properties.get(i);
                properties.addElement(prop.getElement());
            }
            element.addElement(properties);
        }

        // External handlers
        for (int i = 0; i < m_handlers.size(); i++) {
            HandlerConfiguration hc = (HandlerConfiguration) m_handlers.get(i);
            element.addElement(hc.getElement());
        }

        return element;
    }


    /**
     * Adds an HandlerConfiguration to the component type. Each component type
     * implementation must uses the populated list (m_handlers) when generating
     * the component metadata.
     * @param handler the handler configuration to add
     * @return the current component type
     */
    public PrimitiveComponentType addHandler(HandlerConfiguration handler) {
        m_handlers.add(handler);
        return this;
    }

    /**
     * Creates the component factory.
     */
    private void createFactory() {
        ensureValidity();
        byte[] clazz = manipulate();
        m_metadata = generateComponentMetadata();
        Element meta = m_metadata;
        meta.addElement(m_manipulation);
        try {
            if (clazz.length == 0) { // Already manipulated
                m_factory = new ComponentFactory(m_context, meta);
            } else {
                m_factory = new ComponentFactory(m_context, clazz, meta);
            }
            m_factory.start();
        } catch (ConfigurationException e) {
            throw new IllegalStateException("An exception occurs during factory initialization : " + e.getMessage());
        }

    }

    /**
     * Manipulates the implementation class.
     * @return the manipulated class
     */
    private byte[] manipulate() {
        Manipulator manipulator = new Manipulator();
        try {
            byte[] array = getClassByteArray();
            byte[] newclazz = manipulator.manipulate(array);
            m_manipulation = manipulator.getManipulationMetadata();
            return newclazz;
        } catch (IOException e) {
            throw new IllegalStateException("An exception occurs during implementation class manipulation : " + e.getMessage());
        }
    }

    /**
     * Gets a class file as a byte array.
     * @return the byte array.
     * @throws IOException the class file cannot be read.
     */
    private byte[] getClassByteArray() throws IOException {
        String filename = m_classname.replace('.', '/') + ".class";
        URL url = m_context.getBundle().getResource(filename);
        if (url == null) {
            throw new IllegalStateException("An exception occurs during implementation class manipulation : cannot found the class file " + filename);
        }
        InputStream is = url.openStream();
        if (is == null) {
            throw new IllegalStateException("An exception occurs during implementation class manipulation : cannot read the class file " + url);
        }
        byte[] b = new byte[is.available()];
        is.read(b);
        return b;
    }

    /**
     * Adds a provided service.
     * @param svc the service to add
     * @return the current component type
     */
    public PrimitiveComponentType addService(Service svc) {
        ensureNotInitialized();
        m_services.add(svc);
        return this;
    }

    /**
     * Adds a service dependency.
     * @param dep the dependency to add
     * @return the current component type
     */
    public PrimitiveComponentType addDependency(Dependency dep) {
        ensureNotInitialized();
        m_dependencies.add(dep);
        return this;
    }

    /**
     * Adds a temporal service dependency.
     * @param dep the temporal dependency to add
     * @return the current component type
     */
    public PrimitiveComponentType addDependency(TemporalDependency dep) {
        ensureNotInitialized();
        m_temporals.add(dep);
        return this;
    }

    /**
     * Adds a configuration property.
     * @param prop the property to add
     * @return the current component type
     */
    public PrimitiveComponentType addProperty(Property prop) {
        ensureNotInitialized();
        m_properties.add(prop);
        return this;
    }
    
    /**
     * Adds a configuration property.
     * @param key the key
     * @param obj the value (can be <code>null</code>)
     * @return the current component type
     */
    public PrimitiveComponentType addProperty(String key, Object obj) {
        String value = null;
        if (obj != null) {
            value = obj.toString();
        }

        addProperty(new Property().setName(key).setValue(value));
        return this;
    }

}
