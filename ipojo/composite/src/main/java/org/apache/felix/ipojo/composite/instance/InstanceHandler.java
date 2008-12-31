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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseException;

/**
 * Composite Instance Handler.
 * This handler allows creating an instance inside a composite.
 * This instance is determine by its type and a configuration.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceHandler extends CompositeHandler implements InstanceStateListener {

    /**
     * Internal context.
     */
    private ServiceContext m_scope;
    
    /**
     * Available factories.
     */
    private Factory[] m_factories;
    
    /**
     * Handler description.
     */
    private InstanceHandlerDescription m_description;
    

    /**
     * This structure aims to manage a configuration. It stores all necessary
     * information to create an instance and to track the factory.
     */
    class ManagedConfiguration {
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
         * Desired Factory (can be the classname).
         */
        private String m_desiredFactory;

        /**
         * Constructor.
         * 
         * @param conf : the configuration to create.
         */
        ManagedConfiguration(Dictionary conf) {
            m_configuration = conf;
            m_desiredFactory = (String) conf.get("component");
        }

        /**
         * Return the managed configuration.
         * @return the configuration.
         */
        protected Dictionary getConfiguration() {
            return m_configuration;
        }

        /**
         * Return the used factory name.
         * @return the factory name
         */
        protected String getFactory() {
            return m_factoryName;
        }
        
        protected String getNeededFactoryName() {
            return m_desiredFactory;
        }

        /**
         * Return the created instance.
         * @return the instance (or null if no instance are created).
         */
        protected ComponentInstance getInstance() {
            return m_instance;
        }

        /**
         * Set the factory name.
         * 
         * @param name : the factory name.
         */
        protected void setFactory(String name) {
            m_factoryName = name;
        }

        /**
         * Set the instance object.
         * 
         * @param instance : the instance
         */
        protected void setInstance(ComponentInstance instance) {
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
     * @param fact : the factory name to used.
     * @param config : the configuration.
     */
    private void createInstance(Factory fact, ManagedConfiguration config) {
        Dictionary conf = config.getConfiguration();
        try {
            config.setInstance(fact.createComponentInstance(conf, m_scope));
            config.setFactory(fact.getName());
            config.getInstance().addInstanceStateListener(this);
        } catch (UnacceptableConfiguration e) {
            error("A factory is available for the configuration but the configuration is not acceptable", e);
        } catch (MissingHandlerException e) {
            error("The instance creation has failed, at least one handler is missing", e);
        } catch (ConfigurationException e) {
            error("The instance creation has failed, an error during the configuration has occured", e);
        }
    }
    
    /**
     * A new valid factory appears.
     * @param factory : factory.
     */
    public void bindFactory(Factory factory) {
        boolean implicated = false;
        String factName = factory.getName();
        String className = factory.getComponentDescription().getClassName();
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() == null && (m_configurations[i].getNeededFactoryName().equals(factName) || m_configurations[i].getNeededFactoryName().equals(className))) {
                createInstance(factory, m_configurations[i]);
                implicated = true;
            }
        }
        if (implicated && ! getValidity()) {
            checkValidity();
        }
    }
    
    /**
     * An existing factory disappears or becomes invalid.
     * @param factory : factory
     */
    public void unbindFactory(Factory factory) {
        boolean implicated = false;
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null && m_configurations[i].getFactory().equals(factory.getName())) {
                m_configurations[i].setInstance(null);
                m_configurations[i].setFactory(null);
                implicated = true;
            }
        }
        if (implicated && getValidity()) {
            checkValidity();
        }
    }

    /**
     * Stop all created instances.
     */
    public synchronized void stop() {
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null) {
                m_configurations[i].getInstance().removeInstanceStateListener(this);
                if (m_configurations[i].getInstance().getState() != ComponentInstance.DISPOSED) {
                    m_configurations[i].getInstance().dispose();
                }
            }
            m_configurations[i].setInstance(null);
            m_configurations[i].setFactory(null);
        }
        m_configurations = new ManagedConfiguration[0];
    }

    /**
     * Configure method.
     * @param metadata : component type metadata.
     * @param configuration : instance configuration.
     * @throws ConfigurationException : occurs an instance cannot be parsed correctly. 
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        m_scope = getCompositeManager().getServiceContext();
        Element[] instances = metadata.getElements("instance");
        m_configurations = new ManagedConfiguration[instances.length];
        for (int i = 0; i < instances.length; i++) {
            Dictionary conf = null;
            try {
                conf = parseInstance(instances[i]);
            } catch (ParseException e) {
                error("An instance cannot be parsed correctly", e);
                throw new ConfigurationException("An instance cannot be parsed correctly : " + e.getMessage());
            }
            m_configurations[i] = new ManagedConfiguration(conf);
        }
        
        m_description = new InstanceHandlerDescription(this, m_configurations);
    }

    /**
     * Parse an Element to get a dictionary.
     * @param instance : the Element describing an instance.
     * @return : the resulting dictionary
     * @throws ParseException : occurs when a configuration cannot be parse correctly.
     */
    public static Dictionary parseInstance(Element instance) throws ParseException {
        Dictionary dict = new Properties();
        String name = instance.getAttribute("name");
        if (name != null) {
            dict.put("name", name);
        }
        
        String comp = instance.getAttribute("component");
        if (comp == null) { 
            throw new ParseException("An instance does not have the 'component' attribute"); 
        } else {
            dict.put("component", comp);
        }

        Element[] props = instance.getElements("property");
        for (int i = 0; props != null && i < props.length; i++) {
            parseProperty(props[i], dict);
        }

        return dict;
    }

    /**
     * Parse a property.
     * @param prop : the current element to parse
     * @param dict : the dictionary to populate
     * @throws ParseException : occurs if the property cannot be parsed correctly
     */
    public static void parseProperty(Element prop, Dictionary dict) throws ParseException {
        // Check that the property has a name
        String name = prop.getAttribute("name");
        String value = prop.getAttribute("value");
        if (name == null) { throw new ParseException("A property does not have the 'name' attribute"); }
        // Final case : the property element has a 'value' attribute
        if (value == null) {
            // Recursive case
            // Check if there is 'property' element
            Element[] subProps = prop.getElements("property");
            if (subProps == null) { throw new ParseException("A complex property must have at least one 'property' sub-element"); }
            Dictionary dict2 = new Properties();
            for (int i = 0; i < subProps.length; i++) {
                parseProperty(subProps[i], dict2);
                dict.put(prop.getAttribute("name"), dict2);
            }
        } else {
            dict.put(name, value);
        }
    }

    /**
     * Start method.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() { 
        for (int j = 0; j < m_factories.length; j++) {
            String factName = m_factories[j].getName();
            String className = m_factories[j].getClassName(); 
            for (int i = 0; i < m_configurations.length; i++) {
                if (m_configurations[i].getInstance() == null && (m_configurations[i].getNeededFactoryName().equals(factName) || m_configurations[i].getNeededFactoryName().equals(className))) {
                    createInstance(m_factories[j], m_configurations[i]);
                }
            }
        }
        checkValidity();
    }

    /**
     * Check handler validity.
     * The method update the validaity of the handler.
     */
    private void checkValidity() {
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() == null || m_configurations[i].getInstance().getState() != ComponentInstance.VALID) {
                setValidity(false);
                return;
            }
        }
        setValidity(true);
    }

    /**
     *  Instance state listener.
     *  This method listens when managed instance states change.
     *  @param instance : instance
     *  @param newState : the now state of the given instance
     *  @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public void stateChanged(ComponentInstance instance, int newState) {
        switch (newState) {
            case ComponentInstance.DISPOSED:
            case ComponentInstance.STOPPED:
                break; // Should not happen
            case ComponentInstance.VALID:
                if (!getValidity()) {
                    checkValidity();
                }
                break;
            case ComponentInstance.INVALID:
                if (getValidity()) {
                    checkValidity();
                }
                break;
            default:
                break;

        }
    }

    /**
     * Method returning an instance object of the given component type.
     * This method must be called only on 'primitive' type.
     * @param type : type.
     * @return an instance object or null if not found.
     */
    public Object getObjectFromInstance(String type) {
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null && type.equals(m_configurations[i].getFactory())) {
                if (m_configurations[i].getInstance().getState() == ComponentInstance.VALID) {
                    return ((InstanceManager) m_configurations[i].getInstance()).getPojoObject();
                } else {
                    error("An object cannot be get from the instance of the type " + type + ": invalid instance" + m_configurations[i].getInstance().getInstanceDescription().getDescription());
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Return the handler description, i.e. the state of created instances.
     * @return the handler description.
     * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }

    /**
     * Get the list of used component type.
     * @return the list containing the used component type
     */
    public List getUsedType() {
        List result = new ArrayList();
        for (int i = 0; i < m_configurations.length; i++) {
            result.add(m_configurations[i].getConfiguration().get("component"));
        }
        return result;
    }

}
