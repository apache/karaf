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
import java.util.Properties;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
 * The component factory manages component instance objects. This management
 * consist in creating and managing component instance build with the component
 * factory. This class could export Factory and ManagedServiceFactory services.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerFactory extends ComponentFactory implements Factory {

    /**
     * iPOJO Default Namespace.
     */
    public static final String IPOJO_NAMESPACE = "org.apache.felix.ipojo";

    /**
     * Handler type (composite|primitive).
     * Default: handler.
     */
    private String m_type = "primitive";

    /**
     * Default iPOJO Namespace.
     */
    private String m_namespace = IPOJO_NAMESPACE;

    /**
     * Get the handler start level.
     * Lower level are priority are configured and started before higher level, and are stopped after.
     * 
     */
    private int m_level = Integer.MAX_VALUE;

    /**
     * Create a composite factory.
     * @param context : bundle context
     * @param metadata : metadata of the component to create
     * @throws ConfigurationException occurs when the element describing the factory is malformed.
     */
    public HandlerFactory(BundleContext context, Element metadata) throws ConfigurationException {
        super(context, metadata);

        // Get the name
        m_factoryName = metadata.getAttribute("name");
        if (m_factoryName == null) { throw new ConfigurationException("An Handler needs a name"); }

        // Get the type
        String type = metadata.getAttribute("type");
        if (type != null) {
            m_type = type;
        }

        String level = metadata.getAttribute("level");
        if (level != null) {
            m_level = new Integer(level).intValue();
        }

        // Get the namespace
        String namespace = metadata.getAttribute("namespace");
        if (namespace != null) {
            m_namespace = namespace.toLowerCase();
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
     * Stop the factory.
     * This method does not disposed created instances.
     * These instances will be disposed by the instance managers.
     */
    public synchronized void stopping() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
    }

    /**
     * Compute factory service properties.
     * This method add three mandatory handler factory properties (name, namespace and type)
     * @return the properties.
     * @see org.apache.felix.ipojo.ComponentFactory#getProperties()
     */
    protected Properties getProperties() {
        Properties props = new Properties();

        return props;
    }

    /**
     * Create an instance. The given configuration needs to contain the 'name'
     * property.
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
    }
}
