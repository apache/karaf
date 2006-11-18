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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * The component manager factory class manages component manager object.
 * This class could export Factory and ManagedServiceFactory services.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentManagerFactory implements Factory, ManagedServiceFactory {

    // Fields :
    /**
     * List of the managed component manager.
     * The key of tis hashmap is the name (pid) of the component created
     */
    private HashMap m_componentManagers = new HashMap();

    /**
     * The bundle context reference.
     */
    private BundleContext m_bundleContext = null;

    /**
     * Component class.
     */
    private byte[] m_clazz = null;

    /**
     * Component Class Name.
     */
    private String m_componentClassName = null;

    /**
     * Classloader to delegate loading.
     */
    private FactoryClassloader m_classLoader = null;

    /**
     * Component Type provided by this factory.
     */
    private Element m_componentMetadata;
    
    /**
     * Factory Name / PID.
     * Could be the component class name if the factory name is not set.
     */
    private String m_factoryName;

    /**
     * Service Registration of this factory (Facotry & ManagedServiceFactory).
     */
    private ServiceRegistration m_sr;

    /**
     * Component-Type info exposed by the factory service.
     */
    private ComponentInfo m_componentInfo;
    

    //End field

    /**
     * FactoryClassloader.
     */
    private class FactoryClassloader extends ClassLoader  {

        /**
         * load the class.
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         * @param name : the name of the class
         * @param resolve : should be the class resolve now ?
         * @return : the loaded class
         * @throws ClassNotFoundException : the class to load is not found
         */
        protected synchronized Class loadClass(final String name,
                final boolean resolve) throws ClassNotFoundException {
            return m_bundleContext.getBundle().loadClass(name);
        }


        /**
         * Return the URL of the asked ressource.
         * @param arg : the name of the resource to find.
         * @return the URL of the resource.
         * @see java.lang.ClassLoader#getResource(java.lang.String)
         */
        public URL getResource(String arg) {
            return m_bundleContext.getBundle().getResource(arg);
        }

        /**
         * .
         * @param arg : resource to find
         * @return : the enumeration found
         * @throws IOException : if the lookup failed.
         * @see java.lang.ClassLoader#getResources(java.lang.String)
         */
        public Enumeration getRessources(String arg) throws IOException {
            return m_bundleContext.getBundle().getResources(arg);
        }

        /**
         * The defineClass method.
         * @param name : name of the class
         * @param b : the byte array of the class
         * @param domain : the protection domain
         * @return : the defined class.
         * @throws Exception : if a problem is detected during the loading
         */
        public Class defineClass(String name, byte[] b,
                ProtectionDomain domain) throws Exception {
            return super.defineClass(name, b, 0, b.length, domain);
        }
    }


    /**
     * @return the Bundle Context
     */
    public BundleContext getBundleContext() { return m_bundleContext; }

    /**
     * @return the class name of the component-type provided by this factory.
     */
    protected String getComponentClassName() { return m_componentClassName; }

    /**
     * Create a component manager factory.
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public ComponentManagerFactory(BundleContext bc, Element cm) {
        m_bundleContext = bc;
        m_componentClassName = cm.getAttribute("className");
        m_componentMetadata = cm;
        
        // Get factory PID :
        if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) { m_factoryName = m_componentMetadata.getAttribute("factory"); }
        else { m_factoryName = m_componentMetadata.getAttribute("className"); }

    }

    /**
     * Create a component manager factory. The class is given in parameter.
     * @param bc : bundle context
     * @param clazz : the component class
     * @param cm : metadata of the component
     */
    public ComponentManagerFactory(BundleContext bc, byte[] clazz, Element cm) {
    	m_bundleContext = bc;
        m_clazz = clazz;
        m_componentClassName = cm.getAttribute("className");
        m_componentMetadata = cm;
        
        // Get factory PID :
        if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) { m_factoryName = m_componentMetadata.getAttribute("factory"); }
        else { m_factoryName = m_componentMetadata.getAttribute("className"); }
    }

    /**
     * Create a component manager factory, no component manager are created.
     * @param bc
     */
    public ComponentManagerFactory(BundleContext bc) { m_bundleContext = bc; }


    // Factory lifecycle management

    /**
     * Stop all the component managers.
     */
    public void stop() {
        Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Stop the component factory");
        Collection col = m_componentManagers.values();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            ComponentManagerImpl cm = (ComponentManagerImpl) it.next();
            cm.stop();
        }
        m_componentManagers.clear();
        if (m_sr != null) { m_sr.unregister(); }
        m_sr = null;
    }

    /**
     * Start all the component managers.
     */
    public void start() {
        Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Start the component factory");
        
        // Check if the factory should be exposed
        if (m_componentMetadata.containsAttribute("factory") && m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) { return; }
        Properties props = new Properties();
        props.put("component.class", m_componentClassName);

        // create a ghost component
        ComponentManagerImpl ghost = new ComponentManagerImpl(this, m_bundleContext);
        ghost.configure(m_componentMetadata, new Properties());
        m_componentInfo = ghost.getComponentInfo();

        props.put("component.providedServiceSpecifications", m_componentInfo.getprovidedServiceSpecification());
        props.put("component.properties", m_componentInfo.getProperties());
        props.put("component.information", m_componentInfo.toString());

        // Add Facotry PID to the component properties
        props.put(Constants.SERVICE_PID, m_factoryName);

        // Exposition of the factory service
        m_sr = m_bundleContext.registerService(new String[] {Factory.class.getName(), ManagedServiceFactory.class.getName()}, this, props);
    }

    /**
     * @see org.apache.felix.ipojo.Factory#getComponentInfo()
     */
    public ComponentInfo getComponentInfo() { return m_componentInfo; }

    /**
     * Load a class.
     * @param className : name of the class to load
     * @return the resulting Class object
     * @throws ClassNotFoundException : happen when the class is not found
     */
    public Class loadClass(String className) throws ClassNotFoundException {
        Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] In load for : " + className);
        if (m_clazz != null && className.equals(m_componentClassName)) {
            if (m_classLoader == null) {
                Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Create the FactoryClassLoader for : " + className);
                m_classLoader = new FactoryClassloader();
            }
            try {
                Class c = m_classLoader.defineClass(m_componentClassName, m_clazz, null);
                Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Return " + c + " for " + className);
                return c;
            } catch (Exception e) {
                Activator.getLogger().log(Level.SEVERE, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Cannot define the class : " + className);
                return null;
            }
        }
        return m_bundleContext.getBundle().loadClass(className);
    }

    /**
     * Return the URL of a resource.
     * @param resName : resource name
     * @return the URL of the resource
     */
    public URL getResource(String resName) {
        return m_bundleContext.getBundle().getResource(resName);
    }

    /**
     * @see org.apache.felix.ipojo.Factory#createComponent(java.util.Dictionary)
     */
    public ComponentManager createComponent(Dictionary configuration) {
        Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Create a component and start it");
        IPojoContext context = new IPojoContext(m_bundleContext);
        ComponentManagerImpl component = new ComponentManagerImpl(this, context);
        context.setComponentInstance(component);
        component.configure(m_componentMetadata, configuration);

        String pid = null;
        if (configuration.get("name") != null) { pid = (String) configuration.get("name"); }
        else { pid = m_componentMetadata.getAttribute("className"); }

        m_componentManagers.put(pid, component);
        component.start();
        return component;
    }
    
    /**
     * @see org.apache.felix.ipojo.Factory#createComponent(java.util.Dictionary)
     */
    public ComponentManager createComponent(Dictionary configuration, ServiceContext serviceContext) {
        Activator.getLogger().log(Level.INFO, "[Bundle " + m_bundleContext.getBundle().getBundleId() + "] Create a component and start it");
        IPojoContext context = new IPojoContext(m_bundleContext, serviceContext);
        ComponentManagerImpl component = new ComponentManagerImpl(this, context);
        context.setComponentInstance(component);
        component.configure(m_componentMetadata, configuration);

        String pid = null;
        if (configuration.get("name") != null) { pid = (String) configuration.get("name"); }
        else { pid = m_componentMetadata.getAttribute("className"); }

        m_componentManagers.put(pid, component);
        component.start();
        return component;
    }
    
    

    /**
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    public void deleted(String pid) {
        ComponentManagerImpl cm = (ComponentManagerImpl) m_componentManagers.remove(pid);
        if (cm == null) { return;  } // do nothing, the component does not exist !
        else { cm.stop(); }
    }

    /**
     * @see org.osgi.service.cm.ManagedServiceFactory#getName()
     */
    public String getName() { return getFactoryName(); }

    /**
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String, java.util.Dictionary)
     */
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        ComponentManagerImpl cm = (ComponentManagerImpl) m_componentManagers.get(pid);
        if (cm == null) { createComponent(properties); } // Create the component
        else {
            cm.stop(); // Stop the component
            cm.configure(m_componentMetadata, properties); // re-configure the component
            cm.start(); // restart it
        }
    }
    
    /**
     * @return the factory name
     */
    public String getFactoryName() { return m_factoryName; }
    
    /**
     * Check if the given configuration is acceptable as a component instance configuration.
     * This checks that a name is given in the configuration and if all the configurable properties have a value.
     * @param conf : the configuration to check
     * @return true when the configuration is acceptable
     */
    public boolean isAcceptable(Dictionary conf) {
    	// First check that the configuration contains a name : 
    	if(conf.get("name") == null) { return false; }
    	List props = m_componentInfo.getProperties();
    	for(int i = 0; i < props.size(); i++) {
    		PropertyInfo pi = (PropertyInfo) props.get(i);
    		// Failed if the props has no default value and the configuration does not push a value 
    		if(pi.getValue() == null && conf.get(pi.getName()) == null) { return false; }
    	}
    	return true;
    }

}
