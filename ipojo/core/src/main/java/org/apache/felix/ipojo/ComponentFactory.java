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
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * The component factory manages component instance objects. This management
 * consist in creating and managing component instance build with the component
 * factory. This class could export Factory and ManagedServiceFactory services.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentFactory implements Factory, ManagedServiceFactory, TrackerCustomizer {

    /**
     * List of the managed instance name. This list is shared by all factories.
     */
    protected static List m_instancesName = new ArrayList();
    
    /**
     * Component-Type description exposed by the factory service.
     */
    protected ComponentDescription m_componentDesc;
    
    /**
     * List of the managed instance managers. The key of this map is the
     * name (i.e. instance names) of the created instance
     */
    protected Map m_componentInstances = new HashMap();
    
    /**
     * Component Type provided by this factory.
     */
    protected Element m_componentMetadata;
    
    /**
     * The bundle context reference.
     */
    protected BundleContext m_context = null;    
    
    /**
     * Factory Name. Could be the component class name if the
     * factory name is not set.
     */
    protected String m_factoryName;

    /**
     * List of required handler.
     */
    protected List m_handlerIdentifiers = new ArrayList();

    /**
     * List of listeners.
     */
    protected List m_listeners = new ArrayList(5);
    
    /**
     * Logger for the factory (and all component instance).
     */
    protected Logger m_logger;

    /**
     * Factory state.
     */
    protected int m_state = Factory.INVALID;

    /**
     * Tracker used to track required handler factories.
     */
    protected Tracker m_tracker;
    
    /**
     * Component Type Name.
     */
    protected String m_typeName = null;

    /**
     * Class loader to delegate loading.
     */
    private FactoryClassloader m_classLoader = null;
    
    /**
     * Component Implementation class.
     */
    private byte[] m_clazz = null;
    
    /**
     * Component Implementation Class Name.
     */
    private String m_componentClassName = null;
    
    /**
     * Index used to generate instance name if not set.
     */
    private long m_index = 0;
    
    /**
     * Service Registration of this factory (Factory & ManagedServiceFactory).
     */
    private ServiceRegistration m_sr;
    
    /**
     * Create a instance manager factory. The class is given in parameter. The
     * component type is not a composite.
     * @param bc : bundle context
     * @param clazz : the component class
     * @param cm : metadata of the component
     */
    public ComponentFactory(BundleContext bc, byte[] clazz, Element cm) {
        this(bc, cm);
        m_clazz = clazz;
    }
    
    /**
     * Create a instance manager factory.
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public ComponentFactory(BundleContext bc, Element cm) {
        m_context = bc;
        m_componentMetadata = cm;
        
        if (! check(cm)) {
            return;
        }
        
        // Get the name
        if (cm.containsAttribute("name")) {
            m_typeName = cm.getAttribute("name");
        }

        if (m_typeName != null) {
            m_logger = new Logger(m_context, m_typeName, Logger.WARNING);
        } else {
            m_logger = new Logger(m_context, m_componentClassName, Logger.WARNING);
        }
        
        computeFactoryName();
        computeRequiredHandlers();
        
    }
    
    /**
     * Add a factory listener.
     * @param l : the factory listener to add
     * @see org.apache.felix.ipojo.Factory#addFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void addFactoryStateListener(FactoryStateListener l) {
        synchronized (m_listeners) {
            m_listeners.add(l);
        }
        // TODO do we need to notify the actual state of the factory to the new listener ?
    }

    /**
     * A new handler factory is detected.
     * Test if the factory can be used or not.
     * @param reference : the new service reference.
     * @return true if the given factory reference match with a required handler.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference reference) {
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            if (hi.m_reference == null && match(hi, reference)) {
                hi.setReference(reference);
                computeFactoryState();
                return true;
            }
        }
        return false;
    }

    /**
     * Check method : allow a factory to check if given element are correct.
     * A component factory metadata are correct if they contain the 'classname' attribute.
     * @param cm : the metadata
     * @return true if the metadata are correct
     */
    public boolean check(Element cm) {
        if (cm.containsAttribute("className")) {
            m_componentClassName = cm.getAttribute("className");
            return true;
        } else {
            System.err.println("A component needs a class name : " + cm);
            return false;
        }
    }

    /**
     * Create an instance. The given configuration needs to contain the 'name'
     * property.
     * @param configuration : configuration of the created instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is
     * not consistent with the component type of this factory.
     * @throws MissingHandlerException  : occurs if an handler is unavailable when the instance is created.
     * @throws org.apache.felix.ipojo.ConfigurationException : occurs when the instance or type configuration are not correct.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public synchronized ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException, org.apache.felix.ipojo.ConfigurationException {
        return createComponentInstance(configuration, null);
    }
    
    /**
     * Create an instance. The given configuration needs to contain the 'name'
     * property.
     * @param configuration : configuration of the created instance.
     * @param serviceContext : the service context to push for this instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is
     * not consistent with the component type of this factory.
     * @throws MissingHandlerException : occurs when an handler is unavailable when creating the instance.
     * @throws org.apache.felix.ipojo.ConfigurationException : when the instance configuration failed.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration, MissingHandlerException, org.apache.felix.ipojo.ConfigurationException {
        if (m_state == INVALID) {
            throw new MissingHandlerException(getMissingHandlers());
        }
        
        if (configuration == null) {
            configuration = new Properties();
        }
        
        try {
            checkAcceptability(configuration);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
            throw new UnacceptableConfiguration("The configuration " + configuration + " is not acceptable for " + m_factoryName + ": " + e.getMessage());
        }

        
        String n = null;
        if (configuration.get("name") != null) {
            n = (String) configuration.get("name");
        } else {
            n = generateName();
            configuration.put("name", n);
        }
        
        if (m_instancesName.contains(n)) {
            throw new UnacceptableConfiguration("Name already used : " + n);
        } else {
            m_instancesName.add(n);
        }

        BundleContext context = null;
        if (serviceContext == null) {
            context = new IPojoContext(m_context);
        } else {
            context = new IPojoContext(m_context, serviceContext);
        }
        List handlers = new ArrayList();
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            handlers.add(getHandlerInstance(hi, serviceContext));
        }
        InstanceManager instance = new InstanceManager(this, context, (HandlerManager[]) handlers.toArray(new HandlerManager[0]));
        instance.configure(m_componentMetadata, configuration);

        m_componentInstances.put(n, instance);
        instance.start();
        return instance;
    }

    /**
     * Define a class.
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
     * Delete an instance.
     * @param in : name of the instance to delete
     * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
     */
    public synchronized void deleted(String in) {
        m_instancesName.remove(in);
        final ComponentInstance cm = (ComponentInstance) m_componentInstances.remove(in);
        if (cm == null) {
            return; // do nothing, the component does not exist !
        } else {
            cm.dispose();
        }
    }

    /**
     * Get the component type description.
     * @return the component type description object. Null if not already computed.
     */
    public ComponentDescription getComponentDescription() {
        return m_componentDesc;
    }

    /**
     * Get the component type description attached to this factory.
     * @return : the component type description
     * @see org.apache.felix.ipojo.Factory#getDescription()
     */
    public Element getDescription() {
        if (m_componentDesc == null) { return new Element("No description available for " + getName(), ""); }
        return m_componentDesc.getDescription();
    }
    
    /**
     * Get the logger used by instances of he current factory.
     * @return the factory logger.
     */
    public Logger getLogger() {
        return m_logger;
    }
    
    /**
     * Get the list of missing handlers.
     * @return the list of missing handlers (namespace:name)
     * @see org.apache.felix.ipojo.Factory#getMissingHandlers()
     */
    public List getMissingHandlers() {
        List l = new ArrayList();
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            if (hi.m_reference == null) {
                l.add(hi.getFullName());
            }
        }
        return l;
    }

    /**
     * Get the name of this factory.
     * 
     * @return the name of this factory
     * @see org.apache.felix.ipojo.Factory#getName()
     */
    public String getName() {
        if (m_factoryName != null) {
            return m_factoryName;
        } else if (m_typeName != null) {
            return m_typeName;
        } else {
            return m_componentClassName;
        }
    }

    /**
     * Get the list of required handlers.
     * @return the list of required handlers (namespace:name)
     * @see org.apache.felix.ipojo.Factory#getRequiredHandlers()
     */
    public List getRequiredHandlers() {
        List l = new ArrayList();
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            l.add(hi.getFullName());
        }
        return l;
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
     * Check if the given configuration is acceptable as a component instance
     * configuration. This method checks that if all the configurable properties
     * have a value.
     * @param conf : the configuration to check
     * @return true when the configuration seems to be acceptable
     */
    public boolean isAcceptable(Dictionary conf) {
        try {
            checkAcceptability(conf);
        } catch (UnacceptableConfiguration e) { 
            return false; 
        } catch (MissingHandlerException e) { 
            return false;  
        }
        return true;
    }

    /**
     * Load a class.
     * @param className : name of the class to load
     * @return the resulting Class object
     * @throws ClassNotFoundException : happen when the class is not found
     */
    public Class loadClass(String className) throws ClassNotFoundException {
        if (m_clazz != null && className.equals(m_componentClassName)) {
            // Used the factory classloader to load the component implementation
            // class
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
     * A used handler factory is modified.
     * @param reference : the service reference
     * @param service : the Factory object (if already get)
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference reference, Object service) {
        // Noting to do
    }

    /**
     * Reconfigure an existing instance.
     * @param properties : the new configuration to push.
     * @throws UnacceptableConfiguration : occurs if the new configuration is
     * not consistent with the component type.
     * @throws MissingHandlerException : occurs if the current factory is not valid.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration, MissingHandlerException {
        if (properties == null || properties.get("name") == null) {
            throw new UnacceptableConfiguration("The configuration does not contains the \"name\" property");
        }
        final String name = (String) properties.get("name");
        InstanceManager cm = (InstanceManager) m_componentInstances.get(name);
        
        if (cm == null) {
            return; // The instance does not exist.
        } else {
            checkAcceptability(properties); // Test if the configuration is acceptable
        }
        cm.reconfigure(properties); // re-configure the component
    }

    /**
     * A used factory disappears.
     * @param reference : service reference.
     * @param service : factory object.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference reference, Object service) {
        // Look for the implied reference and invalid the handler identifier
        boolean touched = false;
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            if (reference.equals(hi.getReference())) {
                hi.setReference(null); // This method will unget the service.
                touched = true;
            }
        }
        if (touched) { computeFactoryState(); }
    }

    /**
     * Remove a factory listener.
     * @param l : the factory listener to remove
     * @see org.apache.felix.ipojo.Factory#removeFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void removeFactoryStateListener(FactoryStateListener l) {
        synchronized (m_listeners) {
            m_listeners.remove(l);
        }
    }

    /**
     * Start the factory.
     */
    public synchronized void start() {
        if (m_componentDesc != null) { // Already started.
            return;
        } 
        
        try {
            String filter = "(&(" + Constants.OBJECTCLASS + "=" + Factory.class.getName() + ")"
                    + "(" + Handler.HANDLER_NAME_PROPERTY + "=*)" + "(" + Handler.HANDLER_NAMESPACE_PROPERTY + "=*)" /* Look only for handlers */
                    + "(" + Handler.HANDLER_TYPE_PROPERTY + "=" + PrimitiveHandler.HANDLER_TYPE + ")" 
                    + "(factory.state=1)"
                    + ")";
            m_tracker = new Tracker(m_context, m_context.createFilter(filter), this);
            m_tracker.open();
            
        } catch (InvalidSyntaxException e) {
            m_logger.log(Logger.ERROR, "A factory filter is not valid: " + e.getMessage());
            return;
        }
        
        computeFactoryState();

        if (m_factoryName == null) {
            return;
        }
        
        // Exposition of the factory service
        m_sr = m_context.registerService(new String[] { Factory.class.getName(), ManagedServiceFactory.class.getName() }, this, getProperties());
    }

    /**
     * Stop all the instance managers.
     */
    public synchronized void stop() {
        if (m_sr != null) {
            m_sr.unregister();
            m_sr = null;
        }
        
        if (m_tracker != null) {        
            m_tracker.close();
        }
        
        final Collection col = m_componentInstances.values();
        final Iterator it = col.iterator();
        while (it.hasNext()) {
            InstanceManager ci = (InstanceManager) it.next();
            if (ci.getState() != ComponentInstance.DISPOSED) {
                ci.kill();
            }
            m_instancesName.remove(ci.getInstanceName());
        }
        
        m_componentInstances.clear();
        
        // Release each handler
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            if (hi.getReference() != null) {
                hi.setReference(null);
            }
        }
        
        m_tracker = null;
        m_componentDesc = null;
        m_classLoader = null;
        m_clazz = null;
        m_state = INVALID;
    }
    
    /**
     * Create of update an instance.
     * @param in : name of the instance
     * @param properties : configuration of the instance
     * @throws ConfigurationException : if the configuration is not consistent
     * for this component type
     * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String,
     * java.util.Dictionary)
     */
    public synchronized void updated(String in, Dictionary properties) throws ConfigurationException {
        final InstanceManager cm = (InstanceManager) m_componentInstances.get(in);
        if (cm == null) {
            try {
                properties.put("name", in); // Add the name in the configuration
                createComponentInstance(properties);
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "Handler not available : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            } catch (org.apache.felix.ipojo.ConfigurationException e) {
                m_logger.log(Logger.ERROR, "The Component Type metadata are not correct : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            }
        } else {
            try {
                properties.put("name", in); // Add the name in the configuration
                reconfigure(properties); // re-configure the component
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "The facotry is not valid, at least one handler is missing : " + e.getMessage());
                throw new ConfigurationException(properties.toString(), e.getMessage());
            }
        }
    }

    /**
     * Test is a configuration is acceptable for the factory.
     * @param conf : the configuration to test.
     * @throws UnacceptableConfiguration : the configuration is not acceptable.
     * @throws MissingHandlerException : the factory is not valid.
     */
    protected void checkAcceptability(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException {
        computeFactoryState();
        
        if (m_state == Factory.INVALID) {
            throw new MissingHandlerException(getMissingHandlers());
        }
        
        final PropertyDescription[] props = m_componentDesc.getProperties();
        for (int i = 0; i < props.length; i++) {
            final PropertyDescription pd = props[i];
            // Failed if the props has no default value and the configuration does not push a value
            if (pd.getValue() == null && conf.get(pd.getName()) == null) {
                throw new UnacceptableConfiguration("The configuration does not contains the \"" + pd.getName() + "\" property");
            }
        }
    }
    
    /**
     * Compute the component type description.
     * The factory must be valid when calling this method.
     * @throws org.apache.felix.ipojo.ConfigurationException if one handler has rejected the configuration.
     */
    protected void computeDescription() throws org.apache.felix.ipojo.ConfigurationException {
        List l = new ArrayList();
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            l.add(hi.getFullName());
        }

        m_componentDesc = new ComponentDescription(getName(), m_componentClassName, m_state, l, getMissingHandlers(), m_context.getBundle().getBundleId());
         
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            HandlerManager hm = getHandlerInstance(hi, null);
            hm.getHandler();
            Handler ch =  hm.getHandler();
            ch.initializeComponentFactory(m_componentDesc, m_componentMetadata);
            ((Pojo) ch).getComponentInstance().dispose();
        }
    }

    /**
     * Compute the factory name.
     */
    protected void computeFactoryName() {
        if (m_componentMetadata.containsAttribute("factory")) {
            // DEPRECATED BLOCK
            if (m_componentMetadata.getAttribute("factory").equalsIgnoreCase("no")) {
                m_logger.log(Logger.WARNING, "'factory=no' is deprecated, Please use 'factory=false' instead of 'factory='no'");
                m_factoryName = null;
                return;
            }
            // END OF DEPRECATED BLOCK
            if (m_componentMetadata.getAttribute("factory").equalsIgnoreCase("false")) {
                m_factoryName = null;
                return;
            }
            if (m_componentMetadata.getAttribute("factory").equalsIgnoreCase("true")) {
                if (m_typeName == null) { //m_typeName is necessary set for composite.
                    m_factoryName = m_componentMetadata.getAttribute("className");
                } else {
                    m_factoryName = m_typeName;
                }
                return;
            }
            // factory is set with the factory name
            m_factoryName = m_componentMetadata.getAttribute("factory");
            return;
        } else {
            if (m_typeName == null) { //m_typeName is necessary set for composite.
                m_factoryName = m_componentMetadata.getAttribute("className");
            } else {
                m_factoryName = m_typeName;
            }
            return;
        }
    }
    
    /**
     * Compute factory state.
     */
    protected void computeFactoryState() {
        boolean isValid = true;
        for (int i = 0; isValid && i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            isValid = hi.m_reference != null;
        }

        if (isValid && m_componentDesc == null) {
            try {
                computeDescription();
            } catch (org.apache.felix.ipojo.ConfigurationException e) {
                m_logger.log(Logger.ERROR, "The component type metadata are not correct : " + e.getMessage());
                stop();
                return;
            }
        }

        if (isValid && m_state == INVALID) {
            m_state = VALID;
            if (m_sr != null) {
                m_sr.setProperties(getProperties());
            }
            for (int i = 0; i < m_listeners.size(); i++) {
                ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, VALID);
            }
            return;
        }

        if (!isValid && m_state == VALID) {
            m_state = INVALID;

            final Collection col = m_componentInstances.values();
            final Iterator it = col.iterator();
            while (it.hasNext()) {
                InstanceManager ci = (InstanceManager) it.next();
                if (ci.getState() != ComponentInstance.DISPOSED) {
                    ci.kill();
                }
                m_instancesName.remove(ci.getInstanceName());
            }

            m_componentInstances.clear();

            if (m_sr != null) {
                m_sr.setProperties(getProperties());
            }

            for (int i = 0; i < m_listeners.size(); i++) {
                ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, INVALID);
            }
            return;
        }
    }
    
    /**
     * Compute required handlers.
     */
    protected void computeRequiredHandlers() {
        Element[] elems = m_componentMetadata.getElements();
        for (int i = 0; i < elems.length; i++) {
            Element current = elems[i]; 
            if (current.getName().equals("manipulation")) { continue; }
            HandlerIdentifier hi = new HandlerIdentifier(current.getName(), current.getNameSpace());
            if (! m_handlerIdentifiers.contains(hi)) { m_handlerIdentifiers.add(hi); }
        }
        
        // Add architecture if needed
        HandlerIdentifier hi = new HandlerIdentifier("architecture", "");
        if (! m_handlerIdentifiers.contains(hi) && m_componentMetadata.containsAttribute("architecture") && m_componentMetadata.getAttribute("architecture").equalsIgnoreCase("true")) {
            m_handlerIdentifiers.add(hi);
        }
        
        // Add lifecycle callback if immediate = true
        HandlerIdentifier hi2 = new HandlerIdentifier("callback", "");
        if (! m_handlerIdentifiers.contains(hi2) && m_componentMetadata.containsAttribute("immediate") && m_componentMetadata.getAttribute("immediate").equalsIgnoreCase("true")) {
            m_handlerIdentifiers.add(hi2);
        }
    }

    /**
     * Callback called by instance when disposed.
     * @param ci : the destroyed instance
     */
    protected void disposed(ComponentInstance ci) {
        m_instancesName.remove(ci.getInstanceName());
        m_componentInstances.remove(ci.getInstanceName());
    }
    
    /**
     * Generate an instance name.
     * @return an non already used name
     */
    protected synchronized String generateName() {
        String name = getName() + "-" + m_index;
        while (m_instancesName.contains(name)) {
            m_index = m_index + 1;
            name = getName() + "-" + m_index;
        }
        return name;
    }
    
    /**
     * Return the bundle context.
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

    protected String getComponentTypeName() {
        return m_typeName;
    }

    /**
     * Compute factory properties.
     * @return the properties.
     */
    protected Properties getProperties() {
        final Properties props = new Properties();

        props.put("component.class", m_componentClassName);
       
        props.put("factory.name", m_factoryName);
        props.put(Constants.SERVICE_PID, m_factoryName); // Service PID is required for the integration in the configuration admin.
        if (m_typeName != null) {
            props.put("component.type", m_typeName);
        }
        
        if (m_componentDesc != null) {
            props.put("component.providedServiceSpecifications", m_componentDesc.getprovidedServiceSpecification());
            props.put("component.properties", m_componentDesc.getProperties());
            props.put("component.description", m_componentDesc);
            props.put("component.desc", m_componentDesc.toString());
        }
        
        // Add factory state
        props.put("factory.state", "" + m_state);
        
        return props;
    }
    
    /**
     * Check if the given handler identifier and the service reference can match.
     * @param hi : the handler identifier.
     * @param ref : the service reference.
     * @return true if the service reference can fulfill the handler requirement
     */
    protected boolean match(HandlerIdentifier hi, ServiceReference ref) {
        String name = (String) ref.getProperty(Handler.HANDLER_NAME_PROPERTY);
        String ns = (String) ref.getProperty(Handler.HANDLER_NAMESPACE_PROPERTY);
        if (IPojoConfiguration.IPOJO_NAMESPACE.equals(ns)) {
            ns = "";
        }
        return name.equals(hi.m_name) && ns.equals(hi.m_namespace); 
    }

    /**
     * Return an handler object.
     * 
     * @param hi : handler to create.
     * @param sc : service context in which create the handler (instance context).
     * @return the Handler object.
     */
    private HandlerManager getHandlerInstance(HandlerIdentifier hi, ServiceContext sc) {
        Factory factory = hi.getFactory();
        try {
            return (HandlerManager) factory.createComponentInstance(null, sc);
        } catch (MissingHandlerException e) {
            m_logger.log(Logger.ERROR, "The creation of the handler " + hi.getFullName() + " has failed: " + e.getMessage());
            return null;
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The creation of the handler " + hi.getFullName() + " has failed (UnacceptableConfiguration): " + e.getMessage());
            return null;
        } catch (org.apache.felix.ipojo.ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The configuration of the handler " + hi.getFullName() + " has failed (ConfigurationException): " + e.getMessage());
            return null;
        }
    }

    /**
     * Structure storing required handlers.
     */
    class HandlerIdentifier {
        /**
         * Factory to create this handler.
         */
        private Factory m_factory;
        
        /**
         * Handler name.
         */
        private String m_name;
        
        /**
         * Handler namespace.
         */
        private String m_namespace;
        
        /**
         * Service Reference of the handler factory.
         */
        private ServiceReference m_reference;
        
        /**
         * Constructor.
         * @param n : handler name.
         * @param ns : handler namespace.
         */
        public HandlerIdentifier(String n, String ns) {
            m_name = n;
            m_namespace = ns;
        }
        
        /**
         * Equals method.
         * Two handlers are equals if they have same name and namespace or they share the same service reference.
         * @param o : object to compare to the current object.
         * @return : true if the two compared object are equals
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof HandlerIdentifier) {
                return ((HandlerIdentifier) o).getName().equalsIgnoreCase(m_name) && ((HandlerIdentifier) o).getNamespace().equalsIgnoreCase(m_namespace);
            } 
            return false;
        }
        
        /**
         * Get the factory object used for this handler.
         * The object is get when used for the first time.
         * @return the factory object.
         */
        public Factory getFactory() {
            if (m_reference == null) {
                return null;
            }
            if (m_factory == null) {
                m_factory = (Factory) m_tracker.getService(getReference());
            }
            return m_factory;
        }
        
        /**
         * Get the handler full name (namespace:name).
         * @return the handler full name
         */
        public String getFullName() {
            if ("".equals(m_namespace)) {
                return IPojoConfiguration.IPOJO_NAMESPACE + ":" + m_name;
            } else {
                return m_namespace + ":" + m_name;
            }
        }
        
        public String getName() {
            return m_name;
        }
        
        public String getNamespace() {
            return m_namespace;
        }
        
        public ServiceReference getReference() {
            return m_reference;
        }
        
        /**
         * Set the service reference.
         * If the new service reference is null, it unget the used factory (if already get).
         * @param ref : new service reference.
         */
        public void setReference(ServiceReference ref) {
            if (m_reference != null) {
                m_tracker.ungetService(m_reference);
                m_factory = null;
            }
            m_reference = ref;
        }
    }

    /**
     * FactoryClassloader.
     */
    private class FactoryClassloader extends ClassLoader {

        /**
         * Map of defined classes [Name, Class Object].
         */
        private Map m_definedClasses = new HashMap();

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
            final Class c = super.defineClass(name, b, 0, b.length, domain);
            m_definedClasses.put(name, c);
            return c;
        }

        /**
         * Return the URL of the asked resource.
         * 
         * @param arg : the name of the resource to find.
         * @return the URL of the resource.
         * @see java.lang.ClassLoader#getResource(java.lang.String)
         */
        public URL getResource(String arg) {
            return m_context.getBundle().getResource(arg);
        }

        /**
         * Load the class.
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         * @param name : the name of the class
         * @param resolve : should be the class resolve now ?
         * @return : the loaded class
         * @throws ClassNotFoundException : the class to load is not found
         */
        protected Class loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            return m_context.getBundle().loadClass(name);
        }
    }
}
