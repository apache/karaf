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
package org.apache.felix.ipojo.composite.instance;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.CompositeManager;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Composite Instance Handler.
 * This handler allo to create an instance inside a composite.
 * This instance is determine by its type and a configuration.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceHandler extends CompositeHandler implements ServiceListener, InstanceStateListener {

    /**
     * Bundle context.
     */
    private BundleContext m_context;
    
    /**
     * Internal context.
     */
    private ServiceContext m_scope;
    
    /**
     * Instance logger.
     */
    private Logger m_logger;
    
    /**
     * Composite Manager.
     */
    private CompositeManager m_manager;
    
    /**
     * IS the handler valid ?
     */
    private boolean m_validity = false;
    
    /**
     * This structure aims to manage a configuration. It stores all necessary
     * information to create an instance and to track the factory.
     */
    private class ManagedConfiguration {
        /**
         * Configuration of the instance to create.
         */
        private Dictionary m_configuration;

        /**
         * Factory name.
         */
        private String m_factoryName;

        /**
         * Created instance.
         */
        private ComponentInstance m_instance;

        /**
         * Constructor.
         * 
         * @param conf : the configuration to create.
         */
        ManagedConfiguration(Dictionary conf) {
            m_configuration = conf;
        }

        /**
         * Return the managed configuration.
         * @return the configuration.
         */
        Dictionary getConfiguration() {
            return m_configuration;
        }

        /**
         * Return the used factory name.
         * @return the factory
         */
        String getFactory() {
            return m_factoryName;
        }

        /**
         * Return the created instance.
         * @return the instance (or null if no instance are created).
         */
        ComponentInstance getInstance() {
            return m_instance;
        }

        /**
         * Set the factory name.
         * 
         * @param name : the factory name.
         */
        void setFactory(String name) {
            m_factoryName = name;
        }

        /**
         * Set the instance object.
         * 
         * @param instance : the instance
         */
        void setInstance(ComponentInstance instance) {
            m_instance = instance;
        }
    }

    /**
     * Configurations to create and maintains.
     */
    private ManagedConfiguration[] m_configurations = new ManagedConfiguration[0];

    /**
     * Create an instance using the given factory and the given configuration.
     * 
     * @param fact : the facotry name to used.
     * @param config : the configuration.
     */
    private void createInstance(Factory fact, ManagedConfiguration config) {
        Dictionary conf = config.getConfiguration();
        try {
            config.setInstance(fact.createComponentInstance(conf, m_scope));
            config.setFactory(fact.getName());
            config.getInstance().addInstanceStateListener(this);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "A factory is available for the configuration but the configuration is not acceptable", e);
        }
    }

    /**
     * Service Listener implementation.
     * @param ev : the service event
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent ev) {
        ServiceReference ref = ev.getServiceReference();
        String factoryName = (String) ref.getProperty(org.osgi.framework.Constants.SERVICE_PID);
        String componentClass = (String) ref.getProperty("component.class");
        boolean implicated = false;
        if (ev.getType() == ServiceEvent.REGISTERED) {
            for (int i = 0; i < m_configurations.length; i++) {
                if (m_configurations[i].getInstance() == null
                        && (m_configurations[i].getConfiguration().get("component").equals(factoryName)
                        || m_configurations[i].getConfiguration().get("component").equals(componentClass))) {
                    Factory fact = (Factory) m_context.getService(ref);
                    createInstance(fact, m_configurations[i]);
                    implicated = true;
                }
            }
            if (implicated && !m_validity && checkValidity()) {
                m_manager.checkInstanceState();
            }
            return;
        }

        if (ev.getType() == ServiceEvent.UNREGISTERING) {
            for (int i = 0; i < m_configurations.length; i++) {
                if (m_configurations[i].getInstance() != null && m_configurations[i].getFactory().equals(factoryName)) {
                    m_configurations[i].setInstance(null);
                    m_configurations[i].setFactory(null);
                    m_context.ungetService(ref);
                    implicated = true;
                }
            }
            if (implicated && m_validity && !checkValidity()) {
                m_manager.checkInstanceState();
            }
            return;
        }
    }

    /**
     * Stop all created instances.
     */
    public synchronized void stop() {
        m_context.removeServiceListener(this);
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null) {
                m_configurations[i].getInstance().removeInstanceStateListener(this);
                m_configurations[i].getInstance().dispose();
            }
            m_configurations[i].setInstance(null);
            m_configurations[i].setFactory(null);
        }
        m_configurations = null;
    }
    
    
    /**
     * Configrue method.
     * @param im : instance manager.
     * @param metadata : component type metadata.
     * @param configuration : instance configuration.
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(CompositeManager im, Element metadata, Dictionary configuration) {
        m_manager = im;
        m_context = im.getContext();
        m_logger = im.getFactory().getLogger();
        m_scope = im.getServiceContext();
        Element[] instances = metadata.getElements("instance");
        m_configurations = new ManagedConfiguration[instances.length];
        for (int i = 0; i < instances.length; i++) {
            Dictionary conf = null;
            try {
                conf = parseInstance(instances[i]);
            } catch (ParseException e) {
                m_logger.log(Logger.ERROR, "An instance cannot be parsed correctly", e);
                return;
            }
            m_configurations[i] = new ManagedConfiguration(conf);
        }

        if (m_configurations.length > 0) {
            im.register(this);
        }
    }
    
    /**
     * Parse an Element to get a dictionary.
     * 
     * @param instance : the Element describing an instance.
     * @return : the resulting dictionary
     * @throws ParseException : occurs when a configuration cannot be parse correctly.
     */
    private Dictionary parseInstance(Element instance) throws ParseException {
        Dictionary dict = new Properties();
        if (instance.containsAttribute("name")) {
            dict.put("name", instance.getAttribute("name"));
        }
        if (!instance.containsAttribute("component")) {
            throw new ParseException("An instance does not have the 'component' attribute");
        }
        
        dict.put("component", instance.getAttribute("component"));

        for (int i = 0; i < instance.getElements("property").length; i++) {
            parseProperty(instance.getElements("property")[i], dict);
        }

        return dict;
    }
    
    /**
     * Parse a property.
     * @param prop : the current element to parse
     * @param dict : the dictionary to populate
     * @throws ParseException : occurs if the proeprty cannot be parsed correctly
     */
    private void parseProperty(Element prop, Dictionary dict) throws ParseException {
        // Check that the property has a name
        if (!prop.containsAttribute("name")) {
            throw new ParseException("A property does not have the 'name' attribute");
        }
        // Final case : the property element has a 'value' attribute
        if (prop.containsAttribute("value")) {
            dict.put(prop.getAttribute("name"), prop.getAttribute("value"));
        } else {
            // Recursive case
            // Check if there is 'property' element
            Element[] subProps = prop.getElements("property");
            if (subProps.length == 0) {
                throw new ParseException("A complex property must have at least one 'property' sub-element");
            }
            Dictionary dict2 = new Properties();
            for (int i = 0; i < subProps.length; i++) {
                parseProperty(subProps[i], dict2);
                dict.put(prop.getAttribute("name"), dict2);
            }
        }
    }

    /**
     * Start method.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        for (int i = 0; i < m_configurations.length; i++) {

            // Get the component type name :
            String componentType = (String) m_configurations[i].getConfiguration().get("component");
            Factory fact = null;

            try {
                String fil = "(|(" + org.osgi.framework.Constants.SERVICE_PID + "=" + componentType + ")(component.class=" + componentType + "))";
                ServiceReference[] refs = m_context.getServiceReferences(org.apache.felix.ipojo.Factory.class.getName(), fil);
                if (refs != null) {
                    fact = (Factory) m_context.getService(refs[0]);
                    createInstance(fact, m_configurations[i]);
                }
            } catch (InvalidSyntaxException e) {
                m_logger.log(Logger.ERROR, "Invalid syntax filter for the type : " + componentType, e);
            }
        }

        // Register a service listenner on Factory Service
        try {
            m_context.addServiceListener(this, "(objectClass=" + Factory.class.getName() + ")");
        } catch (InvalidSyntaxException e) {
            m_logger.log(Logger.ERROR, "Invalid syntax filter when registering a listener on Factory Service", e);
        }
        
        //Compute validity 
        checkValidity();
    }
    
    /**
     * The handler is valid if all managed instances are created and are valid.
     * @return true if all managed configuration have been instanciated and are valid.
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    public boolean isValid() {
        return m_validity;
    }
    
    /**
     * Check handler validity.
     * The method update the m_validity field.
     * @return the new validity.
     */
    private boolean checkValidity() {
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() == null || m_configurations[i].getInstance().getState() != ComponentInstance.VALID) {
                m_validity = false;
                return false;
            }
        }
        m_validity = true;
        return true;
    }

    /** Instance state listener.
     *  This method listens when managed instance states change.
     *  @param instance : instance
     *  @param newState : the now state of the given instance
     *  @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public void stateChanged(ComponentInstance instance, int newState) {
        switch(newState) {
            case ComponentInstance.DISPOSED : 
            case ComponentInstance.STOPPED :
                break; // Should not happen
            case ComponentInstance.VALID :
                if (! m_validity && checkValidity()) { 
                    m_manager.checkInstanceState();
                }
                break;
            case ComponentInstance.INVALID :
                if (m_validity && ! checkValidity()) {
                    m_manager.checkInstanceState();
                }
        }
    }
    
    /**
     * Method returning an instance object of the given componenet type.
     * This method must be coalled only on 'primitive' type.
     * @param type : type.
     * @return an instance object or null if not found.
     */
    public Object getObjectFromInstance(String type)  {
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null && type.equals(m_configurations[i].getFactory()) && m_configurations[i].getInstance().getState() == ComponentInstance.VALID) {
                return ((InstanceManager) m_configurations[i].getInstance()).getPojoObject();
            }
        }
        return null;
    }
    
    public HandlerDescription getDescription() {
        //TODO
        return null;
    }

}
