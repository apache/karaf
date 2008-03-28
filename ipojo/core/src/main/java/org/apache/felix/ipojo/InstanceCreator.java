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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * An instance creator aims to create instances and to track their factories. It's allow to create instance from outside factories.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceCreator implements FactoryStateListener {

    /**
     * Logger to log messages if error occurs.
     */
    private Logger m_logger;

    /**
     * Configurations to create and maintains.
     */
    private List m_idle = new ArrayList();

    /**
     * Map storing created instance. [AbstractFactory, List [ManagedInstance]]
     */
    private Map m_attached = new HashMap();

    /**
     * Abstract Factory list.
     */
    private List m_factories = new ArrayList();

    /**
     * Constructor.
     * @param context : iPOJO bundle context.
     */
    public InstanceCreator(BundleContext context) {
        m_logger = new Logger(context, "iPOJO Instance Creator");
    }

    /**
     * Add an instance to manage.
     * @param instance : instance configuration
     * @param bundle : bundle id declaring the instance
     */
    synchronized void addInstance(Dictionary instance, long bundle) {
        ManagedInstance managed = new ManagedInstance(instance, bundle);
        for (int i = 0; i < m_factories.size(); i++) {
            IPojoFactory factory = (IPojoFactory) m_factories.get(i);
            if (factory.getState() == Factory.VALID && managed.match(factory)) {
                managed.create(factory);
                List list = (List) m_attached.get(factory);
                if (list == null) {
                    list = new ArrayList();
                    list.add(managed);
                    m_attached.put(factory, list);
                    // Subscribe to the factory state change
                    factory.addFactoryStateListener(this);
                } else {
                    list.add(managed);
                }
                return;
            }
        }
        // If there is no matching factory, add the instance to the idle list
        m_idle.add(managed);
    }

    /**
     * Dispose and stop to manage all instances declared by the given bundle.
     * @param bundle : bundle.
     */
    void removeInstancesFromBundle(long bundle) {
        // Disposes instance from attached instances
        Collection col = m_attached.keySet();
        Iterator iterator = col.iterator();
        List instanceToRemove = new ArrayList();
        List factoryToRemove = new ArrayList();
        while (iterator.hasNext()) {
            IPojoFactory factory = (IPojoFactory) iterator.next();
            List list = (List) m_attached.get(factory);
            for (int i = 0; i < list.size(); i++) {
                ManagedInstance managed = (ManagedInstance) list.get(i);
                if (managed.m_bundleId == bundle) {
                    managed.dispose();
                    instanceToRemove.add(managed);
                }
            }
            if (!instanceToRemove.isEmpty()) {
                list.removeAll(instanceToRemove);
                if (list.isEmpty()) {
                    factory.removeFactoryStateListener(this);
                    factoryToRemove.add(factory);
                }
            }
        }

        for (int i = 0; i < factoryToRemove.size(); i++) {
            m_attached.remove(factoryToRemove.get(i));
        }

        // Delete idle instances
        instanceToRemove.clear();
        for (int i = 0; i < m_idle.size(); i++) {
            ManagedInstance managed = (ManagedInstance) m_idle.get(i);
            if (managed.m_bundleId == bundle) {
                instanceToRemove.add(managed);
            }
        }
        m_idle.removeAll(instanceToRemove);
    }

    /**
     * A new factory appears.
     * @param factory : the new factory.
     */
    public synchronized void addFactory(IPojoFactory factory) {
        List createdInstances = new ArrayList(1);
        m_factories.add(factory);
        for (int i = 0; i < m_idle.size(); i++) {
            ManagedInstance managed = (ManagedInstance) m_idle.get(i);
            if (managed.match(factory)) {
                // We have to subscribe to the factory.
                factory.addFactoryStateListener(this);
                if (factory.getState() == Factory.VALID) {
                    managed.create(factory);
                    List list = (List) m_attached.get(factory);
                    if (list == null) {
                        list = new ArrayList();
                        list.add(managed);
                        m_attached.put(factory, list);
                    } else {
                        list.add(managed);
                    }
                    createdInstances.add(managed);
                }
            }
        }
        if (!createdInstances.isEmpty()) {
            m_idle.removeAll(createdInstances);
        }
    }

    /**
     * A factory is leaving.
     * @param factory : the leaving factory
     */
    void removeFactory(IPojoFactory factory) {
        factory.removeFactoryStateListener(this);
        m_factories.remove(factory);
        onInvalidation(factory);
        m_attached.remove(factory);
    }

    /**
     * The given factory becomes valid.
     * @param factory : the factory becoming valid.
     */
    private void onValidation(IPojoFactory factory) {
        List toRemove = new ArrayList();
        for (int i = 0; i < m_idle.size(); i++) {
            ManagedInstance managed = (ManagedInstance) m_idle.get(i);
            if (managed.match(factory)) {
                managed.create(factory);
                List list = (List) m_attached.get(factory);
                if (list == null) {
                    list = new ArrayList();
                    list.add(managed);
                    m_attached.put(factory, list);
                } else {
                    list.add(managed);
                }
                toRemove.add(managed);
            }
        }
        if (!toRemove.isEmpty()) {
            m_idle.removeAll(toRemove);
        }
    }

    /**
     * The given factory becomes invalid.
     * @param factory : factory which becomes invalid.
     */
    private void onInvalidation(IPojoFactory factory) {
        List instances = (List) m_attached.remove(factory);
        if (instances != null) {
            for (int i = 0; i < instances.size(); i++) {
                ManagedInstance managed = (ManagedInstance) instances.get(i);
                managed.dispose();
                m_idle.add(managed);
            }
        }
    }

    /**
     * Factory state changed method.
     * @param factory : factory.
     * @param newState : new state.
     * @see org.apache.felix.ipojo.FactoryStateListener#stateChanged(org.apache.felix.ipojo.Factory, int)
     */
    public void stateChanged(Factory factory, int newState) {
        if (newState == Factory.VALID) {
            onValidation((IPojoFactory) factory);
        } else {
            onInvalidation((IPojoFactory) factory);
        }
    }

    /**
     * This structure aims to manage a configuration. It stores all necessary information to create an instance and to track the factory.
     */
    private class ManagedInstance {
        /**
         * Configuration of the instance to create.
         */
        private Dictionary m_configuration;

        /**
         * Bundle which create the instance.
         */
        private long m_bundleId;

        /**
         * Factory used to create the instance.
         */
        private IPojoFactory m_factory;

        /**
         * Created instance.
         */
        private ComponentInstance m_instance;

        /**
         * Constructor.
         * @param conf : the configuration to create.
         * @param bundle : the bundle in which the instance is declared.
         */
        ManagedInstance(Dictionary conf, long bundle) {
            m_configuration = conf;
            m_bundleId = bundle;
        }

        /**
         * Return the used factory name.
         * @return the factory
         */
        IPojoFactory getFactory() {
            return m_factory;
        }

        /**
         * Return the created instance.
         * @return the instance (or null if no instance are created).
         */
        ComponentInstance getInstance() {
            return m_instance;
        }

        /**
         * Test if the given factory match with the factory required by this instance. A factory matches if its name or its class name is equals to
         * the 'component' property of the instance. Then the acceptability of the configuration is checked.
         * @param factory : the factory to confront against the current instance.
         * @return true if the factory match.
         */
        public boolean match(IPojoFactory factory) {
            // Test factory name (and classname)
            String component = (String) m_configuration.get("component");
            if (factory.getName().equals(component) || factory.getClassName().equalsIgnoreCase(component)) {
                // Test factory accessibility
                if (factory.m_isPublic || factory.getBundleContext().getBundle().getBundleId() == m_bundleId) {
                    // Test the configuration validity.
                    if (factory.isAcceptable(m_configuration)) {
                        return true;
                    } else {
                        m_logger.log(Logger.ERROR, "An instance can be bound to a matching factory, however the configuration seems unacceptable : "
                                + m_configuration);
                    }
                }
            }
            return false;
        }

        /**
         * Create the instance by using the given factory.
         * @param factory : the factory to use to create the instance. The factory must match.
         */
        public void create(IPojoFactory factory) {
            try {
                m_factory = factory;
                m_instance = m_factory.createComponentInstance(m_configuration);
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "A matching factory was found for " + m_configuration + ", but the instantiation failed : "
                        + e.getMessage());
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "A matching factory was found for " + m_configuration + ", but the instantiation failed : "
                        + e.getMessage());
            } catch (ConfigurationException e) {
                m_logger.log(Logger.ERROR, "A matching factory was found for " + m_configuration + ", but the instantiation failed : "
                        + e.getMessage());
            }
        }

        /**
         * Dispose the current instance.
         */
        public void dispose() {
            if (m_instance != null) {
                m_instance.dispose();
            }
            m_instance = null;
            m_factory = null;
        }
    }

}
