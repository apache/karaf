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
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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
     * Create a composite factory.
     * @param bc : bundle context
     * @param cm : metadata of the component to create
     */
    public HandlerFactory(BundleContext bc, Element cm) {
        super(bc, cm);
        
        // Get the name
        if (cm.containsAttribute("name")) {
            m_typeName = cm.getAttribute("name").toLowerCase();
        } else {
            System.err.println("An Handler needs a name");
            return;
        }
        
        // Get the type
        if (cm.containsAttribute("type")) {
            m_type = cm.getAttribute("type");
        }
        
        // Get the namespace
        if (cm.containsAttribute("namespace")) {
            m_namespace = cm.getAttribute("namespace").toLowerCase();
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

    /**
     * Start all the instance managers.
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
        
        // Check if the factory should be exposed
        if (m_factoryName == null) { return; }
        
        // Exposition of the factory service
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
        
        m_tracker.close();
        
        // Release each handler
        for (int i = 0; i < m_handlerIdentifiers.size(); i++) {
            HandlerIdentifier hi = (HandlerIdentifier) m_handlerIdentifiers.get(i);
            if (hi.getReference() != null) {
                hi.setReference(null);
            }
        }
        
        m_handlerIdentifiers.clear();        
        m_listeners.clear();
        m_tracker = null;
        m_componentDesc = null;
        m_state = INVALID;        
    }
    
    
    /**
     * Compute factory service properties.
     * This method add three mandatory handler factory properties (name, namespace and type)
     * @return the properties.
     * @see org.apache.felix.ipojo.ComponentFactory#getProperties()
     */
    protected Properties getProperties() {
        final Properties props = super.getProperties();

        props.put(Handler.HANDLER_NAME_PROPERTY, m_typeName);
        props.put(Handler.HANDLER_NAMESPACE_PROPERTY, m_namespace);
        props.put(Handler.HANDLER_TYPE_PROPERTY, m_type);
        
        return props;
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
        HandlerManager instance = new HandlerManager(this, context, (HandlerManager[]) handlers.toArray(new HandlerManager[0]));
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
}
