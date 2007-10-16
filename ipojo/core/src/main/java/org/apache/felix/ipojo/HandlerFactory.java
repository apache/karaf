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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

/**
 * The component factory manages component instance objects. This management
 * consist in creating and managing component instance build with the component
 * factory. This class could export Factory and ManagedServiceFactory services.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerFactory extends ComponentFactory implements Factory {

    /**
     * Service Registration of this factory (Factory & ManagedServiceFactory).
     */
    private ServiceRegistration m_sr;
    
    /**
     * Handler type (composite|primitive).
     * Default: handler.
     */
    private String m_type = "primitive";
    
    /**
     * Default iPOJO Namespace.
     */
    private String m_namespace = IPojoConfiguration.IPOJO_NAMESPACE;

    /**
     * Get the handler start level.
     * Lower level are priority are configured and started before higher level, and are stopped after.
     * 
     */
    private int m_level = Integer.MAX_VALUE;

    /**
     * Create a composite factory.
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public HandlerFactory(BundleContext bc, Element cm) {
        super(bc, cm);
        
        // Get the name
        m_factoryName = cm.getAttribute("name").toLowerCase();
        if (m_factoryName == null) {
            System.err.println("An Handler needs a name");
            return;
        }
        
        // Get the type
        String t = cm.getAttribute("type");
        if (t != null) {
            m_type = t;
        }
        
        String l = cm.getAttribute("level");
        if (l != null) {
            m_level = new Integer(l).intValue();
        }
        
        // Get the namespace
        String ns = cm.getAttribute("namespace");
        if (ns != null) {
            m_namespace = ns.toLowerCase();
        }        
    }
    
    public String getNamespace() {
        return m_namespace;
    }
    
    public String getHandlerName() {
        return m_namespace + ":" + getName();
    }
    
    public String getType() {
        return m_type;
    }
    
    public int getStartLevel() {
        return m_level;
    }

    /**
     * Start all the instance managers.
     */
    public synchronized void start() {
        if (m_sr != null) { // Already started.
            return;
        }
        
        if (m_handlerIdentifiers.size() != 0) {
            try {
                String filter = "(&(" + Handler.HANDLER_TYPE_PROPERTY + "=" + PrimitiveHandler.HANDLER_TYPE + ")" 
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
            m_logger.log(Logger.ERROR, "Cannot initilize the factory " + e.getMessage());
            stop();
            return;
        }
        
        // Exposition of the factory service (always public for handlers)
        m_sr = m_context.registerService(new String[] { Factory.class.getName()}, this, getProperties());
    }
    
    /**
     * Stop the factory.
     * This method does not disposed created instances.
     * These instances will be disposed by the instance managers.
     */
    public synchronized void stop() {
        if (m_sr != null) {
            m_sr.unregister();
            m_sr = null;
        }
        
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
        
        // Release each handler
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            ((HandlerIdentifier) m_handlerIdentifiers.get(i)).unRef();
        }
        
        m_handlerIdentifiers.clear();        
        m_listeners.clear();
        m_state = INVALID;        
    }
    
    
    /**
     * Compute factory service properties.
     * This method add three mandatory handler factory properties (name, namespace and type)
     * @return the properties.
     * @see org.apache.felix.ipojo.ComponentFactory#getProperties()
     */
    protected Properties getProperties() {
        Properties props = new Properties();
        
        // Add factory state
        props.put("factory.state", "" + m_state);

        props.put(Handler.HANDLER_NAME_PROPERTY, m_factoryName);
        props.put(Handler.HANDLER_NAMESPACE_PROPERTY, m_namespace);
        props.put(Handler.HANDLER_TYPE_PROPERTY, m_type);
        if (m_level != Integer.MAX_VALUE) {
            props.put(Handler.HANDLER_LEVEL_PROPERTY, new Integer(m_level));
        }
        
        return props;
    }
    
    /**
     * Compute handler factory state.
     * @throws ConfigurationException : occurs when an handler cannot be initialized.
     */
    protected void computeFactoryState() throws ConfigurationException {
        boolean isValid = true;
        for (int i = 0; isValid && i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            isValid = hi.getReference() != null;
        }
        
        if (isValid) {            
            if (m_state == INVALID) {
                m_state = VALID;
                
                if (m_sr == null) {
                    m_componentDesc = new ComponentDescription(this);
                    for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
                        HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
                        HandlerManager hm = getHandlerInstance(hi, null);
                        hm.getHandler();
                        Handler ch = hm.getHandler();
                        ch.setLogger(getLogger());
                        ch.initializeComponentFactory(m_componentDesc, m_componentMetadata);
                        ((Pojo) ch).getComponentInstance().dispose();
                    }
                }
                
                if (m_sr != null) {
                    m_sr.setProperties(getProperties());
                }
                for (int i = 0; i < m_listeners.size(); i++) {
                    ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, VALID);
                }
                return;
            }
        } else {
            if (m_state == VALID) {
                m_state = INVALID;
                
                // Notify listeners.
                for (int i = 0; i < m_listeners.size(); i++) {
                    ((FactoryStateListener) m_listeners.get(i)).stateChanged(this, INVALID);
                }

                // Dispose created instances.
                final Collection col = m_componentInstances.values();
                final Iterator it = col.iterator();
                while (it.hasNext()) {
                    InstanceManager ci = (InstanceManager) it.next();
                    if (ci.getState() != ComponentInstance.DISPOSED) {
                        ci.kill();
                    }
                    m_instancesName.remove(ci.m_name);
                }

                m_componentInstances.clear();

                if (m_sr != null) {
                    m_sr.setProperties(getProperties());
                }
                
                return;
            }
        }
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
    public synchronized ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration, MissingHandlerException, org.apache.felix.ipojo.ConfigurationException {
        if (m_state == Factory.INVALID) {
            throw new MissingHandlerException(getMissingHandlers());
        }
        
        if (configuration == null) {
            configuration = new Properties();
        }
        
        String in = null;
        if (configuration.get("name") != null) {
            in = (String) configuration.get("name");
        } else {
            in = generateName();
            configuration.put("name", in);
        }
        
        if (m_instancesName.contains(in)) {
            throw new UnacceptableConfiguration("Name already used : " + in);
        } else {
            m_instancesName.add(in);
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
        
        HandlerManager instance = new HandlerManager(this, context, (HandlerManager[]) handlers.toArray(new HandlerManager[handlers.size()]));
        instance.configure(m_componentMetadata, configuration);

        m_componentInstances.put(in, instance);
        
        
        return instance;
    }
    
    /**
     * Return an handler object.
     * @param hi : handler to create.
     * @param sc : service context in which create the handler (instance context).
     * @return the Handler object.
     */
    private HandlerManager getHandlerInstance(HandlerIdentifier hi, ServiceContext sc) {
        try {
            return (HandlerManager) hi.getFactory().createComponentInstance(null, sc);
        } catch (MissingHandlerException e) {
            m_logger.log(Logger.ERROR, "The creation of the handler " + hi.getFullName() + " has failed: " + e.getMessage());
            stop();
            return null;
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The creation of the handler " + hi.getFullName() + " has failed (UnacceptableConfiguration): " + e.getMessage());
            stop();
            return null;
        } catch (org.apache.felix.ipojo.ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The configuration of the handler " + hi.getFullName() + " has failed (ConfigurationException): " + e.getMessage());
            stop();
            return null;
        }
    }
}
