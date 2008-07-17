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
package org.apache.felix.ipojo.composite;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;

/**
 * The component factory manages component instance objects. This management
 * consist in creating and managing component instance build with the component
 * factory. This class could export Factory and ManagedServiceFactory services.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeFactory extends ComponentFactory implements TrackerCustomizer {

    /**
     * Tracker used to track required handler factories.
     */
    protected Tracker m_tracker;

    /**
     * Create a composite factory.
     * @param context : bundle context
     * @param metadata : metadata of the component to create
     * @throws ConfigurationException occurs when the element describing the factory is malformed.
     */
    public CompositeFactory(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
    }
    
    /**
     * Check if the metadata are well formed.
     * @param metadata : metadata
     * @throws ConfigurationException occurs when the element describing the factory is malformed.
     * @see org.apache.felix.ipojo.ComponentFactory#check(org.apache.felix.ipojo.metadata.Element)
     */
    public void check(Element metadata) throws ConfigurationException {
        String name = metadata.getAttribute("name");
        if (name == null) {
            throw new ConfigurationException("A composite needs a name : " + metadata);
        }
    }
    
    public String getClassName() { return "composite"; }
        
    
    /**
     * Compute required handlers.
     * @return the list of required handler.
     */
    public List getRequiredHandlerList() {
        List list = new ArrayList();
        Element[] elems = m_componentMetadata.getElements();
        for (int i = 0; i < elems.length; i++) {
            Element current = elems[i]; 
            RequiredHandler req = new RequiredHandler(current.getName(), current.getNameSpace());
            if (! list.contains(req)) { list.add(req); }
        }
        
        // Add architecture if architecture != 'false'
        String arch = m_componentMetadata.getAttribute("architecture");
        if (arch == null || arch.equalsIgnoreCase("true")) {
            RequiredHandler req = new RequiredHandler("architecture", null);
            if (! list.contains(req)) { list.add(req); }
        }
        
        return list;
    }
    
    /**
     * Stop all the instance managers.
     */
    public synchronized void stopping() {
        if (m_tracker != null) {
            m_tracker.close();
        }
        m_tracker = null;
    }

    /**
     * Start all the instance managers.
     */
    public synchronized void starting() {
        if (m_requiredHandlers.size() != 0) {
            try {
                String filter = "(&(" + Constants.OBJECTCLASS + "=" + HandlerFactory.class.getName() + ")"
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
    }
    
    /**
     * Create an instance from the current factory.
     * @param configuration : instance configuration
     * @param context : bundle context to inject in the instance manager
     * @param handlers : array of handler object to attached on the instance 
     * @return the created instance
     * @throws ConfigurationException either the instance configuration or the instance starting has failed 
     * @see org.apache.felix.ipojo.ComponentFactory#createInstance(java.util.Dictionary, org.apache.felix.ipojo.IPojoContext, org.apache.felix.ipojo.HandlerManager[])
     */
    public ComponentInstance createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers) throws ConfigurationException {
        CompositeManager inst = new CompositeManager(this, context, handlers);
        inst.configure(m_componentMetadata, configuration);
        inst.start();
        return inst;
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
        String name = (String) properties.get("name");
        
        ComponentInstance instance = (CompositeManager) m_componentInstances.get(name);
        
        if (instance == null) {
            return; // The instance does not exist.
        }
        
        instance.reconfigure(properties); // re-configure the component
    }

    public String getFactoryName() {
        return m_componentMetadata.getAttribute("name"); // Mandatory attribute.
    }

}
