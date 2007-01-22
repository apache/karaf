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
 * The component factory manages component instance objects.
 * This management consist in creating and managing component instance build with the component factory.
 * This class could export Factory and ManagedServiceFactory services.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentFactory implements Factory, ManagedServiceFactory {

    /**
     * List of the managed instance managers.
     * The key of this hashmap is the name (i.e. pid) of the created instance
     */
    private HashMap m_componentInstances = new HashMap();

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
     * Classloader to delegate loading.
     */
    private FactoryClassloader m_classLoader = null;  //TODO is this classloader really useful ?

    /**
     * Component Type provided by this factory.  //TODO Should be keep this reference ?
     */
    private Element m_componentMetadata;
    
    /**
     * Factory Name (i.e. Factory PID).
     * Could be the component class name if the factory name is not set.
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
     * Logger for the factory (and all component instance)
     */
    private Logger m_logger;

    /**
     * FactoryClassloader.
     */
    private class FactoryClassloader extends ClassLoader  {
    	
    	HashMap m_definedClasses = new HashMap();

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
            return m_context.getBundle().loadClass(name);
        }


        /**
         * Return the URL of the asked ressource.
         * @param arg : the name of the resource to find.
         * @return the URL of the resource.
         * @see java.lang.ClassLoader#getResource(java.lang.String)
         */
        public URL getResource(String arg) {
            return m_context.getBundle().getResource(arg);
        }

        /**
         * .
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
         * @param name : name of the class
         * @param b : the byte array of the class
         * @param domain : the protection domain
         * @return : the defined class.
         * @throws Exception : if a problem is detected during the loading
         */
        public Class defineClass(String name, byte[] b,
                ProtectionDomain domain) throws Exception {
        	if(m_definedClasses.containsKey(name)) { return (Class) m_definedClasses.get(name); } 
            Class c = super.defineClass(name, b, 0, b.length, domain);
            m_definedClasses.put(name, c);
            return c;
        }
    }


    /**
     * @return the Bundle Context
     */
    protected BundleContext getBundleContext() { return m_context; }

    /**
     * @return the name of the component-type implementation class.
     */
    protected String getComponentClassName() { return m_componentClassName; }
    
    /**
     * @return the factory logger.
     */
    public Logger getLogger() { return m_logger; }

    /**
     * Create a instance manager factory.
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public ComponentFactory(BundleContext bc, Element cm) {
        m_context = bc;
        m_componentClassName = cm.getAttribute("className");
        m_componentMetadata = cm;
        
        // Get factory PID :
        if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) { m_factoryName = m_componentMetadata.getAttribute("factory"); }
        else { m_factoryName = m_componentMetadata.getAttribute("className"); }
        
        m_logger = new Logger(m_context, m_factoryName, Logger.WARNING);
    }

    /**
     * Create a instance manager factory. The class is given in parameter.
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
        if (m_componentMetadata.containsAttribute("factory") && !m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) { m_factoryName = m_componentMetadata.getAttribute("factory"); }
        else { m_factoryName = m_componentMetadata.getAttribute("className"); }
        
        m_logger = new Logger(m_context, m_factoryName, Logger.WARNING);
    }

    /**
     * Stop all the instance managers.
     */
    public void stop() {
        Collection col = m_componentInstances.values();
        Iterator it = col.iterator();
        while (it.hasNext()) {
            ComponentInstance ci = (ComponentInstance) it.next();
            if(ci.isStarted()) { ci.stop(); }
        }
        m_componentInstances.clear();
        if (m_sr != null) { m_sr.unregister(); }
        m_sr = null;
    }

    /**
     * Start all the instance managers.
     */
    public void start() {        
        Properties props = new Properties();

        // create a ghost component
        InstanceManager ghost = new InstanceManager(this, m_context);
        Properties p = new Properties();
        p.put("name", "ghost");
        ghost.configure(m_componentMetadata, p);
        m_componentDesc = ghost.getComponentDescription();
        
        // Check if the factory should be exposed
        if (m_componentMetadata.containsAttribute("factory") && m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) { return; }
        
        props.put("component.class", m_componentClassName);
        props.put("component.providedServiceSpecifications", m_componentDesc.getprovidedServiceSpecification());
        props.put("component.properties", m_componentDesc.getProperties());
        props.put("component.description", m_componentDesc);
        props.put("component.desc", m_componentDesc.toString());
        
        // Add Facotry PID to the component properties
        props.put(Constants.SERVICE_PID, m_factoryName);

        // Exposition of the factory service
        m_sr = m_context.registerService(new String[] {Factory.class.getName(), ManagedServiceFactory.class.getName()}, this, props);
    }

    /**
     * @see org.apache.felix.ipojo.Factory#getComponentInfo()
     */
    public ComponentDescription getComponentDescription() { return m_componentDesc; }

    /**
     * Load a class.
     * @param className : name of the class to load
     * @return the resulting Class object
     * @throws ClassNotFoundException : happen when the class is not found
     */
    public Class loadClass(String className) throws ClassNotFoundException {
        getLogger().log(Logger.INFO, "[Bundle " + m_context.getBundle().getBundleId() + "] In load for : " + className);
        if (m_clazz != null && className.equals(m_componentClassName)) {  // Used the factory classloader to load the component implementation class
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
    
    public Class defineClass(String name, byte[] b, ProtectionDomain domain) throws Exception {
    	if(m_classLoader == null) { m_classLoader = new FactoryClassloader(); }
    	return m_classLoader.defineClass(name, b, domain);
    }

    /**
     * Return the URL of a resource.
     * @param resName : resource name
     * @return the URL of the resource
     */
    public URL getResource(String resName) {
        return m_context.getBundle().getResource(resName);
    }

    /**
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration) {
    	if(!isAcceptable(configuration)) {
    		m_logger.log(Logger.ERROR, "The configuration is not acceptable");
    		return null; 
    	}
    	
        IPojoContext context = new IPojoContext(m_context);
        InstanceManager instance = new InstanceManager(this, context);
        context.setComponentInstance(instance);
        instance.configure(m_componentMetadata, configuration);

        String pid = null;
        if (configuration.get("name") != null) { pid = (String) configuration.get("name"); }
        else { pid = m_componentMetadata.getAttribute("className"); }

        m_componentInstances.put(pid, instance);
        instance.start();
        return instance;
    }
    
    /**
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) {
    	if(!isAcceptable(configuration)) {
    		//TODO : extend the message with infomation about the non acceptability
    		m_logger.log(Logger.ERROR, "The configuration is not acceptable");
    		return null; 
    	}
    	
    	IPojoContext context = new IPojoContext(m_context, serviceContext);
        InstanceManager instance = new InstanceManager(this, context);
        context.setComponentInstance(instance);
        instance.configure(m_componentMetadata, configuration);

        String pid = null;
        if (configuration.get("name") != null) { pid = (String) configuration.get("name"); }
        else { pid = m_componentMetadata.getAttribute("className"); }

        m_componentInstances.put(pid, instance);
        instance.start();
        return instance;
    }
    
    

    /**
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    public void deleted(String pid) {
        InstanceManager cm = (InstanceManager) m_componentInstances.remove(pid);
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
        InstanceManager cm = (InstanceManager) m_componentInstances.get(pid);
        if (cm == null) { createComponentInstance(properties); } // Create the component
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
     * @return true when the configuration seems to be acceptable
     */
    public boolean isAcceptable(Dictionary conf) {
    	// First check that the configuration contains a name : 
    	if(conf.get("name") == null) { return false; }
    	PropertyDescription[] props = m_componentDesc.getProperties();
    	for(int i = 0; i < props.length; i++) {
    		PropertyDescription pd = props[i];
    		// Failed if the props has no default value and the configuration does not push a value 
    		if(pd.getValue() == null && conf.get(pd.getName()) == null) {
    			return false;
    		}
    	}
    	return true;
    }

}
