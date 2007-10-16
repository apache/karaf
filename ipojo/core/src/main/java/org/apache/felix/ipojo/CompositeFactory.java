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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The component factory manages component instance objects. This management
 * consist in creating and managing component instance build with the component
 * factory. This class could export Factory and ManagedServiceFactory services.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeFactory extends ComponentFactory implements Factory {

    /**
     * Service Registration of this factory (Factory & ManagedServiceFactory).
     */
    private ServiceRegistration m_sr;

    /**
     * Create a composite factory.
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public CompositeFactory(BundleContext bc, Element cm) {
        super(bc, cm);
    }
    
    /**
     * Check if the metadata are well formed.
     * @param cm : metadata
     * @return true if the metadata are correct.
     * @see org.apache.felix.ipojo.ComponentFactory#check(org.apache.felix.ipojo.metadata.Element)
     */
    public boolean check(Element cm) {
        m_factoryName = cm.getAttribute("name");
        if (m_factoryName == null) {
            System.err.println("A composite needs a name");
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * Check if the given handler identifier can be fulfilled by the given service reference.
     * @param hi : handler identifier.
     * @param ref : service reference.
     * @return true if the service reference can fulfill the given handler identifier
     * @see org.apache.felix.ipojo.ComponentFactory#match(org.apache.felix.ipojo.ComponentFactory.HandlerIdentifier, org.osgi.framework.ServiceReference)
     */
    public boolean match(HandlerIdentifier hi, ServiceReference ref) {
        String name = (String) ref.getProperty(Handler.HANDLER_NAME_PROPERTY);
        String ns = (String) ref.getProperty(Handler.HANDLER_NAMESPACE_PROPERTY);
        String type = (String) ref.getProperty(Handler.HANDLER_TYPE_PROPERTY);
        
        if ("composite".equals(type)) {
            if (IPojoConfiguration.IPOJO_NAMESPACE.equals(ns)) {
                return name.equals(hi.getName()) && hi.getNamespace() == null;
            }
            return name.equals(hi.getName()) && ns.equals(hi.getNamespace()); 
        } else {
            return false;
        }
    }
        
    
    /**
     * Compute required handlers.
     */
    protected void computeRequiredHandlers() {
        Element[] elems = m_componentMetadata.getElements();
        for (int i = 0; i < elems.length; i++) {
            Element current = elems[i]; 
            HandlerIdentifier hi = new HandlerIdentifier(current.getName(), current.getNameSpace());
            if (! m_handlerIdentifiers.contains(hi)) { m_handlerIdentifiers.add(hi); }
        }
        
        // Add architecture if architecture != 'false'
        String arch = m_componentMetadata.getAttribute("architecture");
        if (arch == null || arch.equalsIgnoreCase("true")) {
            HandlerIdentifier hi = new HandlerIdentifier("architecture", null);
            if (! m_handlerIdentifiers.contains(hi)) { m_handlerIdentifiers.add(hi); }
        }
    }
    
    /**
     * Stop all the instance managers.
     */
    public synchronized void stop() {
        if (m_tracker != null) {
            m_tracker.close();
        }
        
        final Collection col = m_componentInstances.values();
        final Iterator it = col.iterator();
        while (it.hasNext()) {
            CompositeManager ci = (CompositeManager) it.next();
            if (ci.getState() != ComponentInstance.DISPOSED) {
                ci.kill();
            }
            m_instancesName.remove(ci.getInstanceName());
        }
        
        m_componentInstances.clear();
        if (m_sr != null) { 
            m_sr.unregister();
            m_sr = null;
        }
        m_tracker = null;
        m_componentDesc = null;
        m_state = INVALID;
    }

    /**
     * Start all the instance managers.
     */
    public synchronized void start() {
        if (m_componentDesc != null) { // Already started.
            return;
        } 
        if (m_handlerIdentifiers.size() != 0) {
            try {
                String filter = "(&(" + Constants.OBJECTCLASS + "=" + Factory.class.getName() + ")"
                    + "(" + Handler.HANDLER_TYPE_PROPERTY + "=" + CompositeHandler.HANDLER_TYPE + ")" 
                    + "(factory.state=1)"
                    + ")";
                m_tracker = new Tracker(m_context, m_context.createFilter(filter), this);
                m_tracker.open();
            } catch (InvalidSyntaxException e) {
                m_logger.log(Logger.ERROR, "A factory filter is not valid: " + e.getMessage());
                stop();
                return;
            }
        }

        try {
            computeFactoryState();
        } catch (ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The component type metadata are not correct : " + e.getMessage());
            stop();
            return;
        }
        
        // Check if the factory should be exposed
        if (m_isPublic) {
            // Exposition of the factory service
            m_sr = m_context.registerService(new String[] { Factory.class.getName() }, this, getProperties());
        }
    }
    
    
    /**
     * Compute factory service properties.
     * @return the factory service properties
     * @see org.apache.felix.ipojo.ComponentFactory#getProperties()
     */
    protected Properties getProperties() {
        final Properties props = new Properties();
        
        if (m_componentDesc != null) {
            props.put("component.providedServiceSpecifications", m_componentDesc.getprovidedServiceSpecification());
            props.put("component.properties", m_componentDesc.getProperties());
            props.put("component.description", m_componentDesc);
        }
        
        // Add factory state
        props.put("factory.state", "" + m_state);
        
        props.put("factory.name", m_factoryName);
        
        return props;
    }
    
    public String getClassName() {
        return "composite";
    }


    /**
     * Create an instance. The given configuration needs to contain the 'name'
     * property.
     * @param configuration : configuration of the created instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration : occurs if the given configuration is
     * not consistent with the component type of this factory.
     * @throws MissingHandlerException  : occurs if an handler is unavailable when the instance is created.
     * @throws ConfigurationException  : occurs if an error occurs during the instance configuration.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public synchronized ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
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
     * @throws ConfigurationException  : occurs when the instance configuration failed.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public synchronized ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        if (configuration == null) {
            configuration = new Properties();
        }
        
        try {
            checkAcceptability(configuration);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
            throw new UnacceptableConfiguration("The configuration " + configuration + " is not acceptable for " + m_factoryName + ": " + e.getMessage());
        }

        
        String in = null;
        if (configuration.get("name") != null) {
            in = (String) configuration.get("name");
        } else {
            in = generateName();
            configuration.put("name", in);
        }
        
        if (m_instancesName.contains(in)) {
            throw new UnacceptableConfiguration("Name already used : " + in + "(" + m_instancesName + ")");
        } else {
            m_instancesName.add(in);
        }

        BundleContext context = null;
        if (serviceContext == null) {
            context = new IPojoContext(m_context);
        } else {
            context = new IPojoContext(m_context, serviceContext);
        }
        ComponentInstance instance = null;
        List handlers = new ArrayList();
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            handlers.add(getHandlerInstance(hi, serviceContext));
        }
       
        final CompositeManager inst = new CompositeManager(this, context, (HandlerManager[]) handlers.toArray(new HandlerManager[0]));
        inst.configure(m_componentMetadata, configuration);
        instance = inst;

        m_componentInstances.put(in, instance);
        instance.start();
        return instance;
    }

    /**
     * Reconfigure an existing instance.
     * @param properties : the new configuration to push.
     * @throws UnacceptableConfiguration : occurs if the new configuration is
     * not consistent with the component type.
     * @throws MissingHandlerException : occurs when an handler is unavailable when creating the instance.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration, MissingHandlerException {
        if (properties == null || properties.get("name") == null) {
            throw new UnacceptableConfiguration("The configuration does not contains the \"name\" property");
        }
        final String name = (String) properties.get("name");
        
        ComponentInstance cm = (CompositeManager) m_componentInstances.get(name);
        
        if (cm == null) {
            return; // The instance does not exist.
        }
        
        cm.reconfigure(properties); // re-configure the component
    }
    
    /**
     * Compute factory state.
     * @throws ConfigurationException : occurs if the component type cannot be initialized correctly.
     */
    protected void computeFactoryState() throws ConfigurationException {
        boolean isValid = true;
        for (int i = 0; isValid && i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            isValid = hi.getReference() != null;
        }

        if (isValid && m_componentDesc == null) {
            computeDescription();
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
                CompositeManager ci = (CompositeManager) it.next();
                ci.kill();
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
     * Compute component type description.
     * @throws ConfigurationException : if one handler has rejected the configuration.
     * @see org.apache.felix.ipojo.ComponentFactory#computeDescription()
     */
    public void computeDescription() throws ConfigurationException {
        List l = new ArrayList();
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            l.add(hi.getFullName());
        }
        
        m_componentDesc = new ComponentDescription(this);
       
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
     * Get a composite handler object for the given handler identifier.
     * @param handler : the handler identifier.
     * @param sc : the service context in which creating the handler.
     * @return the composite handler object or null if not found.
     */
    private HandlerManager getHandlerInstance(HandlerIdentifier handler, ServiceContext sc) {
        Factory factory = (Factory) m_context.getService(handler.getReference());
        try {
            return (HandlerManager) factory.createComponentInstance(null, sc);
        } catch (MissingHandlerException e) {
            m_logger.log(Logger.ERROR, "The creation of the composite handler " + handler.getFullName() + " has failed: " + e.getMessage());
            stop();
            return null;
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The creation of the composite handler " + handler.getFullName() + " has failed (UnacceptableConfiguration): " + e.getMessage());
            stop();
            return null;
        } catch (ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The creation of the composite handler " + handler.getFullName() + " has failed (ConfigurationException): " + e.getMessage());
            stop();
            return null;
        }
    }
}
