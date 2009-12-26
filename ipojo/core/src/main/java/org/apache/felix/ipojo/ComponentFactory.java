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

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The component factory manages component instance objects. This management
 * consists to create and manage component instances build with the current
 * component factory. This class could export Factory and ManagedServiceFactory
 * services.
 * @see IPojoFactory
 * @see TrackerCustomizer
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentFactory extends IPojoFactory implements TrackerCustomizer {

    /**
     * The tracker used to track required handler factories.
     * Immutable once set.
     */
    protected Tracker m_tracker;

    /**
     * The class loader to delegate classloading.
     * Immutable once set.
     */
    private FactoryClassloader m_classLoader;

    /**
     * The component implementation class.
     * (manipulated byte array)
     */
    private byte[] m_clazz;

    /**
     * The component implementation qualified class name.
     * Immutable once set.
     * This attribute is set during the creation of the factory.
     */
    private String m_classname;

    /**
     * The manipulation metadata of the implementation class.
     * Immutable once set.
     * This attribute is set during the creation of the factory.
     */
    private PojoMetadata m_manipulation;

    /**
     * Creates a instance manager factory.
     * The class is given in parameter. The component type is not a composite.
     * @param context the bundle context
     * @param clazz the component class
     * @param element the metadata of the component
     * @throws ConfigurationException if the element describing the factory is malformed.
     */
    public ComponentFactory(BundleContext context, byte[] clazz, Element element) throws ConfigurationException {
        this(context, element);
        m_clazz = clazz;
    }

    /**
     * Creates a instance manager factory.
     * @param context the bundle context
     * @param element the metadata of the component to create
     * @throws ConfigurationException if element describing the factory is malformed.
     */
    public ComponentFactory(BundleContext context, Element element) throws ConfigurationException {
        super(context, element);
        check(element); // NOPMD. This invocation is normal.
    }

    /**
     * Gets the component type description of the current factory.
     * @return the description of the component type attached to this factory.
     * @see org.apache.felix.ipojo.IPojoFactory#getComponentTypeDescription()
     */
    public ComponentTypeDescription getComponentTypeDescription() {
        return new PrimitiveTypeDescription(this);
    }

    /**
     * Allows a factory to check if the given element is well-formed.
     * A component factory metadata is correct if they contain the 'classname' attribute.
     * As this method is called from the (single-threaded) constructor, no synchronization is needed.
     * @param element the metadata describing the component
     * @throws ConfigurationException if the element describing the factory is malformed.
     */
    public void check(Element element) throws ConfigurationException {
        m_classname = element.getAttribute("classname");
        if (m_classname == null) { throw new ConfigurationException("A component needs a class name : " + element); }
        m_manipulation = new PojoMetadata(m_componentMetadata);
    }

    /**
     * Gets the class name.
     * No synchronization needed, the classname is immutable.
     * @return the class name.
     * @see org.apache.felix.ipojo.IPojoFactory#getClassName()
     */
    public String getClassName() {
        return m_classname;
    }

    /**
     * Creates a primitive instance.
     * This method is called when holding the lock.
     * @param config the instance configuration
     * @param context the service context (null if the instance has to be created in the global space).
     * @param handlers the handlers to attach to the instance
     * @return the created instance
     * @throws org.apache.felix.ipojo.ConfigurationException if the configuration process failed.
     * @see org.apache.felix.ipojo.IPojoFactory#createInstance(java.util.Dictionary, org.apache.felix.ipojo.IPojoContext, org.apache.felix.ipojo.HandlerManager[])
     */
    public ComponentInstance createInstance(Dictionary config, IPojoContext context, HandlerManager[] handlers) throws org.apache.felix.ipojo.ConfigurationException {
        InstanceManager instance = new InstanceManager(this, context, handlers);

        try {
            instance.configure(m_componentMetadata, config);
            instance.start();
            return instance;
        } catch (ConfigurationException e) {
            // An exception occurs while executing the configure or start
            // methods.
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
            throw e;
        } catch (Throwable e) { // All others exception are handled here.
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
            m_logger.log(Logger.ERROR, e.getMessage(), e);
            throw new ConfigurationException(e.getMessage());
        }

    }

    /**
     * Defines a class.
     * This method needs to be synchronized to avoid that the classloader
     * is created twice.
     * This method delegate the <code>define</code> method invocation to the
     * factory classloader.
     * @param name the qualified name of the class
     * @param clazz the byte array of the class
     * @param domain the protection domain of the class
     * @return the defined class object
     */
    public synchronized Class defineClass(String name, byte[] clazz, ProtectionDomain domain) {
        if (m_classLoader == null) {
            m_classLoader = new FactoryClassloader();
        }
        return m_classLoader.defineClass(name, clazz, domain);
    }

    /**
     * Returns the URL of a resource.
     * This methods delegates the invocation to the
     * {@link Bundle#getResource(String)} method.
     * @param resName the resource name
     * @return the URL of the resource
     */
    public URL getResource(String resName) {
        //No synchronization needed, the context is immutable and
        //the call is managed by the underlying framework.
        return m_context.getBundle().getResource(resName);
    }

    /**
     * Loads a class. This method checks if the class
     * to load is the implementation class or not.
     * If it is, the factory classloader is used, else
     * the {@link Bundle#loadClass(String)} is called.
     * @param className the name of the class to load
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class is not found
     */
    public Class loadClass(String className) throws ClassNotFoundException {
        if (m_clazz != null && m_classname.equals(className)) {  // Immutable fields.
            return defineClass(className, m_clazz, null);
        }
        return m_context.getBundle().loadClass(className);
    }

    /**
     * Starts the factory.
     * This method is called when holding the monitor lock.
     */
    public void starting() {
        if (m_tracker != null) {
            return; // Already started
        } else {
            if (m_requiredHandlers.size() != 0) {
                try {
                    String filter = "(&(" + Handler.HANDLER_TYPE_PROPERTY + "=" + PrimitiveHandler.HANDLER_TYPE + ")" + "(factory.state=1)" + ")";
                    m_tracker = new Tracker(m_context, m_context.createFilter(filter), this);
                    m_tracker.open();
                } catch (InvalidSyntaxException e) {
                    m_logger.log(Logger.ERROR, "A factory filter is not valid: " + e.getMessage()); //Holding the lock should not be an issue here.
                    stop();
                }
            }
        }
    }

    /**
     * Stops all the instance managers.
     * This method is called when holding the lock.
     */
    public void stopping() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
    }

    /**
     * Computes the factory name. The factory name is computed from
     * the 'name' and 'classname' attributes.
     * This method does not manipulate any non-immutable fields,
     * so does not need to be synchronized.
     * @return the factory name.
     */
    public String getFactoryName() {
        String name = m_componentMetadata.getAttribute("name");
        if (name == null) {
            // No factory name, use the classname (mandatory attribute)
            name = m_componentMetadata.getAttribute("classname");
        }
        return name;
    }

    /**
     * Computes required handlers.
     * This method does not manipulate any non-immutable fields,
     * so does not need to be synchronized.
     * @return the required handler list.
     */
    public List getRequiredHandlerList() {
        List list = new ArrayList();
        Element[] elems = m_componentMetadata.getElements();
        for (int i = 0; i < elems.length; i++) {
            Element current = elems[i];
            if (!"manipulation".equals(current.getName())) {
                RequiredHandler req = new RequiredHandler(current.getName(), current.getNameSpace());
                if (!list.contains(req)) {
                    list.add(req);
                }
            }
        }

        // Add architecture if architecture != 'false'
        String arch = m_componentMetadata.getAttribute("architecture");
        if (arch == null || arch.equalsIgnoreCase("true")) {
            list.add(new RequiredHandler("architecture", null));
        }


        // Determine if the component must be immediate.
        // A component becomes immediate if it doesn't provide a service,
        // and does not specified that the component is not immediate.
        if (m_componentMetadata.getElements("provides") == null) {
            String imm = m_componentMetadata.getAttribute("immediate");
            if (imm == null) { // immediate not specified, set the immediate attribute to true
                getLogger().log(
                        Logger.INFO,
                        "The component type " + getFactoryName()
                                + " becomes immediate");
                m_componentMetadata.addAttribute(new Attribute("immediate",
                        "true"));
            }
        }

        // Add lifecycle callback if immediate = true
        RequiredHandler reqCallback = new RequiredHandler("callback", null);
        String imm = m_componentMetadata.getAttribute("immediate");
        if (!list.contains(reqCallback) && imm != null && imm.equalsIgnoreCase("true")) {
            list.add(reqCallback);
        }


        return list;
    }

    /**
     * This method is called when a new handler factory is detected.
     * Test if the factory can be used or not.
     * This method need to be synchronized as it accesses to the content
     * of required handlers.
     * @param reference the new service reference.
     * @return <code>true</code> if the given factory reference matches with a required handler.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public synchronized boolean addingService(ServiceReference reference) {
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            if (req.getReference() == null && match(req, reference)) {
                int oldP = req.getLevel();
                req.setReference(reference);
                // If the priority has changed, sort the list.
                if (oldP != req.getLevel()) {
                    // Manipulate the list.
                    Collections.sort(m_requiredHandlers);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called when a matching service has been added to the tracker,
     * we can no compute the factory state. This method is synchronized to avoid
     * concurrent calls to method modifying the factory state.
     * @param reference the added service reference.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
     */
    public synchronized void addedService(ServiceReference reference) {
        if (m_state == INVALID) {
            computeFactoryState();
        }
    }

    /**
     * This method is called when a used handler factory disappears.
     * This method is synchronized to avoid concurrent calls to method modifying
     * the factory state.
     * @param reference the leaving service reference.
     * @param service  the handler factory object.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public synchronized void removedService(ServiceReference reference, Object service) {
        // Look for the implied reference and invalid the handler identifier
        for (int i = 0; i < m_requiredHandlers.size(); i++) {
            RequiredHandler req = (RequiredHandler) m_requiredHandlers.get(i);
            if (reference.equals(req.getReference())) {
                req.unRef(); // This method will unget the service.
                computeFactoryState();
                return; // The factory can be used only once.
            }
        }
    }

    /**
     * This method is called when a used handler factory is modified.
     * However, handler factory modification is not possible, so this method
     * is never called.
     * @param reference the service reference
     * @param service the Factory object (if already get)
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference reference, Object service) {
        // Noting to do
    }

    /**
     * Returns manipulation metadata of this component type.
     * @return manipulation metadata of this component type.
     */
    public PojoMetadata getPojoMetadata() {
        return m_manipulation;
    }

    /**
     * Gets the version of the component type.
     * @return the version of <code>null</code> if not set.
     * @see org.apache.felix.ipojo.Factory#getVersion()
     */
    public String getVersion() {
        return m_version;
    }
    
    public ClassLoader getBundleClassLoader() {
        return m_classLoader;
    }

    /**
     * this class defines the classloader attached to a factory.
     * This class loader is used to load the implementation (e.g. manipulated)
     * class.
     * @see ClassLoader
     * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
     */
    private class FactoryClassloader extends ClassLoader {

        /**
         * The map of defined classes [Name, Class Object].
         */
        private final Map m_definedClasses = new HashMap();

        /**
         * The defineClass method.
         * @param name name of the class
         * @param clazz the byte array of the class
         * @param domain the protection domain
         * @return the defined class.
         */
        public Class defineClass(String name, byte[] clazz, ProtectionDomain domain) {
            if (m_definedClasses.containsKey(name)) { return (Class) m_definedClasses.get(name); }
            Class clas = super.defineClass(name, clazz, 0, clazz.length, domain);
            m_definedClasses.put(name, clas);
            return clas;
        }

        /**
         * Returns the URL of the required resource.
         * @param arg the name of the resource to find.
         * @return the URL of the resource.
         * @see java.lang.ClassLoader#getResource(java.lang.String)
         */
        public URL getResource(String arg) {
            return m_context.getBundle().getResource(arg);
        }

        /**
         * Loads the given class.
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         * @param name the name of the class
         * @param resolve should be the class resolve now ?
         * @return the loaded class object
         * @throws ClassNotFoundException if the class to load is not found
         * @see java.lang.ClassLoader#loadClass(String, boolean)
         */
        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return m_context.getBundle().loadClass(name);
        }
    }

    /**
     * This class defines the description of primitive (non-composite) component
     * types. An instance of this class will be returned when invoking the
     * {@link ComponentFactory#getComponentDescription()} method.
     * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
     */
    private final class PrimitiveTypeDescription extends ComponentTypeDescription {

        /**
         * Creates a PrimitiveTypeDescription object.
         * @param factory the factory attached to this component type description.
         */
        public PrimitiveTypeDescription(IPojoFactory factory) {
            super(factory);
        }

        /**
         * Computes the properties to publish.
         * The <code>component.class</code> property contains the implementation class name.
         * @return the dictionary of properties to publish
         * @see org.apache.felix.ipojo.architecture.ComponentTypeDescription#getPropertiesToPublish()
         */
        public Dictionary getPropertiesToPublish() {
            Dictionary dict = super.getPropertiesToPublish();
            if (m_classname != null) {
                dict.put("component.class", m_classname);
            }
            return dict;
        }

        /**
         * Adds the "implementation-class" attribute to the type description.
         * @return the component type description.
         * @see org.apache.felix.ipojo.architecture.ComponentTypeDescription#getDescription()
         */
        public Element getDescription() {
            Element elem = super.getDescription();
            elem.addAttribute(new Attribute("Implementation-Class", m_classname));
            return elem;
        }
    }
}
