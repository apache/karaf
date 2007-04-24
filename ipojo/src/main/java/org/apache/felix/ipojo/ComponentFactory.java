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

import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * The component factory manages component instance objects. This management
 * consist in creating and managing component instance build with the component
 * factory. This class could export Factory and ManagedServiceFactory services.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentFactory implements Factory, ManagedServiceFactory {

    /**
     * List of the managed instance managers. The key of this hashmap is the
     * name (i.e. pid) of the created instance
     */
    private HashMap m_componentInstances = new HashMap();

    /**
     * Ture if the component is a composition.
     */
    private boolean m_isComposite = false;

    /**
     * The bundle context reference.
     */
    private BundleContext m_context = null;

    /**
     * Component Implementation class.
     */
    private byte[] m_clazz = null;

    /**
     * Component Implementation Class Name.
     */
    private String m_componentClassName = null;

    /**
     * Composition Name.
     */
    private String m_compositeName = null;

    /**
     * Classloader to delegate loading.
     */
    private FactoryClassloader m_classLoader = null;

    /**
     * Component Type provided by this factory. //TODO Should be keep this
     * reference ?
     */
    private Element m_componentMetadata;

    /**
     * Factory Name (i.e. Factory PID). Could be the component class name if the
     * factory name is not set.
     */
    private String m_factoryName;

    /**
     * Service Registration of this factory (Factory & ManagedServiceFactory).
     */
    private ServiceRegistration m_sr;

    /**
     * Component-Type description exposed by the factory service.
     */
    private ComponentDescription m_componentDesc;

    /**
     * Logger for the factory (and all component instance).
     */
    private Logger m_logger;

    /**
     * True when the factory is active (non stopping and non starting).
     */
    private boolean m_active = false;

    /**
     * FactoryClassloader.
     */
    private class FactoryClassloader extends ClassLoader {

        /**
         * Map of defined classes [Name, Class Object].
         */
        private HashMap m_definedClasses = new HashMap();

        /**
         * load the class.
         * 
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         * @param name : the name of the class
         * @param resolve : should be the class resolve now ?
         * @return : the loaded class
         * @throws ClassNotFoundException : the class to load is not found
         */
        protected synchronized Class loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            return m_context.getBundle().loadClass(name);
        }

        /**
         * Return the URL of the asked ressource.
         * 
         * @param arg : the name of the resource to find.
         * @return the URL of the resource.
         * @see java.lang.ClassLoader#getResource(java.lang.String)
         */
        public URL getResource(String arg) {
            return m_context.getBundle().getResource(arg);
        }

        /**
         * .
         * 
         * @param arg : resource to find
         * @return : the enumeration found
         * @throws IOException : if the lookup failed.
         * @see java.lang.ClassLoader#getResources(java.lang.String)
         */
        public Enumeration getRessources(String arg) throws IOException {
            return m_context.getBundle().getResources(arg);
        }

        /**
         * The defineClass method.
         * 
         * @param name : name of the class
         * @param b : the byte array of the class
         * @param domain : the protection domain
         * @return : the defined class.
         * @throws Exception : if a problem is detected during the loading
         */
        public Class defineClass(String name, byte[] b, ProtectionDomain domain) throws Exception {
            if (m_definedClasses.containsKey(name)) {
                return (Class) m_definedClasses.get(name);
            }
            Class c = super.defineClass(name, b, 0, b.length, domain);
            m_definedClasses.put(name, c);
            return c;
        }
    }

    /**
     * Return the bundle context.
     * 
     * @return the Bundle Context.
     */
    protected BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Get the implementation class of the component type.
     * 
     * @return the name of the component-type implementation class.
     */
    protected String getComponentClassName() {
        return m_componentClassName;
    }

    /**
     * Get the logger used by instances of he current factory.
     * 
     * @return the factory logger.
     */
    public Logger getLogger() {
        return m_logger;
    }

    /**
     * Create a instance manager factory.
     * 
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public ComponentFactory(BundleContext bc, Element cm) {
        m_context = bc;
        m_componentMetadata = cm;
        if (cm.getName().equalsIgnoreCase("composite")) {
            m_componentClassName = null;
            m_isComposite = true;
            // Get the name
            if (cm.containsAttribute("name")) {
                m_compositeName = cm.getAttribute("name");
            } else {
                System.err.println("A composite needs a name");
                return;
            }
            // Compute factory name
            if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) {
                m_factoryName = m_componentMetadata.getAttribute("factory");
            } else {
                m_factoryName = m_compositeName;
            }
        } else {
            if (cm.containsAttribute("className")) {
                m_componentClassName = cm.getAttribute("className");
            } else {
                System.err.println("A component needs a class name");
                return;
            }
            if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) {
                m_factoryName = m_componentMetadata.getAttribute("factory");
            } else {
                m_factoryName = m_componentMetadata.getAttribute("className");
            }
        }
        if (m_factoryName != null) {
            m_logger = new Logger(m_context, m_factoryName, Logger.WARNING);
        } else {
            if (m_isComposite) {
                m_logger = new Logger(m_context, m_compositeName, Logger.WARNING);
            } else {
                m_logger = new Logger(m_context, m_componentClassName, Logger.WARNING);
            }
        }
    }

    /**
     * Create a instance manager factory. The class is given in parameter. The
     * component type is not a composite.
     * 
     * @param bc : bundle context
     * @param clazz : the component class
     * @param cm : metadata of the component
     */
    public ComponentFactory(BundleContext bc, byte[] clazz, Element cm) {
        m_context = bc;
        m_clazz = clazz;
        m_componentClassName = cm.getAttribute("className");
        m_componentMetadata = cm;

        // Get factory PID :
        if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) {
            m_factoryName = m_componentMetadata.getAttribute("factory");
        } else {
            m_factoryName = m_componentMetadata.getAttribute("className");
        }

        m_logger = new Logger(m_context, m_factoryName, Logger.WARNING);
    }

    /**
     * Stop all the instance managers.
     */
    public synchronized void stop() {
        m_active = false;
        Collection col = m_componentInstances.values();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            ComponentInstance ci = (ComponentInstance) it.next();
            if (ci.isStarted()) {
                ci.dispose();
            }
        }
        m_componentInstances.clear();
        if (m_sr != null) {
            m_sr.unregister();
        }
        m_sr = null;
    }

    /**
     * Start all the instance managers.
     */
    public synchronized void start() {
        if (m_active) {
            return;
        } // Already started

        Properties props = new Properties();

        // create a ghost component
        if (!m_isComposite) {
            InstanceManager ghost = new InstanceManager(this, m_context);
            Properties p = new Properties();
            p.put("name", "ghost");
            ghost.configure(m_componentMetadata, p);
            m_componentDesc = ghost.getComponentDescription();
        } else {
            CompositeManager ghost = new CompositeManager(this, m_context);
            Properties p = new Properties();
            p.put("name", "ghost");
            ghost.configure(m_componentMetadata, p);
            m_componentDesc = ghost.getComponentDescription();
        }

        // Check if the factory should be exposed
        if (m_componentMetadata.containsAttribute("factory") && m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) {
            return;
        }

        if (!m_isComposite) {
            props.put("component.class", m_componentClassName);
        } else {
            props.put("component.class", "no implementation class");
        }
        props.put("factory.name", m_factoryName);
        props.put("component.providedServiceSpecifications", m_componentDesc.getprovidedServiceSpecification());
        props.put("component.properties", m_componentDesc.getProperties());
        props.put("component.description", m_componentDesc);
        props.put("component.desc", m_componentDesc.toString());

        // Add Factory PID to the component properties
        props.put(Constants.SERVICE_PID, m_factoryName);

        // Exposition of the factory service
        m_active = true;
        m_sr = m_context.registerService(new String[] { Factory.class.getName(), ManagedServiceFactory.class.getName() }, this, props);
    }

    /**
     * Callback called by instance when stopped.
     * 
     * @param ci : the instance stopping
     */
    protected synchronized void stopped(ComponentInstance ci) {
        if (m_active) {
            m_componentInstances.remove(ci.getInstanceName());
        }
    }

    /**
     * Get the component type description attached to this factory.
     * @return : the component type description
     * @see org.apache.felix.ipojo.Factory#getComponentDescription()
     */
    public ComponentDescription getComponentDescription() {
        return m_componentDesc;
    }

    /**
     * Load a class.
     * 
     * @param className : name of the class to load
     * @return the resulting Class object
     * @throws ClassNotFoundException : happen when the class is not found
     */
    public Class loadClass(String className) throws ClassNotFoundException {
        if (m_clazz != null && className.equals(m_componentClassName)) { 
            // Used the factory classloader to load the component implementation class
            if (m_classLoader == null) {
                m_classLoader = new FactoryClassloader();
            }
            try {
                return m_classLoader.defineClass(m_componentClassName, m_clazz, null);
            } catch (Exception e) {
                throw new ClassNotFoundException("[Bundle " + m_context.getBundle().getBundleId() + "] Cannot define the class : " + className, e);
            }
        }
        return m_context.getBundle().loadClass(className);
    }

    /**
     * Define a class.
     * 
     * @param name : qualified name of the class
     * @param b : byte array of the class
     * @param domain : protection domain of the class
     * @return the defined class object
     * @throws Exception : an exception occur during the definition
     */
    public Class defineClass(String name, byte[] b, ProtectionDomain domain) throws Exception {
        if (m_classLoader == null) {
            m_classLoader = new FactoryClassloader();
        }
        return m_classLoader.defineClass(name, b, domain);
    }

    /**
     * Return the URL of a resource.
     * 
     * @param resName : resource name
     * @return the URL of the resource
     */
    public URL getResource(String resName) {
        return m_context.getBundle().getResource(resName);
    }

    /**
     * Create an instance.
     * The given configuration needs to contain the 'name' property.
     * @param configuration : configuration of the created instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is not consistent with the component type of this factory.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration {
        try {
            checkAcceptability(configuration);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
            throw new UnacceptableConfiguration("The configuration " + configuration + " is not acceptable for " + m_factoryName + ": " + e.getMessage());
        }

        String pid = null;
        if (configuration.get("name") != null) {
            pid = (String) configuration.get("name");
        } else {
            throw new UnacceptableConfiguration("The name attribute is missing");
        }

        if (m_componentInstances.containsKey(pid)) {
            throw new UnacceptableConfiguration("Name already used : " + pid);
        }

        IPojoContext context = new IPojoContext(m_context);
        ComponentInstance instance = null;
        if (!m_isComposite) {
            InstanceManager inst = new InstanceManager(this, context);
            // context.setComponentInstance(inst);
            inst.configure(m_componentMetadata, configuration);
            instance = inst;
        } else {
            CompositeManager inst = new CompositeManager(this, context);
            // context.setComponentInstance(inst);
            inst.configure(m_componentMetadata, configuration);
            instance = inst;
        }

        m_componentInstances.put(pid, instance);
        instance.start();
        return instance;
    }

    /**
     * Create an instance.
     * The given configuration needs to contain the 'name' property.
     * @param configuration : configuration of the created instance.
     * @param serviceContext : the service context to push for this instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is not consistent with the component type of this factory.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration {
        try {
            checkAcceptability(configuration);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
            throw new UnacceptableConfiguration("The configuration " + configuration + " is not acceptable for " + m_factoryName + ": " + e.getMessage());
        }

        String pid = null;
        if (configuration.get("name") != null) {
            pid = (String) configuration.get("name");
        } else {
            throw new UnacceptableConfiguration("The name attribute is missing");
        }

        if (m_componentInstances.containsKey(pid)) {
            throw new UnacceptableConfiguration("Name already used : " + pid);
        }

        IPojoContext context = new IPojoContext(m_context, serviceContext);
        ComponentInstance instance = null;
        if (!m_isComposite) {
            InstanceManager inst = new InstanceManager(this, context);
            // context.setComponentInstance(inst);
            inst.configure(m_componentMetadata, configuration);
            instance = inst;
        } else {
            CompositeManager inst = new CompositeManager(this, context);
            // context.setComponentInstance(inst);
            inst.configure(m_componentMetadata, configuration);
            instance = inst;
        }

        m_componentInstances.put(pid, instance);
        instance.start();
        return instance;
    }

    /**
     * Delete an instance.
     * @param pid : name of the instance to delete
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    public void deleted(String pid) {
        InstanceManager cm = (InstanceManager) m_componentInstances.remove(pid);
        if (cm == null) {
            return; // do nothing, the component does not exist !
        } else {
            cm.dispose();
        }
    }

    /**
     * Get the name of this factory.
     * @return the name of this factory
     * @see org.apache.felix.ipojo.Factory#getName()
     */
    public String getName() {
        return m_factoryName;
    }

    /**
     * Create of update an instance.
     * @param pid : name of the instance
     * @param properties : configuration of the instance
     * @throws ConfigurationException : if the configuration is not consistent for this component type
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
     */
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        InstanceManager cm = (InstanceManager) m_componentInstances.get(pid);
        if (cm == null) {
            try {
                properties.put("name", pid); // Add the name in the
                // configuration
                createComponentInstance(properties);
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            }
        } else {
            try {
                properties.put("name", pid); // Add the name in the
                // configuration
                checkAcceptability(properties); // Test if the configuration is
                // acceptable
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            }
            cm.reconfigure(properties); // re-configure the component
        }
    }

    /**
     * Check if the given configuration is acceptable as a component instance
     * configuration. This checks that a name is given in the configuration and
     * if all the configurable properties have a value.
     * 
     * @param conf : the configuration to check
     * @return true when the configuration seems to be acceptable
     */
    public boolean isAcceptable(Dictionary conf) {
        // First check that the configuration contains a name :
        if (conf.get("name") == null) {
            return false;
        }
        PropertyDescription[] props = m_componentDesc.getProperties();
        for (int i = 0; i < props.length; i++) {
            PropertyDescription pd = props[i];
            // Failed if the props has no default value and the configuration
            // does not push a value
            if (pd.getValue() == null && conf.get(pd.getName()) == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test is a configuration is acceptable for the factory.
     * 
     * @param conf : the configuration to test.
     * @throws UnacceptableConfiguration : the configuration is not acceptable.
     */
    private void checkAcceptability(Dictionary conf) throws UnacceptableConfiguration {
        if (conf == null || conf.get("name") == null) {
            throw new UnacceptableConfiguration("The configuration does not contains the \"name\" property");
        }
        PropertyDescription[] props = m_componentDesc.getProperties();
        for (int i = 0; i < props.length; i++) {
            PropertyDescription pd = props[i];
            // Failed if the props has no default value and the configuration
            // does not push a value
            if (pd.getValue() == null && conf.get(pd.getName()) == null) {
                throw new UnacceptableConfiguration("The configuration does not contains the \"" + pd.getName() + "\" property");
            }
        }
    }

    /**
     * Reconfigure an existing instance.
     * @param properties : the new configuration to push.
     * @throws UnacceptableConfiguration : occurs if the new configuration is not consistent with the component type.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary properties) throws UnacceptableConfiguration {
        if (properties == null || properties.get("name") == null) {
            throw new UnacceptableConfiguration("The configuration does not contains the \"name\" property");
        }
        String name = (String) properties.get("name");
        ComponentInstance cm = null;
        if (m_isComposite) {
            cm = (CompositeManager) m_componentInstances.get(name);
        } else {
            cm = (InstanceManager) m_componentInstances.get(name);
        }
        if (cm == null) {
            return; // The instance does not exist.
        } else {
            checkAcceptability(properties); // Test if the configuration is
            // acceptable
        }
        cm.reconfigure(properties); // re-configure the component
    }
}
