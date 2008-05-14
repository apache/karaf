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

import java.util.Dictionary;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * Implementation of the handler factory interface.
 * This factory is able to create handler manager.
 * A handler manager is an iPOJO instance containing a handler object. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerManagerFactory extends ComponentFactory implements HandlerFactory {

    /**
     * Handler type (composite|primitive).
     */
    private final String m_type;

    /**
     * iPOJO Handler Namespace.
     * (Set the the iPOJO default namespace is not specified)
     */
    private final String m_namespace;

    /**
     * Handler start level.
     * Lower level are priority are configured and started before higher level, and are stopped after. 
     */
    private final int m_level;
    
    /**
     * Creates a handler factory.
     * @param context : bundle context
     * @param metadata : metadata of the component to create
     * @throws ConfigurationException occurs when the element describing the factory is malformed.
     */
    public HandlerManagerFactory(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);
        
        // Get the name
        m_factoryName = metadata.getAttribute("name");
        if (m_factoryName == null) { throw new ConfigurationException("An Handler needs a name"); }

        // Get the type
        String type = metadata.getAttribute("type");
        if (type != null) {
            m_type = type;
        } else {
            m_type = "primitive"; // Set to primitive if not specified.
        }

        String level = metadata.getAttribute("level");
        if (level != null) {
            m_level = new Integer(level).intValue();
        } else {
            m_level = Integer.MAX_VALUE; // Set to max if not specified.
        }

        // Get the namespace
        String namespace = metadata.getAttribute("namespace");
        if (namespace != null) {
            m_namespace = namespace.toLowerCase();
        } else {
            m_namespace = IPOJO_NAMESPACE; // Set to the iPOJO default namespace if not specified.
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

    public ComponentTypeDescription getComponentTypeDescription() {
        return new HandlerTypeDescription(this);
    }

    /**
     * Stops the factory.
     * This method does not disposed created instances.
     * These instances will be disposed by the instance managers.
     * This method is called with the lock.
     */
    public void stopping() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
    }

    /**
     * Creates an instance. The given configuration needs to contain the 'name'
     * property. This method is called when holding the lock.
     * @param configuration : configuration of the created instance.
     * @param context : the service context to push for this instance.
     * @param handlers : handler array to used.
     * @return the created component instance.
     * not consistent with the component type of this factory.
     * @throws org.apache.felix.ipojo.ConfigurationException : when the instance configuration failed.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createInstance(Dictionary configuration, IPojoContext context, HandlerManager[] handlers) throws ConfigurationException {
        HandlerManager instance = new HandlerManager(this, context, handlers);
        instance.configure(m_componentMetadata, configuration);
        return instance;
    }

    private class HandlerTypeDescription extends ComponentTypeDescription {

        /**
         * Constructor.
         * @param factory : factory.
         */
        public HandlerTypeDescription(Factory factory) {
            super(factory);
        }

        /**
         * Add properties to publish : 
         * handler.name, handler.namespace, handler.type and handler.level if the level is not Integer.MAX.
         * @return return the dictionary to publish.
         * @see org.apache.felix.ipojo.architecture.ComponentTypeDescription#getPropertiesToPublish()
         */
        public Dictionary getPropertiesToPublish() {
            Dictionary props = super.getPropertiesToPublish();

            props.put(Handler.HANDLER_NAME_PROPERTY, m_factoryName);
            props.put(Handler.HANDLER_NAMESPACE_PROPERTY, m_namespace);
            props.put(Handler.HANDLER_TYPE_PROPERTY, m_type);
            if (m_level != Integer.MAX_VALUE) {
                props.put(Handler.HANDLER_LEVEL_PROPERTY, new Integer(m_level));
            }
            return props;
        }
        
        public String[] getFactoryInterfacesToPublish() {
            return new String[] {HandlerFactory.class.getName()};
        }
    }
}
