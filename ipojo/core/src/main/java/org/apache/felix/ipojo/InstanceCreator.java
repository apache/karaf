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

import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * An instance creator aims to create instances and to track their factories.
 * It's allow to create instance from outside factories.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceCreator implements TrackerCustomizer, FactoryStateListener {
    /**
     * Bundle Context.
     */
    private BundleContext m_context;

    /**
     * Logger to log messages if error occurs.
     */
    private Logger m_logger;
    
    /**
     * Private factories.
     */
    private ComponentFactory[] m_factories;

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
     * Service Tracker tracking factories.
     */
    private Tracker m_tracker;

    /**
     * Constructor.
     * 
     * @param context : the bundle context.
     * @param configurations : configuration set to create and maintain.
     * @param factories : private factories.
     */
    public InstanceCreator(BundleContext context, Dictionary[] configurations, ComponentFactory[] factories) {
        m_context = context;
        m_logger = new Logger(context, "InstanceCreator" + context.getBundle().getBundleId(), Logger.WARNING);
        
        m_configurations = new ManagedConfiguration[configurations.length];
        m_factories = factories;
       
        for (int i = 0; i < configurations.length; i++) {
            ManagedConfiguration conf = new ManagedConfiguration(configurations[i]);
            m_configurations[i] = conf;
            // Get the component type name :
            String componentType = (String) conf.getConfiguration().get("component");

            boolean found = false;
            for (int j = 0; m_factories != null && !found && j < m_factories.length; j++) {
                if (m_factories[j].m_state == Factory.VALID && (m_factories[j].getName().equals(componentType) || (m_factories[j].getComponentClassName() != null && m_factories[j].getComponentClassName().equals(componentType)))) {
                    createInstance(m_factories[j], conf);
                    found = true;
                }
            }
        }
        
        for (int i = 0; m_factories != null && i < m_factories.length; i++) {
            m_factories[i].addFactoryStateListener(this);
        }
       
        
        String filter = "(&(objectclass=" + Factory.class.getName() + ")(factory.state=1))";
        try {
            m_tracker = new Tracker(m_context, m_context.createFilter(filter), this);
            m_tracker.open();
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
            return;
        } 
    }

    /**
     * Create an instance using the given factory and the given configuration.
     * 
     * @param fact : the factory name to used.
     * @param config : the configuration.
     */
    private void createInstance(Factory fact, ManagedConfiguration config) {
        Dictionary conf = config.getConfiguration();
        try {
            config.setInstance(fact.createComponentInstance(conf));
            config.setFactory(fact.getName());
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "A factory is available for the configuration but the configuration is not acceptable", e);
        } catch (MissingHandlerException e) {
            m_logger.log(Logger.ERROR, "The instance creation has failed, at least one handler is missing", e);
        } catch (ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The instance creation has failed, an error during the configuration has occured", e);
        }
    }

    /**
     * Stop all created instances.
     */
    public synchronized void stop() {
        m_tracker.close();
        
        for (int i = 0; m_factories != null && i < m_factories.length; i++) {
            m_factories[i].removeFactoryStateListener(this);
        }
        
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null) {
                m_configurations[i].getInstance().dispose();
            }
            m_configurations[i].setInstance(null);
            m_configurations[i].setFactory(null);
        }
        
        m_factories = null;
        m_tracker = null;
        m_logger = null;
        m_configurations = null;
    }

    /**
     * Factory state changed method.
     * @param factory : factory.
     * @param newState : new state.
     * @see org.apache.felix.ipojo.FactoryStateListener#stateChanged(org.apache.felix.ipojo.Factory, int)
     */
    public void stateChanged(Factory factory, int newState) {
        if (newState == Factory.VALID) {
            for (int i = 0; i < m_configurations.length; i++) {
                if (m_configurations[i].getInstance() == null
                        && (m_configurations[i].getConfiguration().get("component").equals(factory.getName()) || m_configurations[i].getConfiguration().get(
                                "component").equals(((ComponentFactory) factory).getComponentClassName()))) {
                    Factory fact = factory;
                    createInstance(fact, m_configurations[i]);
                }
            }
            return;
        } else {
            // newState == INVALID
            for (int i = 0; i < m_configurations.length; i++) {
                if (m_configurations[i].getInstance() != null && m_configurations[i].getFactory().equals(factory.getName())) {
                    m_configurations[i].setInstance(null);
                    m_configurations[i].setFactory(null);
                }
            }
            return;
        }
    }

    /**
     * A new factory has been detected.
     * @param ref : the factory service reference.
     * @return true if the factory can be used to create a managed instance.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference ref) {
        String factoryName = (String) ref.getProperty("factory.name");
        String componentClass = (String) ref.getProperty("component.class");
        boolean isValid = ((String) ref.getProperty("factory.state")).equals("" + Factory.VALID); 
        Factory fact = (Factory) m_tracker.getService(ref);

        boolean used = false;
        if (isValid) {
            for (int i = 0; i < m_configurations.length; i++) {
                if (m_configurations[i].getInstance() == null
                        && (m_configurations[i].getConfiguration().get("component").equals(factoryName) || m_configurations[i].getConfiguration().get("component").equals(componentClass))) {
                    createInstance(fact, m_configurations[i]);
                    used = true;
                }
            }
        }
        return used;
    }

    /**
     * A used factory is modified.
     * @param ref : modified reference.
     * @param obj : factory object.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference ref, Object obj) {
        // Nothing to do.
    }

    /**
     * A used factory disappears.
     * All created instance are disposed.
     * @param ref : service reference.
     * @param obj : factory object.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference ref, Object obj) {
        Factory fact = (Factory) obj;
        for (int i = 0; i < m_configurations.length; i++) {
            if (m_configurations[i].getInstance() != null && m_configurations[i].getFactory().equals(fact.getName())) {
                m_configurations[i].setInstance(null);
                m_configurations[i].setFactory(null);
            }
        }
        m_tracker.ungetService(ref);
    }

}
