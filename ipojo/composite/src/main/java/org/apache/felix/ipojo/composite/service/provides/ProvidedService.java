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
package org.apache.felix.ipojo.composite.service.provides;

import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.composite.CompositeManager;
import org.apache.felix.ipojo.composite.instance.InstanceHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.DependencyStateListener;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Composite Provided Service.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedService implements DependencyStateListener {

    /**
     * Composite Manager.
     */
    private CompositeManager m_manager;

    /**
     * Composition Model.
     */
    private CompositionMetadata m_composition;

    /**
     * Internal context.
     */
    private ServiceContext m_scope;

    /**
     * External context.
     */
    private BundleContext m_context;

    /**
     * Created Factory.
     */
    private ComponentFactory m_factory;

    /**
     * Created Instance.
     */
    private ComponentInstance m_instance;

    /**
     * Exporter.
     */
    private ServiceExporter m_exports;

    /**
     * Created instance name.
     */
    private String m_instanceName;

    /**
     * Constructor.
     * The delegation mapping is infers in this method.
     * @param handler : the handler.
     * @param element : 'provides' element.
     * @param name : name of this provided service.
     */
    public ProvidedService(ProvidedServiceHandler handler, Element element, String name) {
        m_manager = handler.getCompositeManager();
        m_scope = m_manager.getServiceContext();
        m_context = m_manager.getContext();
        m_composition = new CompositionMetadata(m_manager.getContext(), element, handler, name);
    }

    /**
     * Start method.
     * Build service implementation type, factory and instance.
     * @throws CompositionException if a consistent mapping cannot be discovered.
     */
    public void start() throws CompositionException {
        m_composition.buildMapping();

        m_instanceName = m_composition.getSpecificationMetadata().getName() + "Provider-Gen";
        byte[] clazz = m_composition.buildPOJO();
        Element metadata = m_composition.buildMetadata(m_instanceName);

        // Create the factory
        try {
            m_factory = new ComponentFactory(m_context, clazz, metadata);
            m_factory.start();
        } catch (ConfigurationException e) {
            // Should not happen.
            m_manager.getFactory().getLogger().log(Logger.ERROR, "A factory cannot be created", e);
        }

        try {
            Class spec = DependencyModel.loadSpecification(m_composition.getSpecificationMetadata().getName(), m_context);
            Filter filter = m_context.createFilter("(instance.name=" + m_instanceName + ")");
            // Create the exports
            m_exports = new ServiceExporter(spec, filter, false, false, null, DependencyModel.DYNAMIC_BINDING_POLICY, m_scope, m_context, this, m_manager);
        } catch (InvalidSyntaxException e) {
            throw new CompositionException("A provided service filter is invalid : " + e.getMessage());
        } catch (ConfigurationException e) {
            throw new CompositionException("The class " + m_composition.getSpecificationMetadata().getName() + " cannot be loaded : " + e.getMessage());
        }
    }

    /**
     * Stop the provided service.
     * Kill the exporter, the instance and the factory.
     */
    public void stop() {
        if (m_exports != null) {
            m_exports.stop();
            m_exports = null;
        }
        if (m_instance != null) {
            m_instance.dispose();
            m_instance = null;
        }
        if (m_factory != null) {
            m_factory.stop();
            m_factory = null;
        }
    }

    protected CompositeManager getManager() {
        return m_manager;
    }

    /**
     * The exporter becomes valid.
     * @param exporter : the exporter
     */
    public void validate(DependencyModel exporter) {
        // Nothing to do.
    }

    /**
     * The exporter becomes invalid.
     * @param exporter : the exporter
     */
    public void invalidate(DependencyModel exporter) {
        // Nothing to do.
    }

    /**
     * Get an object from the given type.
     * @param type : type
     * @return an object from an instance of this type or null
     */
    private Object getObjectByType(String type) {
        InstanceHandler handler = (InstanceHandler) m_manager.getCompositeHandler("org.apache.felix.ipojo.composite.instance.InstanceHandler");
        Object pojo = handler.getObjectFromInstance(type);
        if (pojo == null) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "An instance object cannot be found for the type : " + type);
        }
        return pojo;
    }

    public String getSpecification() {
        return m_composition.getSpecificationMetadata().getName();
    }

    /**
     * Unregister the exposed service.
     */
    public void unregister() {
        m_exports.stop();
    }

    /**
     * Register the exposed service.
     */
    public void register() {
        Properties props = new Properties();
        props.put("name", m_instanceName);
        List fields = m_composition.getFieldList();
        for (int i = 0; i < fields.size(); i++) {
            FieldMetadata field = (FieldMetadata) fields.get(i);
            if (field.isUseful() && !field.getSpecification().isInterface()) {
                String type = field.getSpecification().getComponentType();
                Object pojo = getObjectByType(type);
                props.put(field.getName(), pojo);
            }
        }

        if (m_instance == null) {
         // Else we have to create the instance 
            try {
                m_instance = m_factory.createComponentInstance(props, m_manager.getServiceContext());
            } catch (UnacceptableConfiguration e) {
                throw new IllegalStateException("Cannot create the service implementation : " + e.getMessage());
            } catch (MissingHandlerException e) {
                throw new IllegalStateException("Cannot create the service implementation : " + e.getMessage());
            } catch (ConfigurationException e) {
                throw new IllegalStateException("Cannot create the service implementation : " + e.getMessage());
            }
        } else {
            // We have to reconfigure the instance in order to inject up to date glue component instance.
            m_instance.reconfigure(props);
        }
        
        m_exports.start();
    }

    public boolean isRegistered() {
        return m_exports.getState() == DependencyModel.RESOLVED;
    }

}
